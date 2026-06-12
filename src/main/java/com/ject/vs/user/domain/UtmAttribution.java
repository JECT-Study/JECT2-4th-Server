package com.ject.vs.user.domain;

/**
 * 가입 유입 출처(UTM)를 담는 불변 값 객체.
 *
 * <p>비회원이 UTM 링크로 랜딩할 때 프론트가 {@code /api/track/visit} 로 넘긴 값이
 * 쿠키에 박제됐다가, 소셜 로그인 가입이 완료되는 순간 {@link User} 에 기록된다.
 *
 * <p>빈 문자열은 {@link #of} 에서 null로 정규화해 의미 없는 값이 저장되지 않도록 한다.
 */
public record UtmAttribution(String source, String medium, String campaign, String content) {

    private static final UtmAttribution EMPTY = new UtmAttribution(null, null, null, null);

    public static UtmAttribution empty() {
        return EMPTY;
    }

    public static UtmAttribution of(String source, String medium, String campaign, String content) {
        return new UtmAttribution(blankToNull(source), blankToNull(medium), blankToNull(campaign), blankToNull(content));
    }

    public boolean isEmpty() {
        return source == null && medium == null && campaign == null && content == null;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
