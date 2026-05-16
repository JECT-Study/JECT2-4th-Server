package com.ject.vs.vote.adapter.web;

import com.ject.vs.config.OAuth2LoginSuccessHandler;
import com.ject.vs.config.TestPropertiesConfig;
import org.springframework.context.annotation.Import;
import com.ject.vs.auth.port.CustomOAuth2UserService;
import com.ject.vs.util.CookieUtil;
import com.ject.vs.util.JwtProvider;
import com.ject.vs.vote.domain.VoteStatus;
import com.ject.vs.vote.exception.VoteNotFoundException;
import com.ject.vs.vote.exception.VoteNotEndedException;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase.ShareLinkResult;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase.VoteResultDetail;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VoteResultController.class)
@Import(TestPropertiesConfig.class)
class VoteResultControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean VoteResultQueryUseCase voteResultQueryUseCase;
    @MockBean JwtProvider jwtProvider;
    @MockBean CookieUtil cookieUtil;
    @MockBean OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    @MockBean CustomOAuth2UserService customOAuth2UserService;

    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList());

    private VoteResultDetail sampleResult() {
        return new VoteResultDetail(1L, "제목", VoteStatus.ENDED,
                Instant.parse("2025-01-01T01:00:00Z"), 10, List.of(), null,
                VoteResultQueryUseCase.Insight.ofLocked(), VoteResultQueryUseCase.AiInsightView.unavailable());
    }

    @Nested
    class getResult {

        @Test
        void 회원_결과_200_반환() throws Exception {
            given(voteResultQueryUseCase.getResult(eq(1L), eq(1L))).willReturn(sampleResult());

            mockMvc.perform(get("/api/votes/1/result").with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.voteId").value(1))
                    .andExpect(jsonPath("$.title").value("제목"));
        }

        @Test
        @WithMockUser
        void 비회원_결과_200_반환() throws Exception {
            given(voteResultQueryUseCase.getResult(eq(1L), isNull())).willReturn(sampleResult());

            mockMvc.perform(get("/api/votes/1/result"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        void 진행중인_투표_403() throws Exception {
            given(voteResultQueryUseCase.getResult(any(), any())).willThrow(new VoteNotEndedException());

            mockMvc.perform(get("/api/votes/1/result"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("VOTE_NOT_ENDED"));
        }

        @Test
        @WithMockUser
        void 존재하지_않는_투표_404() throws Exception {
            given(voteResultQueryUseCase.getResult(any(), any())).willThrow(new VoteNotFoundException());

            mockMvc.perform(get("/api/votes/99/result"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class getShareLink {

        @Test
        @WithMockUser
        void 공유링크_200_반환() throws Exception {
            given(voteResultQueryUseCase.getShareLink(1L))
                    .willReturn(new ShareLinkResult("https://vs.app/poll/result/1"));

            mockMvc.perform(get("/api/votes/1/share"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value("https://vs.app/poll/result/1"));
        }
    }
}
