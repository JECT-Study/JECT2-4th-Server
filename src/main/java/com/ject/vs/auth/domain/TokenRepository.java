package com.ject.vs.auth.domain;

import com.ject.vs.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByTokenValueAndTokenType(String tokenValue, TokenType tokenType);

    @Query("Select t from Token t where t.user = :user")
    List<Token> findByUserId(@Param("user") User user);
}
