package com.ject.vs.dto;

import com.ject.vs.domain.Gender;
import com.ject.vs.domain.ImageColor;
import com.ject.vs.domain.User;
import com.ject.vs.domain.UserStatus;
import lombok.Builder;

import java.time.Year;

@Builder
public record UserDetailResponse(String email, String nickname, Year birthYear, Gender gender, ImageColor imageColor, UserStatus userStatus) {
    public static UserDetailResponse from(User user) {
        return UserDetailResponse.builder()
                .email(user.getEmail())
                .nickname(user.getNickname())
                .birthYear(user.getBirthYear())
                .gender(user.getGender())
                .imageColor(user.getImageColor())
                .userStatus(user.getUserStatus())
                .build();
    }
}
