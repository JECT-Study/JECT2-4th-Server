package com.ject.vs.config;

import java.util.regex.Pattern;

/**
 * User-Agent로 인앱 브라우저(WebView) 여부와 모바일 OS를 판별한다.
 *
 * <p>구글은 보안 정책상 WebView(인앱 브라우저)에서의 OAuth 로그인을 차단(disallowed_useragent, 403)하므로,
 * 인앱 브라우저로 진입한 로그인 요청은 외부 브라우저로 우회시켜야 한다.
 * 자세한 우회 처리는 {@link InAppBrowserRedirectFilter} 참고.
 */
public final class InAppBrowserDetector {

    private InAppBrowserDetector() {
    }

    /**
     * 대표적인 인앱 브라우저 UA 시그니처.
     * <ul>
     *     <li>{@code ; wv)} : 안드로이드 WebView 공통 마커(거의 모든 안드로이드 인앱 브라우저가 포함)</li>
     *     <li>iOS는 WebView 공통 마커가 없어 앱별 시그니처로 판별(Instagram/Threads/카카오톡/라인/네이버 등)</li>
     * </ul>
     */
    private static final Pattern IN_APP_PATTERN = Pattern.compile(
            "; wv\\)"                                   // Android WebView 공통
                    + "|FBAN|FBAV|FB_IAB"               // Facebook/Messenger
                    + "|Instagram|Barcelona|Threads"    // Instagram, Threads(=Barcelona)
                    + "|KAKAOTALK"                       // 카카오톡
                    + "|Line/|NAVER|DaumApps|BAND"      // 라인, 네이버, 다음, 밴드
                    + "|everytimeApp|Snapchat|Twitter|Pinterest",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ANDROID_PATTERN = Pattern.compile("Android", Pattern.CASE_INSENSITIVE);
    private static final Pattern IOS_PATTERN = Pattern.compile("iPhone|iPad|iPod", Pattern.CASE_INSENSITIVE);

    public static boolean isInAppBrowser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return false;
        }
        return IN_APP_PATTERN.matcher(userAgent).find();
    }

    public static boolean isAndroid(String userAgent) {
        return userAgent != null && ANDROID_PATTERN.matcher(userAgent).find();
    }

    public static boolean isIos(String userAgent) {
        return userAgent != null && IOS_PATTERN.matcher(userAgent).find();
    }
}
