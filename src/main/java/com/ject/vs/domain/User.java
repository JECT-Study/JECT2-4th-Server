package com.ject.vs.domain;

import com.ject.vs.dto.UserExtraInfo;
import com.ject.vs.dto.UserProfileRequest;
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

    public void initializeDefault(UserProfileRequest userInfo, String nickname)  {
        this.email = userInfo.getEmail();
        this.birthYear = userInfo.getBirthYear();
        this.gender = userInfo.getGender();
        this.imageColor = ImageColor.GREEN;
        this.nickname = nickname;
    }
}
