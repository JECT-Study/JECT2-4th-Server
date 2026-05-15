package com.ject.vs.user.adapter.web.dto;

import com.ject.vs.user.domain.Gender;
import com.ject.vs.user.domain.ImageColor;

import java.time.Year;

public record UserExtraInfo (
    Year birthDate,
    Gender gender,
    String nickName,
    ImageColor imageColor
) {}
