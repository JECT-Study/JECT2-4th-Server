package com.ject.vs.auth.port.in.dto;

import com.ject.vs.user.domain.UserStatus;
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
