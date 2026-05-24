package com.ject.vs.vote.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ject.vs.config.OAuth2LoginSuccessHandler;
import com.ject.vs.config.TestPropertiesConfig;
import org.springframework.context.annotation.Import;
import com.ject.vs.auth.port.CustomOAuth2UserService;
import com.ject.vs.util.CookieUtil;
import com.ject.vs.util.JwtProvider;
import com.ject.vs.vote.adapter.web.dto.EmojiRequest;
import com.ject.vs.vote.domain.VoteEmoji;
import com.ject.vs.vote.exception.VoteNotFoundException;
import com.ject.vs.vote.port.in.VoteEmojiCommandUseCase;
import com.ject.vs.vote.port.in.VoteEmojiCommandUseCase.EmojiResult;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VoteEmojiController.class)
@Import(TestPropertiesConfig.class)
class VoteEmojiControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean VoteEmojiCommandUseCase voteEmojiCommandUseCase;
    @MockBean JwtProvider jwtProvider;
    @MockBean CookieUtil cookieUtil;
    @MockBean OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    @MockBean CustomOAuth2UserService customOAuth2UserService;

    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList());

    private EmojiResult sampleResult(VoteEmoji myEmoji) {
        return new EmojiResult(Map.of(VoteEmoji.LIKE, 5L, VoteEmoji.SAD, 0L,
                VoteEmoji.ANGRY, 0L, VoteEmoji.WOW, 0L), 5L, myEmoji);
    }

    @Nested
    class reactOnVote {

        @Test
        void 회원_이모지_반응_200_반환() throws Exception {
            given(voteEmojiCommandUseCase.reactAsMember(eq(1L), eq(1L), eq(VoteEmoji.LIKE)))
                    .willReturn(sampleResult(VoteEmoji.LIKE));

            mockMvc.perform(put("/api/votes/1/emoji")
                            .with(authentication(AUTH)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new EmojiRequest(VoteEmoji.LIKE))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.myEmoji").value("LIKE"));
        }

        @Test
        @WithMockUser
        void 비회원_이모지_반응_200_반환() throws Exception {
            given(voteEmojiCommandUseCase.reactAsGuest(eq(1L), any(), eq(VoteEmoji.WOW)))
                    .willReturn(sampleResult(VoteEmoji.WOW));

            mockMvc.perform(put("/api/votes/1/emoji")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new EmojiRequest(VoteEmoji.WOW))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.myEmoji").value("WOW"));
        }

        @Test
        void null_이모지로_취소_200_반환() throws Exception {
            given(voteEmojiCommandUseCase.reactAsMember(eq(1L), eq(1L), isNull()))
                    .willReturn(sampleResult(null));

            mockMvc.perform(put("/api/votes/1/emoji")
                            .with(authentication(AUTH)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"emoji\":null}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.myEmoji").doesNotExist());
        }
    }

    @Nested
    class reactOnImmersiveVote {

        @Test
        void 몰입형_이모지_반응_200_반환() throws Exception {
            given(voteEmojiCommandUseCase.reactAsMember(eq(1L), eq(1L), eq(VoteEmoji.SAD)))
                    .willReturn(sampleResult(VoteEmoji.SAD));

            mockMvc.perform(put("/api/immersive-votes/1/emoji")
                            .with(authentication(AUTH)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new EmojiRequest(VoteEmoji.SAD))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.myEmoji").value("SAD"));
        }

        @Test
        @WithMockUser
        void 몰입형_비회원_이모지_반응_200_반환() throws Exception {
            given(voteEmojiCommandUseCase.reactAsGuest(eq(1L), any(), eq(VoteEmoji.ANGRY)))
                    .willReturn(sampleResult(VoteEmoji.ANGRY));

            mockMvc.perform(put("/api/immersive-votes/1/emoji")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new EmojiRequest(VoteEmoji.ANGRY))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.myEmoji").value("ANGRY"));
        }
    }

    @Nested
    class 이모지_교체_및_취소 {

        @Test
        void 다른_이모지_선택시_기존_반응_자동_교체() throws Exception {
            EmojiResult result = new EmojiResult(
                    Map.of(VoteEmoji.LIKE, 4L, VoteEmoji.SAD, 0L, VoteEmoji.ANGRY, 0L, VoteEmoji.WOW, 1L),
                    5L, VoteEmoji.WOW
            );
            given(voteEmojiCommandUseCase.reactAsMember(eq(1L), eq(1L), eq(VoteEmoji.WOW)))
                    .willReturn(result);

            mockMvc.perform(put("/api/votes/1/emoji")
                            .with(authentication(AUTH)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new EmojiRequest(VoteEmoji.WOW))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.myEmoji").value("WOW"))
                    .andExpect(jsonPath("$.emojiSummary.LIKE").value(4))
                    .andExpect(jsonPath("$.emojiSummary.WOW").value(1))
                    .andExpect(jsonPath("$.emojiSummary.total").value(5));
        }

        @Test
        void 같은_이모지_재클릭시_취소() throws Exception {
            EmojiResult result = new EmojiResult(
                    Map.of(VoteEmoji.LIKE, 4L, VoteEmoji.SAD, 0L, VoteEmoji.ANGRY, 0L, VoteEmoji.WOW, 0L),
                    4L, null
            );
            given(voteEmojiCommandUseCase.reactAsMember(eq(1L), eq(1L), eq(VoteEmoji.WOW)))
                    .willReturn(result);

            mockMvc.perform(put("/api/votes/1/emoji")
                            .with(authentication(AUTH)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new EmojiRequest(VoteEmoji.WOW))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.myEmoji").doesNotExist());
        }
    }

    @Nested
    class 에러_케이스 {

        @Test
        @WithMockUser
        void 존재하지_않는_투표_404() throws Exception {
            given(voteEmojiCommandUseCase.reactAsGuest(eq(999L), any(), any()))
                    .willThrow(new VoteNotFoundException());

            mockMvc.perform(put("/api/votes/999/emoji")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new EmojiRequest(VoteEmoji.LIKE))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("VOTE_NOT_FOUND"));
        }
    }
}
