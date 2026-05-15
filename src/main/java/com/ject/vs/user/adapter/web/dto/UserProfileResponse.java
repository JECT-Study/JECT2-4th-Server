package com.ject.vs.user.adapter.web.dto;

import com.ject.vs.user.domain.Gender;
import com.ject.vs.user.domain.ImageColor;
import com.ject.vs.user.domain.User;

import java.time.Year;

public record UserProfileResponse(Year birthDate, Gender gender, String nickname, ImageColor imageColor) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(user.getBirthYear(), user.getGender(), user.getNickname(), user.getImageColor());
    }
}
