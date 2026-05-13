package com.ject.vs.vote.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ject.vs.config.OAuth2LoginSuccessHandler;
import com.ject.vs.service.CustomOAuth2UserService;
import com.ject.vs.util.CookieUtil;
import com.ject.vs.util.JwtProvider;
import com.ject.vs.vote.adapter.web.dto.ParticipateRequest;
import com.ject.vs.vote.domain.ImmersiveVoteAction;
import com.ject.vs.vote.port.in.ImmersiveVoteCommandUseCase;
import com.ject.vs.vote.port.in.ImmersiveVoteCommandUseCase.ImmersiveParticipateResult;
import com.ject.vs.vote.port.in.ImmersiveVoteQueryUseCase;
import com.ject.vs.vote.port.in.ImmersiveVoteQueryUseCase.ImmersiveFeedResult;
import com.ject.vs.vote.port.in.ImmersiveVoteQueryUseCase.ImmersiveLiveResult;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ImmersiveVoteController.class)
class ImmersiveVoteControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ImmersiveVoteCommandUseCase immersiveVoteCommandUseCase;
    @MockBean ImmersiveVoteQueryUseCase immersiveVoteQueryUseCase;
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
            given(immersiveVoteQueryUseCase.getLive(1L))
                    .willReturn(new ImmersiveLiveResult(1L, 60, 40, 10, 0));

            mockMvc.perform(get("/api/immersive-votes/1/live"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.optionARatio").value(60))
                    .andExpect(jsonPath("$.optionBRatio").value(40));
        }
    }
}
