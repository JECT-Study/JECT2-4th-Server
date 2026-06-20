package com.ject.vs.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 인앱 브라우저(WebView)에서 들어온 소셜 로그인 진입 요청({@code /oauth2/authorization/**})을
 * 외부 브라우저로 우회시킨다.
 *
 * <p>구글은 WebView에서의 OAuth 로그인을 차단(disallowed_useragent, 403)하므로,
 * 인앱 브라우저로 로그인 진입 시 구글로 리다이렉트하지 않고:
 * <ul>
 *     <li>안드로이드: {@code intent://} 스킴으로 크롬을 강제로 띄운다.</li>
 *     <li>iOS: WebView에서 사파리를 강제 실행할 방법이 없어, "외부 브라우저로 열기" 안내 페이지를 보여준다.</li>
 * </ul>
 * 외부 브라우저는 원래의 로그인 진입 URL을 그대로 다시 열어 OAuth 흐름을 새 세션에서 재시작한다.
 *
 * <p>Spring Security의 {@code OAuth2AuthorizationRequestRedirectFilter} 앞에 등록되어,
 * 구글로 리다이렉트가 일어나기 전에 동작한다.
 */
@Slf4j
public class InAppBrowserRedirectFilter extends OncePerRequestFilter {

    private static final String OAUTH_AUTHORIZATION_PREFIX = "/oauth2/authorization/";
    private static final String CHROME_PACKAGE = "com.android.chrome";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);

        if (!uri.startsWith(OAUTH_AUTHORIZATION_PREFIX) || !InAppBrowserDetector.isInAppBrowser(userAgent)) {
            filterChain.doFilter(request, response);
            return;
        }

        String targetUrl = buildExternalUrl(request);
        log.info("인앱 브라우저 로그인 우회: uri={}, android={}, ios={}",
                uri, InAppBrowserDetector.isAndroid(userAgent), InAppBrowserDetector.isIos(userAgent));

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");

        String html = InAppBrowserDetector.isAndroid(userAgent)
                ? androidIntentPage(targetUrl)
                : iosGuidePage(targetUrl);
        response.getWriter().write(html);
    }

    /** 포워딩 헤더(FRAMEWORK 전략)를 반영한 외부 접근 URL(scheme/host 포함)을 복원한다. */
    private String buildExternalUrl(HttpServletRequest request) {
        StringBuilder url = new StringBuilder(request.getRequestURL());
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isBlank()) {
            url.append('?').append(queryString);
        }
        return url.toString();
    }

    /** 안드로이드: intent:// 스킴으로 크롬을 강제 실행한다. 크롬 미설치 시 마켓 폴백. */
    private String androidIntentPage(String targetUrl) {
        String hostPathQuery = targetUrl.replaceFirst("^https?://", "");
        String intentUrl = "intent://" + hostPathQuery
                + "#Intent;scheme=https;package=" + CHROME_PACKAGE + ";"
                + "S.browser_fallback_url=" + targetUrl + ";end";
        String safeIntentUrl = escapeJs(intentUrl);

        return """
                <!doctype html>
                <html lang="ko">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>외부 브라우저로 이동</title>
                </head>
                <body style="font-family:-apple-system,'Apple SD Gothic Neo',sans-serif;text-align:center;padding:40px 24px;color:#222;">
                  <p style="font-size:16px;line-height:1.6;">크롬으로 이동 중입니다…<br>자동으로 열리지 않으면 아래 버튼을 눌러주세요.</p>
                  <a href="%s" style="display:inline-block;margin-top:16px;padding:14px 24px;background:#1a73e8;color:#fff;border-radius:10px;text-decoration:none;font-size:16px;">크롬으로 열기</a>
                  <script>location.href = "%s";</script>
                </body>
                </html>
                """.formatted(escapeHtmlAttr(intentUrl), safeIntentUrl);
    }

    /** iOS: 사파리를 강제 실행할 수 없으므로 안내 페이지 + 주소 복사 버튼을 제공한다. */
    private String iosGuidePage(String targetUrl) {
        return """
                <!doctype html>
                <html lang="ko">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>외부 브라우저에서 로그인</title>
                </head>
                <body style="font-family:-apple-system,'Apple SD Gothic Neo',sans-serif;padding:40px 24px;color:#222;max-width:480px;margin:0 auto;">
                  <h2 style="font-size:20px;">외부 브라우저에서 로그인해 주세요</h2>
                  <p style="font-size:15px;line-height:1.7;color:#444;">
                    보안 정책상 인앱 브라우저에서는 구글 로그인이 제한됩니다.<br>
                    아래 순서로 <b>Safari</b>에서 열어 로그인해 주세요.
                  </p>
                  <ol style="font-size:15px;line-height:1.9;color:#444;padding-left:20px;">
                    <li>화면 우측 상단 또는 하단의 <b>···</b> · 공유 버튼을 누르세요.</li>
                    <li><b>Safari로 열기</b>(기본 브라우저로 열기)를 선택하세요.</li>
                  </ol>
                  <div style="margin-top:20px;padding:14px;background:#f5f5f7;border-radius:10px;font-size:13px;word-break:break-all;color:#555;">%s</div>
                  <button onclick="copyUrl()" style="display:block;width:100%%;margin-top:16px;padding:14px;background:#1a73e8;color:#fff;border:0;border-radius:10px;font-size:16px;">주소 복사하기</button>
                  <script>
                    var url = "%s";
                    function copyUrl() {
                      if (navigator.clipboard) {
                        navigator.clipboard.writeText(url).then(showCopied, fallbackCopy);
                      } else {
                        fallbackCopy();
                      }
                    }
                    function fallbackCopy() {
                      var ta = document.createElement('textarea');
                      ta.value = url; document.body.appendChild(ta); ta.select();
                      try { document.execCommand('copy'); showCopied(); } catch (e) {}
                      document.body.removeChild(ta);
                    }
                    function showCopied() { alert('주소가 복사되었습니다. Safari 주소창에 붙여넣어 주세요.'); }
                  </script>
                </body>
                </html>
                """.formatted(escapeHtml(targetUrl), escapeJs(targetUrl));
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String escapeHtmlAttr(String value) {
        return escapeHtml(value).replace("\"", "&quot;");
    }

    private String escapeJs(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
