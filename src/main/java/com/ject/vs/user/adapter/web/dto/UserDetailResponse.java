package com.ject.vs.user.adapter.web.dto;

import com.ject.vs.user.domain.Gender;
import com.ject.vs.user.domain.ImageColor;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.domain.UserStatus;
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
