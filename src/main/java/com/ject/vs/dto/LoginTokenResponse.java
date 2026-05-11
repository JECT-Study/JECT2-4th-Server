package com.ject.vs.dto;

import com.ject.vs.domain.UserStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginTokenResponse {
    private Long userId;
    private String accessToken;
    private String refreshToken;
    private UserStatus userStatus;
}
