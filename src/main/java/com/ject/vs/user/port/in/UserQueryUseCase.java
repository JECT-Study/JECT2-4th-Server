package com.ject.vs.user.port.in;

import com.ject.vs.user.domain.User;
import org.springframework.lang.NonNull;

import java.util.Optional;

public interface UserQueryUseCase {
    @NonNull
    User getUser(Long userId);
}
