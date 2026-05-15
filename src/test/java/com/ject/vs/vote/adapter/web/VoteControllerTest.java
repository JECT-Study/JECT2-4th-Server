package com.ject.vs.vote.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ject.vs.config.OAuth2LoginSuccessHandler;
import com.ject.vs.auth.port.CustomOAuth2UserService;
import com.ject.vs.util.CookieUtil;
import com.ject.vs.util.JwtProvider;
import com.ject.vs.vote.adapter.web.dto.ParticipateRequest;
import com.ject.vs.vote.adapter.web.dto.VoteCreateRequest;
import com.ject.vs.vote.domain.*;
import com.ject.vs.vote.exception.VoteEndedException;
import com.ject.vs.vote.exception.VoteNotFoundException;
import com.ject.vs.vote.port.VoteDetailQueryService;
import com.ject.vs.vote.port.VoteDetailQueryService.VoteDetailResult;
import com.ject.vs.vote.port.in.VoteCommandUseCase;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VoteController.class)
class VoteControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean VoteCommandUseCase voteCommandUseCase;
    @MockBean VoteDetailQueryService voteDetailQueryService;
    @MockBean JwtProvider jwtProvider;
    @MockBean CookieUtil cookieUtil;
    @MockBean OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    @MockBean CustomOAuth2UserService customOAuth2UserService;

    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList());

    private VoteCreateRequest validCreateRequest() {
        return new VoteCreateRequest(
                VoteType.GENERAL, "제목", null, "thumb.png", null,
                VoteDuration.HOURS_24, "옵션A", "옵션B"
        );
    }

    private VoteDetailResult sampleDetail() {
        return new VoteDetailResult(
                1L, VoteType.GENERAL, "제목", null, "thumb.png", null,
                VoteStatus.ONGOING, Instant.parse("2025-01-02T00:00:00Z"),
                5, List.of(), null, Map.of(), null
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
    }
}
