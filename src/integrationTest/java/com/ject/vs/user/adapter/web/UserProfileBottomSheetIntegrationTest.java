package com.ject.vs.user.adapter.web;

import com.ject.vs.chat.domain.ChatMessage;
import com.ject.vs.chat.domain.ChatMessageRepository;
import com.ject.vs.support.ChatIntegrationTestSupport;
import com.ject.vs.user.domain.User;
import com.ject.vs.vote.domain.Vote;
import com.ject.vs.vote.domain.VoteOption;
import com.ject.vs.vote.domain.VoteParticipation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("UserProfileBottomSheet API 통합 테스트")
class UserProfileBottomSheetIntegrationTest extends ChatIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Nested
    @DisplayName("GET /api/users/{userId}/profile-sheet")
    class GetProfileBottomSheet {

        @Test
        @DisplayName("참여투표 개수와 최신 활동순 3개 카드를 반환한다")
        void returns_profile_sheet_with_recent_votes() throws Exception {
            User targetUser = createUserWithNickname("프로필대상");
            User viewer = createUserWithNickname("조회자");

            VoteFixture vote1 = createVoteWithOptions("투표1", "선택1-A", "선택1-B", null);
            VoteFixture vote2 = createImmersiveVoteWithOptions("투표2", "선택2-A", "선택2-B");
            VoteFixture vote3 = createVoteWithOptions("투표3", "선택3-A", "선택3-B", null);
            createVoteWithOptions("투표4", "선택4-A", "선택4-B", null);

            participate(targetUser, vote1, vote1.optionA());
            participate(targetUser, vote2, vote2.optionB());
            participate(targetUser, vote3, vote3.optionA());
            participate(viewer, vote1, vote1.optionA());

            // vote3에 채팅 활동을 추가해 최신순 1위로 올린다
            chatMessageRepository.save(ChatMessage.of(vote3.vote().getId(), targetUser, "최근 채팅"));
            entityManager.flush();

            mockMvc.perform(get("/api/users/{userId}/profile-sheet", targetUser.getId())
                            .with(asUser(viewer.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(targetUser.getId()))
                    .andExpect(jsonPath("$.nickname").value("프로필대상"))
                    .andExpect(jsonPath("$.imageColor").value("GREEN"))
                    .andExpect(jsonPath("$.participatedVoteCount").value(3))
                    .andExpect(jsonPath("$.recentParticipatedVotes.length()").value(3))
                    .andExpect(jsonPath("$.recentParticipatedVotes[0].voteId").value(vote3.vote().getId()))
                    .andExpect(jsonPath("$.recentParticipatedVotes[0].title").value("투표3"))
                    .andExpect(jsonPath("$.recentParticipatedVotes[0].status").value("ONGOING"))
                    .andExpect(jsonPath("$.recentParticipatedVotes[0].selectedOptionLabel").value("선택3-A"))
                    .andExpect(jsonPath("$.recentParticipatedVotes[0].viewerParticipated").value(false))
                    .andExpect(jsonPath("$.recentParticipatedVotes[1].voteId").exists())
                    .andExpect(jsonPath("$.recentParticipatedVotes[2].voteId").exists());
        }

        @Test
        @DisplayName("비회원은 401을 반환한다")
        void returns_401_for_anonymous() throws Exception {
            User targetUser = createUserWithNickname("대상");

            mockMvc.perform(get("/api/users/{userId}/profile-sheet", targetUser.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("존재하지 않는 유저는 404를 반환한다")
        void returns_404_when_user_not_found() throws Exception {
            User viewer = createUserWithNickname("조회자");

            mockMvc.perform(get("/api/users/{userId}/profile-sheet", 999_999L)
                            .with(asUser(viewer.getId())))
                    .andExpect(status().isNotFound());
        }
    }

    private VoteFixture createVoteWithOptions(String title, String optionALabel, String optionBLabel, String imageUrl) {
        Vote vote = voteRepository.save(Vote.create(
                title, "content", "https://example.com/thumb.jpg",
                imageUrl, Duration.ofHours(24), clock));
        VoteOption optionA = voteOptionRepository.save(VoteOption.of(vote, optionALabel, 0));
        VoteOption optionB = voteOptionRepository.save(VoteOption.of(vote, optionBLabel, 1));
        entityManager.flush();
        entityManager.clear();
        Vote reloaded = voteRepository.findById(vote.getId()).orElseThrow();
        return new VoteFixture(reloaded, optionA, optionB);
    }

    private VoteFixture createImmersiveVoteWithOptions(String title, String optionALabel, String optionBLabel) {
        return createVoteWithOptions(title, optionALabel, optionBLabel, "https://example.com/immersive.jpg");
    }

    private void participate(User user, VoteFixture fixture, VoteOption option) {
        voteParticipationRepository.save(
                VoteParticipation.ofMember(fixture.vote().getId(), user.getId(), option.getId()));
    }

    private record VoteFixture(Vote vote, VoteOption optionA, VoteOption optionB) {}
}