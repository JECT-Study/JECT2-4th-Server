package com.ject.vs.user.domain;

import com.ject.vs.user.adapter.web.dto.UserExtraInfo;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.time.Year;

@Entity
@Getter
@Table(name = "users")
public class User {
    /** 탈퇴 후 익명 처리된 사용자의 표시용 닉네임 */
    public static final String WITHDRAWN_NICKNAME = "알 수 없음";

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

    private Instant withdrawnAt;

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

    /**
     * 회원 탈퇴(soft delete). 사용자 행은 보존하되 식별 정보를 익명화한다.
     * 닉네임은 "알 수 없음"으로 바꿔 투표/채팅 등 잔존 데이터에 익명으로 노출되도록 한다.
     * 이메일은 재가입 시 기존 탈퇴 계정을 복구하지 않고 새 계정을 만들 수 있도록 유지한다.
     */
    public void withdraw(Instant withdrawnAt) {
        this.nickname = WITHDRAWN_NICKNAME;
        this.birthYear = null;
        this.gender = null;
        this.imageColor = null;
        this.userStatus = UserStatus.WITHDRAWN;
        this.withdrawnAt = withdrawnAt;
    }

    public boolean isWithdrawn() {
        return this.userStatus == UserStatus.WITHDRAWN;
    }

}
