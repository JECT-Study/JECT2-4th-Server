package com.ject.vs.user.port;

import com.ject.vs.common.exception.BusinessException;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.domain.UserRepository;
import com.ject.vs.user.exception.UserErrorCode;
import com.ject.vs.user.port.in.UserQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService implements UserQueryUseCase {

    private final UserRepository userRepository;

    @Override
    public @NonNull User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(UserErrorCode.USER_NOT_FOUND)
        );
    }

    @Override
    public List<User> findAllById(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllById(userIds);
    }
}
