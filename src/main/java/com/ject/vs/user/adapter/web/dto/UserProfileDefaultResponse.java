package com.ject.vs.user.adapter.web.dto;

import lombok.Builder;

@Builder
public record UserProfileDefaultResponse(String nickname, String imageColor) {
    public static UserProfileDefaultResponse from(String nickname, String imageColor) {
        return UserProfileDefaultResponse.builder()
                .nickname(nickname)
                .imageColor(imageColor)
                .build();
    }
}
