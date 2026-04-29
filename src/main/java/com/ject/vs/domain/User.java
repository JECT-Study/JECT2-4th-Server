package com.ject.vs.domain;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sub;
    // 아직 유저에 대한 정보 확정 아님

    public static User createWithSub(String sub) {
        User user = new User();
        user.sub = sub;
        return user;
    }
}
