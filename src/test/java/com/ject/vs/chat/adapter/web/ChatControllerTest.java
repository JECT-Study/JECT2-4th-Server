package com.ject.vs.chat.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ject.vs.chat.adapter.web.dto.MarkAsReadRequest;
import com.ject.vs.chat.adapter.web.dto.SendMessageRequest;
import com.ject.vs.chat.exception.ChatForbiddenException;
import com.ject.vs.chat.port.in.*;
import com.ject.vs.chat.port.in.dto.*;
import com.ject.vs.config.OAuth2LoginSuccessHandler;
import com.ject.vs.auth.port.CustomOAuth2UserService;
import com.ject.vs.config.TestPropertiesConfig;
import org.springframework.context.annotation.Import;
import com.ject.vs.vote.domain.VoteOptionCode;
import com.ject.vs.vote.domain.VoteStatus;
import com.ject.vs.util.CookieUtil;
import com.ject.vs.util.JwtProvider;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@Import(TestPropertiesConfig.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatQueryUseCase chatQueryUseCase;

    @MockBean
    private ChatCommandUseCase chatCommandUseCase;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private CookieUtil cookieUtil;

    @MockBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @Nested
    class getChatList {

        @Test
        @WithMockUser
        void 인증된_사용자는_200을_반환한다() throws Exception {
            // given
            given(chatQueryUseCase.getChatList(any(), any())).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/chats")
                            .param("status", "ONGOING")
                            .with(user("1").roles("USER")))
                    .andExpect(status().isOk());
        }

        @Test
        void 인증_없으면_401을_반환한다() throws Exception {
            // given
            // (no auth)

            // when & then
            mockMvc.perform(get("/api/chats")
                            .param("status", "ONGOING"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class getChatRoom {

        @Test
        @WithMockUser
        void 정상_요청이면_200을_반환한다() throws Exception {
            // given
            given(chatQueryUseCase.getChatRoom(1L)).willReturn(
                    new ChatRoomResult(1L, "투표 #1", VoteStatus.ONGOING, 5, "옵션 A", "옵션 B", Instant.now().plus(java.time.Duration.ofDays(1)))
            );

            // when & then
            mockMvc.perform(get("/api/chats/1"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class getGauge {

        @Test
        @WithMockUser
        void 정상_요청이면_200을_반환한다() throws Exception {
            // given
            given(chatQueryUseCase.getGauge(1L)).willReturn(new GaugeResult(50, 50, 10));

            // when & then
            mockMvc.perform(get("/api/chats/1/gauge"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class getMessages {

        @Test
        @WithMockUser
        void 정상_요청이면_200을_반환한다() throws Exception {
            // given
            given(chatQueryUseCase.getMessages(anyLong(), any(), any(), anyInt()))
                    .willReturn(new MessagePageResult(List.of(), null, false));

            // when & then
            mockMvc.perform(get("/api/chats/1/messages")
                            .with(user("1").roles("USER")))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class sendMessage {

        @Test
        @WithMockUser
        void 정상이면_201을_반환한다() throws Exception {
            // given
            MessageResult result = new MessageResult(1L, "hello", Instant.now(), "nick", null, VoteOptionCode.A, true);
            given(chatCommandUseCase.sendMessage(any(SendMessageCommand.class))).willReturn(result);

            // when & then
            mockMvc.perform(post("/api/chats/1/messages")
                            .with(user("1").roles("USER"))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SendMessageRequest("hello"))))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser
        void 미참여자는_403을_반환한다() throws Exception {
            // given
            given(chatCommandUseCase.sendMessage(any(SendMessageCommand.class)))
                    .willThrow(new ChatForbiddenException());

            // when & then
            mockMvc.perform(post("/api/chats/1/messages")
                            .with(user("1").roles("USER"))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SendMessageRequest("hello"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class markAsRead {

        @Test
        @WithMockUser
        void 정상이면_204를_반환한다() throws Exception {
            // given
            // (no setup needed)

            // when & then
            mockMvc.perform(post("/api/chats/1/read")
                            .with(user("1").roles("USER"))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new MarkAsReadRequest(10L))))
                    .andExpect(status().isNoContent());
        }
    }
}
