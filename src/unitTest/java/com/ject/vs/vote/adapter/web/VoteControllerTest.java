package com.ject.vs.vote.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ject.vs.config.OAuth2LoginSuccessHandler;
import com.ject.vs.config.TestPropertiesConfig;
import com.ject.vs.notification.port.out.PushSenderPort;
import com.ject.vs.vote.port.in.VoteParticipationQueryUseCase;
import org.springframework.context.annotation.Import;
import com.ject.vs.auth.port.CustomOAuth2UserService;
import com.ject.vs.util.CookieUtil;
import com.ject.vs.util.JwtProvider;
import com.ject.vs.vote.adapter.web.dto.ParticipateRequest;
import com.ject.vs.vote.adapter.web.dto.VoteCreateRequest;
import com.ject.vs.vote.domain.*;
import com.ject.vs.vote.exception.InvalidOptionException;
import com.ject.vs.vote.exception.VoteEndedException;
import com.ject.vs.vote.exception.VoteFreeLimitExceededException;
import com.ject.vs.vote.exception.VoteNotFoundException;
import com.ject.vs.vote.port.VoteDetailQueryService;
import com.ject.vs.vote.port.VoteDetailQueryService.VoteDetailResult;
import com.ject.vs.vote.port.in.VoteCommandUseCase;
import com.ject.vs.vote.port.in.VoteCommandUseCase.OptionResult;
import com.ject.vs.vote.port.in.VoteCommandUseCase.ParticipateResult;
import com.ject.vs.vote.port.in.VoteCommandUseCase.VoteCreateResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VoteController.class)
@Import(TestPropertiesConfig.class)
class VoteControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean VoteCommandUseCase voteCommandUseCase;
    @MockBean VoteDetailQueryService voteDetailQueryService;
    @MockBean JwtProvider jwtProvider;
    @MockBean CookieUtil cookieUtil;
    @MockBean OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    @MockBean CustomOAuth2UserService customOAuth2UserService;
    @MockBean PushSenderPort pushSenderPort;
    @MockBean VoteParticipationQueryUseCase voteParticipationQueryUseCase;

    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList());

    private VoteCreateRequest validCreateRequest() {
        return new VoteCreateRequest(
                "제목", null, "thumb.png", null,
                VoteDuration.HOURS_24, "옵션A", "옵션B"
        );
    }

    private VoteDetailResult sampleDetail() {
        return new VoteDetailResult(
                1L, "제목", Instant.parse("2025-01-01T00:00:00Z"),
                null, "thumb.png", null, VoteStatus.ONGOING, Instant.parse("2025-01-02T00:00:00Z"),
                5, List.of(), false, null, Map.of(), null, 0
        );
    }

    @Nested
    class create {

        @Test
        void 인증된_회원은_201_반환() throws Exception {
            given(voteCommandUseCase.create(any())).willReturn(
                    new VoteCreateResult(1L, VoteStatus.ONGOING, Instant.parse("2025-01-02T00:00:00Z")));

            mockMvc.perform(post("/api/votes")
                            .with(authentication(AUTH)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.voteId").value(1));
        }

        @Test
        void 비인증_요청은_401_반환() throws Exception {
            mockMvc.perform(post("/api/votes")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class getDetail {

        @Test
        @WithMockUser
        void 투표_상세_200_반환() throws Exception {
            given(voteDetailQueryService.getDetail(eq(1L), any(), any())).willReturn(sampleDetail());

            mockMvc.perform(get("/api/votes/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.voteId").value(1))
                    .andExpect(jsonPath("$.title").value("제목"));
        }

        @Test
        @WithMockUser
        void 존재하지_않는_투표는_404() throws Exception {
            given(voteDetailQueryService.getDetail(eq(99L), any(), any()))
                    .willThrow(new VoteNotFoundException());

            mockMvc.perform(get("/api/votes/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("VOTE_NOT_FOUND"));
        }
    }

    @Nested
    class participate {

        @Test
        void 회원_참여_200_반환() throws Exception {
            given(voteCommandUseCase.participateAsMember(eq(1L), eq(1L), eq(10L)))
                    .willReturn(new ParticipateResult(1L, 10L, List.of(), 1, null));

            mockMvc.perform(post("/api/votes/1/participate")
                            .with(authentication(AUTH)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ParticipateRequest(10L))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.selectedOptionId").value(10));
        }

        @Test
        void 종료된_투표_참여_403() throws Exception {
            given(voteCommandUseCase.participateAsMember(any(), any(), any()))
                    .willThrow(new VoteEndedException());

            mockMvc.perform(post("/api/votes/1/participate")
                            .with(authentication(AUTH)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ParticipateRequest(10L))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("VOTE_ENDED"));
        }
    }

    @Nested
    class cancel {

        @Test
        void 회원_취소_204_반환() throws Exception {
            willDoNothing().given(voteCommandUseCase).cancel(eq(1L), eq(1L));

            mockMvc.perform(delete("/api/votes/1/participate")
                            .with(authentication(AUTH)).with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        void 비인증_취소_401_반환() throws Exception {
            mockMvc.perform(delete("/api/votes/1/participate")
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void 종료된_투표_취소_403() throws Exception {
            willThrow(new VoteEndedException()).given(voteCommandUseCase).cancel(any(), any());

            mockMvc.perform(delete("/api/votes/1/participate")
                            .with(authentication(AUTH)).with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("VOTE_ENDED"));
        }
    }

    @Nested
    class 비회원_참여 {

        @Test
        @WithMockUser
        void 비회원_참여_성공_200_반환() throws Exception {
            List<OptionResult> options = List.of(
                    new OptionResult(10L, "혼밥이 편하다", 22L, 70),
                    new OptionResult(11L, "같이 먹기", 9L, 30)
            );
            given(voteCommandUseCase.participateAsGuest(eq(1L), any(), eq(10L)))
                    .willReturn(new ParticipateResult(1L, 10L, options, 31, 4));

            mockMvc.perform(post("/api/votes/1/participate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ParticipateRequest(10L))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.voteId").value(1))
                    .andExpect(jsonPath("$.selectedOptionId").value(10))
                    .andExpect(jsonPath("$.participantCount").value(31))
                    .andExpect(jsonPath("$.remainingFreeVotes").value(4))
                    .andExpect(jsonPath("$.options[0].voteCount").value(22))
                    .andExpect(jsonPath("$.options[0].ratio").value(70));
        }

        @Test
        @WithMockUser
        void 비회원_무료투표_초과_403_반환() throws Exception {
            given(voteCommandUseCase.participateAsGuest(eq(1L), any(), any()))
                    .willThrow(new VoteFreeLimitExceededException());

            mockMvc.perform(post("/api/votes/1/participate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ParticipateRequest(10L))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("VOTE_FREE_LIMIT_EXCEEDED"));
        }

        @Test
        @WithMockUser
        void 유효하지_않은_옵션_400_반환() throws Exception {
            given(voteCommandUseCase.participateAsGuest(eq(1L), any(), eq(999L)))
                    .willThrow(new InvalidOptionException());

            mockMvc.perform(post("/api/votes/1/participate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ParticipateRequest(999L))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_OPTION"));
        }
    }

    @Nested
    class 투표_상세_응답_분기 {

        @Test
        @WithMockUser
        void 비회원_조회_voteCount는_null() throws Exception {
            VoteDetailResult result = new VoteDetailResult(
                    1L, "제목", Instant.parse("2025-01-01T00:00:00Z"),
                    "내용", "thumb.png", null, VoteStatus.ONGOING, Instant.parse("2025-01-02T00:00:00Z"),
                    31, List.of(), false, null, Map.of(), null, 0
            );
            given(voteDetailQueryService.getDetail(eq(1L), any(), any())).willReturn(result);

            mockMvc.perform(get("/api/votes/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.voteId").value(1))
                    .andExpect(jsonPath("$.participantCount").value(31))
                    .andExpect(jsonPath("$.myVote.voted").value(false))
                    .andExpect(jsonPath("$.myVote.selectedOptionId").doesNotExist());
        }

        @Test
        void 회원_투표후_조회_voteCount_노출() throws Exception {
            VoteDetailResult result = new VoteDetailResult(
                    1L, "제목", Instant.parse("2025-01-01T00:00:00Z"),
                    "내용", "thumb.png", null, VoteStatus.ONGOING, Instant.parse("2025-01-02T00:00:00Z"),
                    31, List.of(), true, 10L, Map.of(VoteEmoji.WOW, 36L), VoteEmoji.WOW, 0
            );
            given(voteDetailQueryService.getDetail(eq(1L), eq(1L), any())).willReturn(result);

            mockMvc.perform(get("/api/votes/1")
                            .with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.myVote.voted").value(true))
                    .andExpect(jsonPath("$.myVote.selectedOptionId").value(10))
                    .andExpect(jsonPath("$.myEmoji").value("WOW"));
        }

        @Test
        @WithMockUser
        void 종료된_투표_미투표시에도_voteCount_노출() throws Exception {
            List<OptionResult> options = List.of(
                    new OptionResult(10L, "짜장면", 60L, 60),
                    new OptionResult(11L, "짬뽕", 40L, 40)
            );
            VoteDetailResult result = new VoteDetailResult(
                    1L, VoteType.GENERAL, "점심 뭐 먹을까?", Instant.parse("2025-01-01T00:00:00Z"),
                    "내용", "thumb.png", null, VoteStatus.ENDED, Instant.parse("2025-01-02T00:00:00Z"),
                    100, options, false, null, Map.of(), null, 0
            );
            given(voteDetailQueryService.getDetail(eq(1L), any(), any())).willReturn(result);

            mockMvc.perform(get("/api/votes/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ENDED"))
                    .andExpect(jsonPath("$.myVote.voted").value(false))
                    .andExpect(jsonPath("$.options[0].voteCount").value(60))
                    .andExpect(jsonPath("$.options[0].ratio").value(60))
                    .andExpect(jsonPath("$.options[1].voteCount").value(40))
                    .andExpect(jsonPath("$.options[1].ratio").value(40));
        }

        @Test
        @WithMockUser
        void 진행중_투표_미투표시_voteCount는_null() throws Exception {
            List<OptionResult> options = List.of(
                    new OptionResult(10L, "짜장면", 60L, 60),
                    new OptionResult(11L, "짬뽕", 40L, 40)
            );
            VoteDetailResult result = new VoteDetailResult(
                    1L, VoteType.GENERAL, "점심 뭐 먹을까?", Instant.parse("2025-01-01T00:00:00Z"),
                    "내용", "thumb.png", null, VoteStatus.ONGOING, Instant.parse("2025-01-02T00:00:00Z"),
                    100, options, false, null, Map.of(), null, 0
            );
            given(voteDetailQueryService.getDetail(eq(1L), any(), any())).willReturn(result);

            mockMvc.perform(get("/api/votes/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ONGOING"))
                    .andExpect(jsonPath("$.myVote.voted").value(false))
                    .andExpect(jsonPath("$.options[0].voteCount").doesNotExist())
                    .andExpect(jsonPath("$.options[0].ratio").doesNotExist())
                    .andExpect(jsonPath("$.options[1].voteCount").doesNotExist())
                    .andExpect(jsonPath("$.options[1].ratio").doesNotExist());
        }
    }
}
