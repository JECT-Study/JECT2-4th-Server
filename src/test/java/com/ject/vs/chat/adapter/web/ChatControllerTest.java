package com.ject.vs.chat.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ject.vs.chat.adapter.web.dto.MarkAsReadRequest;
import com.ject.vs.chat.adapter.web.dto.SendMessageRequest;
import com.ject.vs.chat.exception.ChatForbiddenException;
import com.ject.vs.chat.port.in.*;
import com.ject.vs.util.CookieUtil;
import com.ject.vs.util.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
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

    @Test
    @WithMockUser
    void getChatList_인증된_사용자는_200을_반환한다() throws Exception {
        given(chatQueryUseCase.getChatList(any(), any())).willReturn(List.of());

        mockMvc.perform(get("/api/chats")
                        .param("status", "ONGOING")
                        .with(user("1").roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    void getChatList_인증_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/chats")
                        .param("status", "ONGOING"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getChatRoom_200을_반환한다() throws Exception {
        given(chatQueryUseCase.getChatRoom(1L)).willReturn(
                new ChatRoomResult(1L, "투표 #1", VoteStatus.ONGOING, 5, "옵션 A", "옵션 B", LocalDateTime.now().plusDays(1))
        );

        mockMvc.perform(get("/api/chats/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getGauge_200을_반환한다() throws Exception {
        given(chatQueryUseCase.getGauge(1L)).willReturn(new GaugeResult(50, 50, 10));

        mockMvc.perform(get("/api/chats/1/gauge"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getMessages_200을_반환한다() throws Exception {
        given(chatQueryUseCase.getMessages(anyLong(), any(), any(), anyInt()))
                .willReturn(new MessagePageResult(List.of(), null, false));

        mockMvc.perform(get("/api/chats/1/messages")
                        .with(user("1").roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void sendMessage_정상이면_201을_반환한다() throws Exception {
        MessageResult result = new MessageResult(1L, "hello", LocalDateTime.now(), "nick", null, "A", true);
        given(chatCommandUseCase.sendMessage(any(SendMessageCommand.class))).willReturn(result);

        mockMvc.perform(post("/api/chats/1/messages")
                        .with(user("1").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("hello"))))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void sendMessage_미참여자는_403을_반환한다() throws Exception {
        given(chatCommandUseCase.sendMessage(any(SendMessageCommand.class)))
                .willThrow(new ChatForbiddenException());

        mockMvc.perform(post("/api/chats/1/messages")
                        .with(user("1").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("hello"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void markAsRead_204를_반환한다() throws Exception {
        mockMvc.perform(post("/api/chats/1/read")
                        .with(user("1").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MarkAsReadRequest(10L))))
                .andExpect(status().isNoContent());
    }
}
