package com.ject.vs.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByTokenValueAndTokenType(String tokenValue, TokenType tokenType);

    List<Token> findAllByTokenFamily(String tokenFamily);
}
