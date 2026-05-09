package com.ject.vs.dto;

import com.ject.vs.domain.Gender;
import lombok.Data;

import java.time.Year;

@Data
public class UserProfileRequest {
    private String email;
    private Year birthYear;
    private Gender gender;
}
