package com.ject.vs.vote.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ject.vs.config.OAuth2LoginSuccessHandler;
import com.ject.vs.config.TestPropertiesConfig;
import org.springframework.context.annotation.Import;
import com.ject.vs.auth.port.CustomOAuth2UserService;
import com.ject.vs.util.CookieUtil;
import com.ject.vs.util.JwtProvider;
import com.ject.vs.vote.adapter.web.dto.ParticipateRequest;
import com.ject.vs.vote.domain.ImmersiveVoteAction;
import com.ject.vs.vote.exception.VoteEndedException;
import com.ject.vs.vote.exception.VoteFreeLimitExceededException;
import com.ject.vs.vote.exception.VoteNotFoundException;
import com.ject.vs.vote.domain.VoteEmoji;
import com.ject.vs.vote.port.in.ImmersiveVoteCommandUseCase;
import com.ject.vs.vote.port.in.ImmersiveVoteCommandUseCase.ImmersiveParticipateResult;
import com.ject.vs.vote.port.in.ImmersiveVoteQueryUseCase;
import com.ject.vs.vote.port.in.VoteCommandUseCase.OptionResult;
import com.ject.vs.vote.port.in.ImmersiveVoteQueryUseCase.FeedOptionItem;
import com.ject.vs.vote.port.in.ImmersiveVoteQueryUseCase.ImmersiveFeedItem;
import com.ject.vs.vote.port.in.ImmersiveVoteQueryUseCase.ImmersiveFeedResult;
import com.ject.vs.vote.port.in.ImmersiveVoteQueryUseCase.ImmersiveLiveResult;
import com.ject.vs.vote.port.in.ImmersiveVoteQueryUseCase.LiveOptionItem;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase.ShareLinkResult;

import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ImmersiveVoteController.class)
@Import(TestPropertiesConfig.class)
class ImmersiveVoteControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ImmersiveVoteCommandUseCase immersiveVoteCommandUseCase;
    @MockBean ImmersiveVoteQueryUseCase immersiveVoteQueryUseCase;
    @MockBean VoteResultQueryUseCase voteResultQueryUseCase;
    @MockBean JwtProvider jwtProvider;
    @MockBean CookieUtil cookieUtil;
    @MockBean OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    @MockBean CustomOAuth2UserService customOAuth2UserService;

    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList());

    @Nested
    class getFeed {

        @Test
        @WithMockUser
        void 피드_목록_200_반환() throws Exception {
            given(immersiveVoteQueryUseCase.getFeed(any(), anyInt(), any(), any()))
                    .willReturn(new ImmersiveFeedResult(List.of(), null, false));

            mockMvc.perform(get("/api/immersive-votes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasNext").value(false));
        }
    }

    @Nested
    class participateOrCancel {

        @Test
        void 회원_참여_VOTED_반환() throws Exception {
            given(immersiveVoteCommandUseCase.participateOrCancel(eq(1L), eq(1L), any(), eq(10L)))
                    .willReturn(new ImmersiveParticipateResult(1L, ImmersiveVoteAction.VOTED, 10L, List.of(), null));

            mockMvc.perform(put("/api/immersive-votes/1/participate")
                            .with(authentication(AUTH)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ParticipateRequest(10L))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.action").value("VOTED"))
                    .andExpect(jsonPath("$.selectedOptionId").value(10));
        }

        @Test
        @WithMockUser
        void 비회원_참여_VOTED_반환() throws Exception {
            given(immersiveVoteCommandUseCase.participateOrCancel(eq(1L), isNull(), any(), eq(10L)))
                    .willReturn(new ImmersiveParticipateResult(1L, ImmersiveVoteAction.VOTED, 10L, List.of(), 4));

            mockMvc.perform(put("/api/immersive-votes/1/participate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ParticipateRequest(10L))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.action").value("VOTED"))
                    .andExpect(jsonPath("$.remainingFreeVotes").value(4));
        }
    }

    @Nested
    class getLive {

        @Test
        @WithMockUser
        void 실시간_결과_200_반환() throws Exception {
            List<LiveOptionItem> options = List.of(
                    new LiveOptionItem(10L, 102, 78),
                    new LiveOptionItem(11L, 29, 22)
            );
            given(immersiveVoteQueryUseCase.getLive(1L))
                    .willReturn(new ImmersiveLiveResult(options, 14, 131));

            mockMvc.perform(get("/api/immersive-votes/1/live"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.options[0].optionId").value(10))
                    .andExpect(jsonPath("$.options[0].voteCount").value(102))
                    .andExpect(jsonPath("$.options[0].ratio").value(78))
                    .andExpect(jsonPath("$.options[1].optionId").value(11))
                    .andExpect(jsonPath("$.currentViewerCount").value(14))
                    .andExpect(jsonPath("$.totalParticipantCount").value(131));
        }

        @Test
        @WithMockUser
        void 존재하지_않는_투표_404() throws Exception {
            given(immersiveVoteQueryUseCase.getLive(999L))
                    .willThrow(new VoteNotFoundException());

            mockMvc.perform(get("/api/immersive-votes/999/live"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("VOTE_NOT_FOUND"));
        }
    }

    @Nested
    class 투표_취소 {

        @Test
        void 같은_옵션_재클릭시_CANCELED_반환() throws Exception {
            given(immersiveVoteCommandUseCase.participateOrCancel(eq(1L), eq(1L), any(), eq(10L)))
                    .willReturn(new ImmersiveParticipateResult(1L, ImmersiveVoteAction.CANCELED, null, List.of(), null));

            mockMvc.perform(put("/api/immersive-votes/1/participate")
                            .with(authentication(AUTH)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ParticipateRequest(10L))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.action").value("CANCELED"))
                    .andExpect(jsonPath("$.selectedOptionId").doesNotExist());
        }
    }

    @Nested
    class 비회원_무료투표 {

        @Test
        @WithMockUser
        void 무료투표_초과_403_반환() throws Exception {
            given(immersiveVoteCommandUseCase.participateOrCancel(eq(1L), isNull(), any(), eq(10L)))
                    .willThrow(new VoteFreeLimitExceededException());

            mockMvc.perform(put("/api/immersive-votes/1/participate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ParticipateRequest(10L))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("VOTE_FREE_LIMIT_EXCEEDED"));
        }

        @Test
        @WithMockUser
        void 옵션_변경은_무료투표_차감_안함() throws Exception {
            List<OptionResult> options = List.of(
                    new OptionResult(10L, "스윙칩만 3달 먹기", 99, 76),
                    new OptionResult(11L, "스윙스한테 30만원 주기", 32, 24)
            );
            given(immersiveVoteCommandUseCase.participateOrCancel(eq(1L), isNull(), any(), eq(11L)))
                    .willReturn(new ImmersiveParticipateResult(1L, ImmersiveVoteAction.VOTED, 11L, options, 2));

            mockMvc.perform(put("/api/immersive-votes/1/participate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ParticipateRequest(11L))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.action").value("VOTED"))
                    .andExpect(jsonPath("$.selectedOptionId").value(11))
                    .andExpect(jsonPath("$.remainingFreeVotes").value(2));
        }
    }

    @Nested
    class 종료된_투표 {

        @Test
        void 종료된_투표_참여_403() throws Exception {
            given(immersiveVoteCommandUseCase.participateOrCancel(eq(1L), eq(1L), any(), eq(10L)))
                    .willThrow(new VoteEndedException());

            mockMvc.perform(put("/api/immersive-votes/1/participate")
                            .with(authentication(AUTH)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ParticipateRequest(10L))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("VOTE_ENDED"));
        }
    }

    @Nested
    class 피드_페이징 {

        @Test
        @WithMockUser
        void 커서_기반_페이징_동작() throws Exception {
            List<FeedOptionItem> options = List.of(
                    new FeedOptionItem(10L, "옵션A", 102L, 78),
                    new FeedOptionItem(11L, "옵션B", 29L, 22)
            );
            Map<VoteEmoji, Long> emojiSummary = Map.of(
                    VoteEmoji.LIKE, 10L, VoteEmoji.SAD, 5L, VoteEmoji.ANGRY, 3L, VoteEmoji.WOW, 2L
            );
            ImmersiveFeedItem item = new ImmersiveFeedItem(
                    1L, "제목", "내용", "https://img.url",
                    Instant.parse("2025-04-27T14:59:00Z"), options, true, 10L,
                    emojiSummary, 20L, VoteEmoji.LIKE, 5, 13
            );
            given(immersiveVoteQueryUseCase.getFeed(eq(100L), eq(10), any(), any()))
                    .willReturn(new ImmersiveFeedResult(List.of(item), 80L, true));

            mockMvc.perform(get("/api/immersive-votes")
                            .param("cursor", "100")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.votes[0].voteId").value(1))
                    .andExpect(jsonPath("$.votes[0].title").value("제목"))
                    .andExpect(jsonPath("$.votes[0].content").value("내용"))
                    .andExpect(jsonPath("$.votes[0].myVote.voted").value(true))
                    .andExpect(jsonPath("$.votes[0].myVote.selectedOptionId").value(10))
                    .andExpect(jsonPath("$.nextCursor").value(80))
                    .andExpect(jsonPath("$.hasNext").value(true));
        }

        @Test
        @WithMockUser
        void 마지막_페이지_hasNext_false() throws Exception {
            given(immersiveVoteQueryUseCase.getFeed(isNull(), eq(10), any(), any()))
                    .willReturn(new ImmersiveFeedResult(List.of(), null, false));

            mockMvc.perform(get("/api/immersive-votes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasNext").value(false))
                    .andExpect(jsonPath("$.nextCursor").doesNotExist());
        }
    }

    @Nested
    class getShareLink {

        @Test
        @WithMockUser
        void 공유링크_200_반환() throws Exception {
            given(voteResultQueryUseCase.getShareLink(1L))
                    .willReturn(new ShareLinkResult("https://vs.app/poll/result/1", "제목", "https://cdn.vs.app/thumb/1.jpg"));

            mockMvc.perform(get("/api/immersive-votes/1/share"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.shareUrl").value("https://vs.app/poll/result/1"))
                    .andExpect(jsonPath("$.title").value("제목"))
                    .andExpect(jsonPath("$.thumbnailUrl").value("https://cdn.vs.app/thumb/1.jpg"));
        }

        @Test
        @WithMockUser
        void 존재하지_않는_투표_공유링크_404() throws Exception {
            given(voteResultQueryUseCase.getShareLink(999L))
                    .willThrow(new VoteNotFoundException());

            mockMvc.perform(get("/api/immersive-votes/999/share"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("VOTE_NOT_FOUND"));
        }
    }
}
