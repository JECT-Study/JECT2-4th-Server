package com.ject.vs.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("select (count(u.id) = 0) from User u where u.nickname = :nickName")
    boolean isNicknameAvailable(@Param("nickName") String nickName);

    Optional<User> findByEmail(String email);

    // 활성 사용자(탈퇴 제외) 조회. 동일 이메일에 탈퇴 row가 공존할 수 있으므로 상태로 필터링한다.
    Optional<User> findByEmailAndUserStatusNot(String email, UserStatus userStatus);

    // 특정 상태(예: WITHDRAWN)의 동일 이메일 row 목록. 재가입 제한 판정에 사용한다.
    List<User> findByEmailAndUserStatus(String email, UserStatus userStatus);
}
