package com.ject.vs.domain;

import com.ject.vs.dto.UserExtraInfo;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;
import java.time.Year;

@Entity
@Getter
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sub;
    // 아직 유저에 대한 정보 확정 아님
    private String nickname;

    private Year birthYear;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    private ImageColor imageColor;

    @Enumerated(EnumType.STRING)
    private UserStatus userStatus = UserStatus.UNREGISTER;

    public static User createWithSub(String sub) {
        User user = new User();
        user.sub = sub;
        return user;
    }

    public void updateInfo(UserExtraInfo userInfo) {
        this.birthYear = userInfo.birthDate();
        this.gender = userInfo.gender();
        this.nickname = userInfo.nickName();
        this.imageColor = userInfo.imageColor();
        this.userStatus = UserStatus.REGISTER;
    }
}
