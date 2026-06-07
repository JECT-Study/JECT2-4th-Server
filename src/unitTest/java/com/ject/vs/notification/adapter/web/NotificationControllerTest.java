package com.ject.vs.notification.adapter.web;

import com.ject.vs.analytics.AnalyticsEventLogger;
import com.ject.vs.auth.port.CustomOAuth2UserService;
import com.ject.vs.config.OAuth2LoginSuccessHandler;
import com.ject.vs.common.exception.GlobalExceptionHandler;
import com.ject.vs.config.TestPropertiesConfig;
import com.ject.vs.notification.domain.NotificationType;
import com.ject.vs.notification.port.in.NotificationCommandUseCase;
import com.ject.vs.notification.port.in.NotificationQueryUseCase;
import com.ject.vs.notification.port.in.NotificationQueryUseCase.NotificationPageResult;
import com.ject.vs.notification.port.in.NotificationQueryUseCase.NotificationView;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import({TestPropertiesConfig.class, GlobalExceptionHandler.class})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationQueryUseCase queryUseCase;

    @MockBean
    private NotificationCommandUseCase commandUseCase;

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

    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList());

    @Nested
    @DisplayName("GET /api/notifications")
    class GetList {

        @Test
        @DisplayName("알림 목록을 조회한다")
        void returns_notification_list() throws Exception {
            NotificationView view = new NotificationView(
                    1L, NotificationType.VOTE_ENDED, 100L,
                    "투표 결과가 공개됐어요", "[테스트 투표] 결과 보러가기",
                    "https://example.com/thumb.jpg", false, Instant.now());
            NotificationPageResult result = new NotificationPageResult(List.of(view), null, false);

            given(queryUseCase.getList(eq(1L), isNull(), eq(20))).willReturn(result);

            mockMvc.perform(get("/api/notifications")
                            .with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notifications").isArray())
                    .andExpect(jsonPath("$.notifications[0].notificationId").value(1))
                    .andExpect(jsonPath("$.notifications[0].type").value("VOTE_ENDED"))
                    .andExpect(jsonPath("$.hasNext").value(false));
        }

        @Test
        @DisplayName("커서와 size를 지정할 수 있다")
        void accepts_cursor_and_size() throws Exception {
            NotificationPageResult result = new NotificationPageResult(List.of(), null, false);

            given(queryUseCase.getList(eq(1L), eq(50L), eq(10))).willReturn(result);

            mockMvc.perform(get("/api/notifications")
                            .param("cursor", "50")
                            .param("size", "10")
                            .with(authentication(AUTH)))
                    .andExpect(status().isOk());

            verify(queryUseCase).getList(1L, 50L, 10);
        }

        @Test
        @DisplayName("인증되지 않은 사용자는 401을 받는다")
        void returns_401_when_not_authenticated() throws Exception {
            mockMvc.perform(get("/api/notifications"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/notifications/unread-count")
    class GetUnreadCount {

        @Test
        @DisplayName("읽지 않은 알림 수를 조회한다")
        void returns_unread_count() throws Exception {
            given(queryUseCase.getUnreadCount(1L)).willReturn(5L);

            mockMvc.perform(get("/api/notifications/unread-count")
                            .with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.unreadCount").value(5));
        }
    }

    @Nested
    @DisplayName("POST /api/notifications/{notificationId}/read")
    class MarkAsRead {

        @Test
        @DisplayName("알림을 읽음 처리한다")
        void marks_notification_as_read() throws Exception {
            given(commandUseCase.markAsRead(1L, 1L)).willReturn(
                    new NotificationCommandUseCase.MarkAsReadResult(NotificationType.VOTE_ENDED, 100L, false));

            mockMvc.perform(post("/api/notifications/1/read")
                            .with(authentication(AUTH))
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(commandUseCase).markAsRead(1L, 1L);
        }
    }

    @Nested
    @DisplayName("POST /api/notifications/read-all")
    class MarkAllAsRead {

        @Test
        @DisplayName("모든 알림을 읽음 처리하고 개수를 반환한다")
        void marks_all_as_read_and_returns_count() throws Exception {
            given(commandUseCase.markAllAsRead(1L)).willReturn(10);

            mockMvc.perform(post("/api/notifications/read-all")
                            .with(authentication(AUTH))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.updatedCount").value(10));

            verify(commandUseCase).markAllAsRead(1L);
        }
    }
}
