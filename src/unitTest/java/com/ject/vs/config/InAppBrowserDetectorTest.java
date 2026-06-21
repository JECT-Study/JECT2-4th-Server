package com.ject.vs.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 인앱 브라우저(WebView) UA 판별 규칙 검증.
 */
class InAppBrowserDetectorTest {

    // 실제 인앱 브라우저 UA 샘플
    private static final String THREADS_IOS =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 "
                    + "(KHTML, like Gecko) Mobile/15E148 Instagram 320.0.0.0 (iPhone; iOS 17_0; en_US; Barcelona)";
    private static final String INSTAGRAM_ANDROID =
            "Mozilla/5.0 (Linux; Android 13; SM-S908N; wv) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Version/4.0 Chrome/120.0.0.0 Mobile Safari/537.36 Instagram 320.0.0.0 Android";
    private static final String KAKAOTALK_ANDROID =
            "Mozilla/5.0 (Linux; Android 12; wv) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Version/4.0 Chrome/120.0.0.0 Mobile Safari/537.36 KAKAOTALK 10.0.0";

    // 정상 브라우저 UA 샘플
    private static final String CHROME_ANDROID =
            "Mozilla/5.0 (Linux; Android 13; SM-S908N) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/120.0.0.0 Mobile Safari/537.36";
    private static final String SAFARI_IOS =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 "
                    + "(KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";

    @Test
    void 인앱_브라우저_UA는_감지된다() {
        assertThat(InAppBrowserDetector.isInAppBrowser(THREADS_IOS)).isTrue();
        assertThat(InAppBrowserDetector.isInAppBrowser(INSTAGRAM_ANDROID)).isTrue();
        assertThat(InAppBrowserDetector.isInAppBrowser(KAKAOTALK_ANDROID)).isTrue();
    }

    @Test
    void 일반_크롬과_사파리는_인앱으로_보지_않는다() {
        assertThat(InAppBrowserDetector.isInAppBrowser(CHROME_ANDROID)).isFalse();
        assertThat(InAppBrowserDetector.isInAppBrowser(SAFARI_IOS)).isFalse();
    }

    @Test
    void UA가_없으면_인앱이_아니다() {
        assertThat(InAppBrowserDetector.isInAppBrowser(null)).isFalse();
        assertThat(InAppBrowserDetector.isInAppBrowser("")).isFalse();
    }

    @Test
    void 안드로이드와_iOS를_구분한다() {
        assertThat(InAppBrowserDetector.isAndroid(INSTAGRAM_ANDROID)).isTrue();
        assertThat(InAppBrowserDetector.isIos(INSTAGRAM_ANDROID)).isFalse();

        assertThat(InAppBrowserDetector.isIos(THREADS_IOS)).isTrue();
        assertThat(InAppBrowserDetector.isAndroid(THREADS_IOS)).isFalse();
    }
}
