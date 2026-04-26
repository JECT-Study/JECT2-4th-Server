package com.ject.vs.service;

import com.ject.vs.domain.User;
import com.ject.vs.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Transactional
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User findOrCreate(String sub) {
        return userRepository.findBySub(sub)
                .orElseGet(() -> userRepository.save(User.createWithSub(sub)));
    }

    public User getBySub(String sub) {      // 추후에 예외처리 통합
        return userRepository.findBySub(sub)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }
}
