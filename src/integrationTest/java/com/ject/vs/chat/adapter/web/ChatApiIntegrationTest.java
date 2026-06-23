package com.ject.vs.chat.adapter.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ject.vs.support.ChatIntegrationTestSupport;
import com.ject.vs.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Chat API 통합 테스트")
class ChatApiIntegrationTest extends ChatIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("GET /api/chats/{voteId}")
    class GetChatRoom {

        @Test
        @DisplayName("참여자별 myVoteOption이 올바르게 반환된다")
        void 참여자별_myVoteOption_반환() throws Exception {
            ChatRoomFixture fixture = createChatRoomFixture("배지 테스트 투표");

            mockMvc.perform(get("/api/chats/{voteId}", fixture.voteId())
                            .with(asUser(fixture.userA().getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.voteId").value(fixture.voteId()))
                    .andExpect(jsonPath("$.myVoteOption").value("A"))
                    .andExpect(jsonPath("$.participantCount").value(2));

            mockMvc.perform(get("/api/chats/{voteId}", fixture.voteId())
                            .with(asUser(fixture.userB().getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.myVoteOption").value("B"));
        }
    }

    @Nested
    @DisplayName("POST /api/chats/{voteId}/messages")
    class SendMessage {

        @Test
        @DisplayName("일반 메시지 전송 시 201과 메시지 정보가 반환된다")
        void 일반_메시지_전송() throws Exception {
            ChatRoomFixture fixture = createChatRoomFixture("메시지 전송 테스트");

            mockMvc.perform(post("/api/chats/{voteId}/messages", fixture.voteId())
                            .with(asUser(fixture.userA().getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"content": "안녕하세요"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.content").value("안녕하세요"))
                    .andExpect(jsonPath("$.senderNickname").value("참여자A"))
                    .andExpect(jsonPath("$.senderVoteOption").value("A"))
                    .andExpect(jsonPath("$.isMine").value(true))
                    .andExpect(jsonPath("$.replyTo").doesNotExist())
                    .andExpect(jsonPath("$.reactions").isMap())
                    .andExpect(jsonPath("$.myReaction").doesNotExist());
        }

        @Test
        @DisplayName("답글 전송 시 replyTo 정보가 포함된다")
        void 답글_전송() throws Exception {
            ChatRoomFixture fixture = createChatRoomFixture("답글 전송 테스트");

            Long parentMessageId = sendMessageAndExtractId(fixture, fixture.userA().getId(), "원본 메시지", null);

            mockMvc.perform(post("/api/chats/{voteId}/messages", fixture.voteId())
                            .with(asUser(fixture.userB().getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"content": "답글입니다", "replyToMessageId": %d}
                                    """.formatted(parentMessageId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.content").value("답글입니다"))
                    .andExpect(jsonPath("$.replyTo.messageId").value(parentMessageId))
                    .andExpect(jsonPath("$.replyTo.senderNickname").value("참여자A"))
                    .andExpect(jsonPath("$.replyTo.contentPreview").value("원본 메시지"));
        }

        @Test
        @DisplayName("미참여자는 403을 반환한다")
        void 미참여자_403() throws Exception {
            ChatRoomFixture fixture = createChatRoomFixture("미참여자 테스트");
            User outsider = createUserWithNickname("외부인");

            mockMvc.perform(post("/api/chats/{voteId}/messages", fixture.voteId())
                            .with(asUser(outsider.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"content": "들어갈 수 없음"}
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("존재하지 않는 답글 대상은 404를 반환한다")
        void 없는_답글대상_404() throws Exception {
            ChatRoomFixture fixture = createChatRoomFixture("답글 대상 없음 테스트");

            mockMvc.perform(post("/api/chats/{voteId}/messages", fixture.voteId())
                            .with(asUser(fixture.userA().getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"content": "답글", "replyToMessageId": 999999}
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/chats/{voteId}/messages")
    class GetMessages {

        @Test
        @DisplayName("메시지 목록에 replyTo, reactions, myReaction이 포함된다")
        void 메시지_목록_enrichment() throws Exception {
            ChatRoomFixture fixture = createChatRoomFixture("메시지 조회 테스트");

            Long parentMessageId = sendMessageAndExtractId(fixture, fixture.userA().getId(), "부모 메시지", null);
            sendMessageAndExtractId(fixture, fixture.userB().getId(), "자식 메시지", parentMessageId);

            react(fixture, fixture.userB().getId(), parentMessageId, "THUMBS_UP");

            mockMvc.perform(get("/api/chats/{voteId}/messages", fixture.voteId())
                            .with(asUser(fixture.userB().getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages.length()").value(2))
                    .andExpect(jsonPath("$.messages[0].content").value("자식 메시지"))
                    .andExpect(jsonPath("$.messages[0].replyTo.messageId").value(parentMessageId))
                    .andExpect(jsonPath("$.messages[0].replyTo.senderNickname").value("참여자A"))
                    .andExpect(jsonPath("$.messages[0].isMine").value(true))
                    .andExpect(jsonPath("$.messages[1].content").value("부모 메시지"))
                    .andExpect(jsonPath("$.messages[1].reactions.THUMBS_UP").value(1))
                    .andExpect(jsonPath("$.messages[1].myReaction").value("THUMBS_UP"));
        }

        @Test
        @DisplayName("원문 메시지가 삭제되면 replyTo는 null이다")
        void 원문_삭제시_replyTo_null() throws Exception {
            ChatRoomFixture fixture = createChatRoomFixture("원문 삭제 테스트");

            Long parentMessageId = sendMessageAndExtractId(fixture, fixture.userA().getId(), "삭제될 원문", null);
            sendMessageAndExtractId(fixture, fixture.userB().getId(), "남는 답글", parentMessageId);

            entityManager.createNativeQuery("DELETE FROM chat_message WHERE id = :id")
                    .setParameter("id", parentMessageId)
                    .executeUpdate();
            entityManager.flush();
            entityManager.clear();

            mockMvc.perform(get("/api/chats/{voteId}/messages", fixture.voteId())
                            .with(asUser(fixture.userB().getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages.length()").value(1))
                    .andExpect(jsonPath("$.messages[0].content").value("남는 답글"))
                    .andExpect(jsonPath("$.messages[0].replyTo").doesNotExist());
        }

        @Test
        @DisplayName("탈퇴한 사용자의 원문 닉네임은 알 수 없음으로 표시된다")
        void 탈퇴_사용자_닉네임_알수없음() throws Exception {
            ChatRoomFixture fixture = createChatRoomFixture("탈퇴 사용자 테스트");

            Long parentMessageId = sendMessageAndExtractId(fixture, fixture.userA().getId(), "탈퇴 전 메시지", null);

            User withdrawnUser = userRepository.findById(fixture.userA().getId()).orElseThrow();
            withdrawnUser.withdraw(FIXED_NOW);
            userRepository.save(withdrawnUser);
            entityManager.flush();
            entityManager.clear();

            sendMessageAndExtractId(fixture, fixture.userB().getId(), "탈퇴 후 답글", parentMessageId);

            mockMvc.perform(get("/api/chats/{voteId}/messages", fixture.voteId())
                            .with(asUser(fixture.userB().getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages[0].replyTo.senderNickname").value("알 수 없음"));
        }
    }

    @Nested
    @DisplayName("PUT /api/chats/{voteId}/messages/{messageId}/reactions")
    class ReactToMessage {

        @Test
        @DisplayName("반응 추가, 변경, 취소가 정상 동작한다")
        void 반응_추가_변경_취소() throws Exception {
            ChatRoomFixture fixture = createChatRoomFixture("반응 테스트");

            Long messageId = sendMessageAndExtractId(fixture, fixture.userA().getId(), "반응 대상", null);

            mockMvc.perform(put("/api/chats/{voteId}/messages/{messageId}/reactions", fixture.voteId(), messageId)
                            .with(asUser(fixture.userB().getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"emoji": "THUMBS_UP"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messageId").value(messageId))
                    .andExpect(jsonPath("$.reactions.THUMBS_UP").value(1))
                    .andExpect(jsonPath("$.myReaction").value("THUMBS_UP"));

            mockMvc.perform(put("/api/chats/{voteId}/messages/{messageId}/reactions", fixture.voteId(), messageId)
                            .with(asUser(fixture.userB().getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"emoji": "THUMBS_DOWN"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reactions.THUMBS_DOWN").value(1))
                    .andExpect(jsonPath("$.reactions.THUMBS_UP").doesNotExist())
                    .andExpect(jsonPath("$.myReaction").value("THUMBS_DOWN"));

            mockMvc.perform(put("/api/chats/{voteId}/messages/{messageId}/reactions", fixture.voteId(), messageId)
                            .with(asUser(fixture.userB().getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"emoji": null}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reactions").isEmpty())
                    .andExpect(jsonPath("$.myReaction").doesNotExist());
        }

        @Test
        @DisplayName("본인 메시지에 반응하면 403을 반환한다")
        void 본인_메시지_반응_403() throws Exception {
            ChatRoomFixture fixture = createChatRoomFixture("본인 반응 테스트");
            Long messageId = sendMessageAndExtractId(fixture, fixture.userA().getId(), "내 메시지", null);

            mockMvc.perform(put("/api/chats/{voteId}/messages/{messageId}/reactions", fixture.voteId(), messageId)
                            .with(asUser(fixture.userA().getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"emoji": "THUMBS_UP"}
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("존재하지 않는 메시지에 반응하면 404를 반환한다")
        void 없는_메시지_반응_404() throws Exception {
            ChatRoomFixture fixture = createChatRoomFixture("없는 메시지 반응 테스트");

            mockMvc.perform(put("/api/chats/{voteId}/messages/{messageId}/reactions", fixture.voteId(), 999999)
                            .with(asUser(fixture.userB().getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"emoji": "THUMBS_UP"}
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("미참여자는 반응할 수 없다")
        void 미참여자_반응_403() throws Exception {
            ChatRoomFixture fixture = createChatRoomFixture("미참여 반응 테스트");

            Long messageId = sendMessageAndExtractId(fixture, fixture.userA().getId(), "대상", null);

            User outsider = createUserWithNickname("외부인");

            mockMvc.perform(put("/api/chats/{voteId}/messages/{messageId}/reactions", fixture.voteId(), messageId)
                            .with(asUser(outsider.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"emoji": "THUMBS_UP"}
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    private Long sendMessageAndExtractId(ChatRoomFixture fixture, Long userId, String content, Long replyToMessageId)
            throws Exception {
        String body = replyToMessageId == null
                ? """
                {"content": "%s"}
                """.formatted(content)
                : """
                {"content": "%s", "replyToMessageId": %d}
                """.formatted(content, replyToMessageId);

        MvcResult result = mockMvc.perform(post("/api/chats/{voteId}/messages", fixture.voteId())
                        .with(asUser(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("messageId").asLong()).isPositive();
        return json.get("messageId").asLong();
    }

    private void react(ChatRoomFixture fixture, Long userId, Long messageId, String emoji) throws Exception {
        mockMvc.perform(put("/api/chats/{voteId}/messages/{messageId}/reactions", fixture.voteId(), messageId)
                        .with(asUser(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"emoji": "%s"}
                                """.formatted(emoji)))
                .andExpect(status().isOk());
    }
}