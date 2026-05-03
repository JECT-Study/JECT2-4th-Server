package com.ject.vs.dto;

import com.ject.vs.domain.Gender;
import com.ject.vs.domain.ImageColor;

import java.time.Year;

public record UserExtraInfo (
    Year birthDate,
    Gender gender,
    String nickName,
    ImageColor imageColor
) {}
