package com.ject.vs.vote.port;

import com.ject.vs.vote.domain.VoteParticipationRepository;
import com.ject.vs.vote.port.in.dto.VoteStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @InjectMocks
    private VoteService voteService;

    @Mock
    private VoteParticipationRepository voteParticipationRepository;

    @Nested
    class isParticipant {

        @Test
        void 참여한_사용자는_true를_반환한다() {
            // given
            given(voteParticipationRepository.existsByVoteIdAndUserId(1L, 2L)).willReturn(true);

            // when
            boolean result = voteService.isParticipant(1L, 2L);

            // then
            assertThat(result).isTrue();
        }

        @Test
        void 참여하지_않은_사용자는_false를_반환한다() {
            // given
            given(voteParticipationRepository.existsByVoteIdAndUserId(1L, 2L)).willReturn(false);

            // when
            boolean result = voteService.isParticipant(1L, 2L);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    class findAllVoteIdsByUserId {

        @Test
        void 유저가_참여한_voteId_목록을_반환한다() {
            // given
            given(voteParticipationRepository.findAllVoteIdsByUserId(1L)).willReturn(List.of(10L, 20L));

            // when
            List<Long> result = voteService.findAllVoteIdsByUserId(1L);

            // then
            assertThat(result).containsExactly(10L, 20L);
        }

        @Test
        void 참여한_투표가_없으면_빈_목록을_반환한다() {
            // given
            given(voteParticipationRepository.findAllVoteIdsByUserId(1L)).willReturn(List.of());

            // when
            List<Long> result = voteService.findAllVoteIdsByUserId(1L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class countParticipantsByVoteId {

        @Test
        void 투표_참여자_수를_반환한다() {
            // given
            given(voteParticipationRepository.countByVoteId(1L)).willReturn(25L);

            // when
            long result = voteService.countParticipantsByVoteId(1L);

            // then
            assertThat(result).isEqualTo(25L);
            verify(voteParticipationRepository).countByVoteId(1L);
        }
    }

    @Nested
    class findAllUserIdsByVoteId {

        @Test
        void 투표에_참여한_userId_목록을_반환한다() {
            // given
            given(voteParticipationRepository.findAllUserIdsByVoteId(1L)).willReturn(List.of(100L, 200L));

            // when
            List<Long> result = voteService.findAllUserIdsByVoteId(1L);

            // then
            assertThat(result).containsExactly(100L, 200L);
        }
    }

    @Nested
    class findAllVoteIdsByStatus {

        @Test
        void ONGOING_상태로_조회하면_전달된_voteId_목록을_그대로_반환한다() {
            // given
            List<Long> voteIds = List.of(1L, 2L, 3L);

            // when
            List<Long> result = voteService.findAllVoteIdsByStatus(voteIds, VoteStatus.ONGOING);

            // then
            // TODO: Vote 도메인 연동 후 실제 status 필터링 검증으로 교체
            assertThat(result).isEqualTo(voteIds);
        }

        @Test
        void 빈_목록을_전달하면_빈_목록을_반환한다() {
            // given
            List<Long> voteIds = List.of();

            // when
            List<Long> result = voteService.findAllVoteIdsByStatus(voteIds, VoteStatus.ENDED);

            // then
            assertThat(result).isEmpty();
        }
    }
}
