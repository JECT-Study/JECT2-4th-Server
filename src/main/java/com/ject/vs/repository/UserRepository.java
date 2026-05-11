package com.ject.vs.repository;

import com.ject.vs.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("select (count(u.id) = 0) from User u where u.nickname = :nickName")
    boolean isNicknameAvailable(@Param("nickName") String nickName);

    Optional<User> findByEmail(String email);
}
