package com.ject.vs.user.adapter.web.dto;

import com.ject.vs.user.domain.Gender;
import lombok.Data;

import java.time.Year;

public record UserProfileRequest(String email, Year birthYear, Gender gender) {
}
