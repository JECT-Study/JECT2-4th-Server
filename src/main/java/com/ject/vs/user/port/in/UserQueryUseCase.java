package com.ject.vs.user.port.in;

import com.ject.vs.user.domain.User;
import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.List;

public interface UserQueryUseCase {
    @NonNull
    User getUser(Long userId);

    List<User> findAllById(Collection<Long> userIds);
}
