package com.ject.vs.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class Token {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Lob
    private String tokenValue;

    @Enumerated(EnumType.STRING)
    private TokenType tokenType;

    private LocalDateTime expiresAt;

    private boolean revoked;

    @Builder
    public Token(String tokenId, User user, String tokenValue, TokenType tokenType, LocalDateTime expiresAt, boolean revoked) {
        this.tokenId = tokenId;
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
