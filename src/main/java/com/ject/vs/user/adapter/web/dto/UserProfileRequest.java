package com.ject.vs.user.adapter.web.dto;

import com.ject.vs.user.domain.Gender;
import lombok.Data;

import java.time.Year;

@Data
public class UserProfileRequest {
    private String email;
    private Year birthYear;
    private Gender gender;
}
