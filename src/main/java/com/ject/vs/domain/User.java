package com.ject.vs.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;

@Entity
@Getter
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sub;

    private String gender;

    private LocalDate birthDate;

    public static User createWithSub(String sub) {
        User user = new User();
        user.sub = sub;
        return user;
    }
}
