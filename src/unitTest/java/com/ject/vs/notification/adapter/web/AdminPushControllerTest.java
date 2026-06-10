package com.ject.vs.notification.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ject.vs.analytics.AnalyticsEventLogger;
import com.ject.vs.auth.port.CustomOAuth2UserService;
import com.ject.vs.config.OAuth2LoginSuccessHandler;
import com.ject.vs.common.exception.GlobalExceptionHandler;
import com.ject.vs.config.TestPropertiesConfig;
import com.ject.vs.notification.adapter.web.dto.AdminPushRequest;
import com.ject.vs.notification.port.AdminPushService;
import com.ject.vs.notification.port.out.PushSenderPort;
import com.ject.vs.util.CookieUtil;
import com.ject.vs.util.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminPushController.class)
@Import({TestPropertiesConfig.class, GlobalExceptionHandler.class})
class AdminPushControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminPushService adminPushService;

    @MockBean
    private CookieUtil cookieUtil;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockBean
    private PushSenderPort pushSenderPort;

    @MockBean
    private AnalyticsEventLogger analytics;

    private static final UsernamePasswordAuthenticationToken ADMIN_AUTH =
            new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList());

    @Nested
    @DisplayName("POST /api/admin/push/test")
    class SendTestPush {

        @Test
        @DisplayName("테스트 푸시를 발송한다")
        void sends_test_push() throws Exception {
            AdminPushRequest request = new AdminPushRequest(
                    6L, "테스트 알림", "테스트 본문입니다.", 100L, "https://example.com/thumb.jpg");

            given(adminPushService.sendTestPush(
                    eq(1L), eq(6L), eq("테스트 알림"), eq("테스트 본문입니다."), eq(100L), eq("https://example.com/thumb.jpg")
            )).willReturn(42L);

            mockMvc.perform(post("/api/admin/push/test")
                            .with(authentication(ADMIN_AUTH))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.notificationId").value(42))
                    .andExpect(jsonPath("$.message").value("푸시 알림이 발송되었습니다."));
        }

        @Test
        @DisplayName("voteId와 thumbnailUrl은 선택 항목이다")
        void allows_optional_fields_null() throws Exception {
            AdminPushRequest request = new AdminPushRequest(
                    6L, "테스트 알림", "테스트 본문입니다.", null, null);

            given(adminPushService.sendTestPush(
                    eq(1L), eq(6L), anyString(), anyString(), isNull(), isNull()
            )).willReturn(1L);

            mockMvc.perform(post("/api/admin/push/test")
                            .with(authentication(ADMIN_AUTH))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("targetUserId가 없으면 400을 반환한다")
        void returns_400_when_targetUserId_missing() throws Exception {
            String invalidJson = """
                    {
                        "title": "테스트",
                        "body": "테스트"
                    }
                    """;

            mockMvc.perform(post("/api/admin/push/test")
                            .with(authentication(ADMIN_AUTH))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("title이 비어있으면 400을 반환한다")
        void returns_400_when_title_blank() throws Exception {
            AdminPushRequest request = new AdminPushRequest(
                    6L, "", "테스트 본문", null, null);

            mockMvc.perform(post("/api/admin/push/test")
                            .with(authentication(ADMIN_AUTH))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("인증되지 않은 사용자는 401을 받는다")
        void returns_401_when_not_authenticated() throws Exception {
            AdminPushRequest request = new AdminPushRequest(
                    6L, "테스트", "테스트", null, null);

            mockMvc.perform(post("/api/admin/push/test")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("관리자 권한이 없으면 서비스에서 예외가 발생한다")
        void throws_exception_when_not_admin() throws Exception {
            AdminPushRequest request = new AdminPushRequest(
                    6L, "테스트", "테스트", null, null);

            given(adminPushService.sendTestPush(
                    anyLong(), anyLong(), anyString(), anyString(), any(), any()
            )).willThrow(new IllegalArgumentException("관리자 권한이 없습니다."));

            mockMvc.perform(post("/api/admin/push/test")
                            .with(authentication(ADMIN_AUTH))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
