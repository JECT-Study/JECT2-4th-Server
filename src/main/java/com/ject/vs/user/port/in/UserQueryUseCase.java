package com.ject.vs.user.port.in;

import com.ject.vs.user.domain.User;

import java.util.Optional;

public interface UserQueryUseCase {

    Optional<User> findById(Long userId);
}
