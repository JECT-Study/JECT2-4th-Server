package com.ject.vs.user.domain;

import com.ject.vs.user.adapter.web.dto.UserDeleteReq;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class UserDelete {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String category;

    private String message;

    public UserDelete(String email, UserDeleteReq req) {
        this.email = email;
        this.category = req.category();
        this.message = req.reasone();
    }

    public static UserDelete from(String email, UserDeleteReq req) {
        return new UserDelete(email, req);
    }
}
