package com.ject.vs.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "Token", indexes = @Index(name = "idx_token_value", columnList = "tokenValue"))
public class Token {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)

    private User user;

    @Column(length = 1024)
    private String tokenValue;

    @Enumerated(EnumType.STRING)
    private TokenType tokenType;

    private LocalDateTime expiresAt;

    private boolean revoked;

    @Builder
    public Token(User user, String tokenValue, TokenType tokenType, LocalDateTime expiresAt, boolean revoked) {
        this.user = user;
        this.tokenValue = tokenValue;
        this.tokenType = tokenType;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
    }

    public void revoke() {
        this.revoked = true;
    }
}
