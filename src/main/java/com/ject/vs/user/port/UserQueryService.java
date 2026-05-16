package com.ject.vs.user.port;

import com.ject.vs.user.domain.User;
import com.ject.vs.user.domain.UserRepository;
import com.ject.vs.user.port.in.UserQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService implements UserQueryUseCase {

    private final UserRepository userRepository;

    @Override
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }
}
