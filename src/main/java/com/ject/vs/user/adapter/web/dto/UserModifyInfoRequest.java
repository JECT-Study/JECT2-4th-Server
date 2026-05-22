package com.ject.vs.user.adapter.web.dto;

import com.ject.vs.user.domain.ImageColor;

public record UserModifyInfoRequest(String nickname, ImageColor imageColor) {
}
