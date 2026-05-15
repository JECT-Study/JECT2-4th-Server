package com.ject.vs.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByTokenValueAndTokenType(String tokenValue, TokenType tokenType);
}
