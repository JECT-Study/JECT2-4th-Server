package com.ject.vs.user.domain;

import com.ject.vs.user.adapter.web.dto.UserExtraInfo;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Year;

@Entity
@Getter
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sub;

    private String email;
    private String nickname;

    private Year birthYear;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    private ImageColor imageColor;

    @Enumerated(EnumType.STRING)
    private UserStatus userStatus = UserStatus.UNREGISTER;

    public static User createWithEmail(String email) {
        User user = new User();
        user.email = email;
        return user;
    }

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

    public void initializeDefault(String email, Year birthYear, Gender gender, String nickname)  {
        this.email = email;
        this.birthYear = birthYear;
        this.gender = gender;
        this.imageColor = ImageColor.GREEN;
        this.nickname = nickname;
    }

    public static void modifyAccount(User user, String nickname, ImageColor imageColor) {
        user.nickname = nickname;
        user.imageColor = imageColor;
    }
}
