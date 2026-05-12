package com.ject.vs.dto;

import com.ject.vs.domain.Gender;
import com.ject.vs.domain.ImageColor;
import com.ject.vs.domain.User;

import java.time.Year;

public record UserProfileResponse(Year birthDate, Gender gender, String nickname, ImageColor imageColor) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(user.getBirthYear(), user.getGender(), user.getNickname(), user.getImageColor());
    }
}

