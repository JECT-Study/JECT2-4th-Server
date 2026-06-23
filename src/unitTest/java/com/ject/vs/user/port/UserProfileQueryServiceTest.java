package com.ject.vs.user.port;

import com.ject.vs.common.exception.BusinessException;
import com.ject.vs.user.adapter.web.dto.UserProfileBottomSheetResponse;
import com.ject.vs.user.domain.Gender;
import com.ject.vs.user.domain.ImageColor;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.domain.UserStatus;
import com.ject.vs.user.exception.UserErrorCode;
import com.ject.vs.user.port.in.UserQueryUseCase;
import com.ject.vs.vote.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileQueryService")
class UserProfileQueryServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2025-06-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Mock
    private UserQueryUseCase userQueryUseCase;
    @Mock
    private VoteParticipationRepository voteParticipationRepository;
    @Mock
    private VoteRepository voteRepository;
    @Mock
    private VoteOptionRepository voteOptionRepository;
    @Mock
    private Clock clock;

    @InjectMocks
    private UserProfileQueryService userProfileQueryService;

    @Nested
    @DisplayName("getProfileBottomSheet")
    class GetProfileBottomSheet {

        @Test
        @DisplayName("참여투표 개수와 최신 3개 카드를 반환한다")
        void returns_profile_with_recent_votes() {
            User targetUser = createUser(10L, "승부사");
            User viewer = createUser(20L, "조회자");

            Vote ongoingVote = createVote(1L, "진행중 투표", null);
            Vote immersiveVote = createVote(2L, "몰입형 투표", "https://example.com/image.jpg");

            VoteOption optionA = createOption(100L, "옵션 A");
            VoteOption optionB = createOption(200L, "옵션 B");

            VoteParticipation targetParticipation1 = VoteParticipation.ofMember(1L, 10L, 100L);
            VoteParticipation targetParticipation2 = VoteParticipation.ofMember(2L, 10L, 200L);

            given(clock.instant()).willReturn(FIXED_NOW);
            given(userQueryUseCase.getUser(10L)).willReturn(targetUser);
            given(voteParticipationRepository.countByUserId(10L)).willReturn(5L);
            given(voteParticipationRepository.findTopVoteIdsByRecentActivity(10L, 3))
                    .willReturn(List.of(1L, 2L));
            given(voteRepository.findAllByIdIn(List.of(1L, 2L))).willReturn(List.of(ongoingVote, immersiveVote));
            given(voteParticipationRepository.findByVoteIdAndUserId(1L, 10L))
                    .willReturn(Optional.of(targetParticipation1));
            given(voteParticipationRepository.findByVoteIdAndUserId(2L, 10L))
                    .willReturn(Optional.of(targetParticipation2));
            given(voteOptionRepository.findAllById(List.of(100L, 200L)))
                    .willReturn(List.of(optionA, optionB));
            given(voteParticipationRepository.existsByVoteIdAndUserId(1L, 20L)).willReturn(true);
            given(voteParticipationRepository.existsByVoteIdAndUserId(2L, 20L)).willReturn(false);

            UserProfileBottomSheetResponse response =
                    userProfileQueryService.getProfileBottomSheet(10L, 20L);

            assertThat(response.userId()).isEqualTo(10L);
            assertThat(response.nickname()).isEqualTo("승부사");
            assertThat(response.imageColor()).isEqualTo(ImageColor.GREEN);
            assertThat(response.participatedVoteCount()).isEqualTo(5L);
            assertThat(response.recentParticipatedVotes()).hasSize(2);

            assertThat(response.recentParticipatedVotes().get(0).voteId()).isEqualTo(1L);
            assertThat(response.recentParticipatedVotes().get(0).status()).isEqualTo(VoteStatus.ONGOING);
            assertThat(response.recentParticipatedVotes().get(0).voteType()).isEqualTo(VoteType.GENERAL);
            assertThat(response.recentParticipatedVotes().get(0).selectedOptionLabel()).isEqualTo("옵션 A");
            assertThat(response.recentParticipatedVotes().get(0).viewerParticipated()).isTrue();

            assertThat(response.recentParticipatedVotes().get(1).voteId()).isEqualTo(2L);
            assertThat(response.recentParticipatedVotes().get(1).voteType()).isEqualTo(VoteType.IMMERSIVE);
            assertThat(response.recentParticipatedVotes().get(1).selectedOptionLabel()).isEqualTo("옵션 B");
            assertThat(response.recentParticipatedVotes().get(1).viewerParticipated()).isFalse();
        }

        @Test
        @DisplayName("참여투표가 없으면 빈 리스트를 반환한다")
        void returns_empty_recent_votes_when_no_participation() {
            User targetUser = createUser(10L, "신규유저");

            given(userQueryUseCase.getUser(10L)).willReturn(targetUser);
            given(voteParticipationRepository.countByUserId(10L)).willReturn(0L);
            given(voteParticipationRepository.findTopVoteIdsByRecentActivity(10L, 3))
                    .willReturn(List.of());

            UserProfileBottomSheetResponse response =
                    userProfileQueryService.getProfileBottomSheet(10L, 20L);

            assertThat(response.participatedVoteCount()).isZero();
            assertThat(response.recentParticipatedVotes()).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 유저는 USER_NOT_FOUND 예외를 던진다")
        void throws_when_user_not_found() {
            given(userQueryUseCase.getUser(999L))
                    .willThrow(new BusinessException(UserErrorCode.USER_NOT_FOUND));

            assertThatThrownBy(() -> userProfileQueryService.getProfileBottomSheet(999L, 20L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);
        }
    }

    private User createUser(Long id, String nickname) {
        User user = User.createWithEmail("user" + id + "@test.com");
        user.updateInfo(new com.ject.vs.user.adapter.web.dto.UserExtraInfo(
                java.time.Year.of(2000), Gender.MALE, nickname, ImageColor.GREEN));
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Vote createVote(Long id, String title, String imageUrl) {
        Vote vote = Vote.create(title, "content", "https://example.com/thumb.jpg",
                imageUrl, Duration.ofHours(24), CLOCK);
        ReflectionTestUtils.setField(vote, "id", id);
        return vote;
    }

    private VoteOption createOption(Long id, String label) {
        Vote vote = createVote(99L, "dummy", null);
        VoteOption option = VoteOption.of(vote, label, 0);
        ReflectionTestUtils.setField(option, "id", id);
        return option;
    }
}