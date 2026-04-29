package com.ject.vs.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginTokenResponse {
    private Long userId;
    private String accessToken;
    private String refreshToken;
}

