package com.ject.vs.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;

class AnonymousIdResolverTest {

    private AnonymousIdResolver resolver;

    @BeforeEach
    void setUp() {
        CookieProperties cookieProperties = new CookieProperties(false, "None");
        resolver = new AnonymousIdResolver(cookieProperties);
    }

    @Nested
    class resolveArgument {

        @Test
        void 쿠키가_있으면_기존_값을_그대로_반환한다() throws Exception {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setCookies(new Cookie("anonymous_id", "existing-uuid"));
            MockHttpServletResponse res = new MockHttpServletResponse();

            String result = (String) resolver.resolveArgument(
                    null, null, new ServletWebRequest(req, res), null);

            assertThat(result).isEqualTo("existing-uuid");
            assertThat(res.getHeader("Set-Cookie")).isNull();
        }

        @Test
        void 쿠키가_없으면_새_UUID를_발급하고_Set_Cookie_헤더를_추가한다() throws Exception {
            MockHttpServletRequest req = new MockHttpServletRequest();
            MockHttpServletResponse res = new MockHttpServletResponse();

            String result = (String) resolver.resolveArgument(
                    null, null, new ServletWebRequest(req, res), null);

            assertThat(result).isNotBlank();
            assertThat(result).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
            assertThat(res.getHeader("Set-Cookie")).contains("anonymous_id=" + result);
            assertThat(res.getHeader("Set-Cookie")).contains("HttpOnly");
            assertThat(res.getHeader("Set-Cookie")).contains("SameSite=None");
        }

        @Test
        void 쿠키_배열이_null인_경우_새_UUID를_발급한다() throws Exception {
            MockHttpServletRequest req = new MockHttpServletRequest();
            // no cookies set
            MockHttpServletResponse res = new MockHttpServletResponse();

            String result = (String) resolver.resolveArgument(
                    null, null, new ServletWebRequest(req, res), null);

            assertThat(result).isNotBlank();
        }
    }

    @Nested
    class supportsParameter {

        @Test
        void AnonymousId_어노테이션이_붙은_String_파라미터를_지원한다() throws NoSuchMethodException {
            var method = TestController.class.getMethod("testMethod", String.class);
            var param = new org.springframework.core.MethodParameter(method, 0);

            assertThat(resolver.supportsParameter(param)).isTrue();
        }

        @Test
        void AnonymousId_어노테이션이_없으면_지원하지_않는다() throws NoSuchMethodException {
            var method = TestController.class.getMethod("noAnnotationMethod", String.class);
            var param = new org.springframework.core.MethodParameter(method, 0);

            assertThat(resolver.supportsParameter(param)).isFalse();
        }

        static class TestController {
            public void testMethod(@AnonymousId String id) {}
            public void noAnnotationMethod(String id) {}
        }
    }
}
