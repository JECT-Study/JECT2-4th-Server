package com.ject.vs.repository;

import com.ject.vs.domain.Token;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByTokenId(String tokenId);
    List<Token> findAllByUserId(Long userId);
    void deleteAllByExpiresAtBefore(LocalDateTime now);
}
