package com.ject.vs.auth.port.in.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
public class OAuthAttributes {
    private final Map<String, Object> attributes;
    private final String nameAttributeKey;
    private final String registrationId;
    private final String sub;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, String registrationId, String sub) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.registrationId = registrationId;
        this.sub = sub;
    }

    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if("google".equals(registrationId)) {
            return ofGoogle(registrationId, userNameAttributeName, attributes);
        }

        throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다.");
    }

    public static OAuthAttributes ofGoogle(String registrationId, String nameAttributeKey, Map<String, Object> attributes) {
        Object sub = attributes.get("sub");

        if(sub == null) {
            throw new IllegalArgumentException("응답에 sub가 없습니다.");
        }

        return OAuthAttributes.builder()
                .attributes(attributes)
                .nameAttributeKey(nameAttributeKey)
                .registrationId(registrationId)
                .sub(sub.toString())
                .build();
    }
}
