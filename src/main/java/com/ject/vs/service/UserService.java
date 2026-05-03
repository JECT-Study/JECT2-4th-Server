package com.ject.vs.service;

import com.ject.vs.domain.User;
import com.ject.vs.dto.UserExtraInfo;
import com.ject.vs.dto.UserProfileResponse;
import com.ject.vs.repository.TokenRepository;
import com.ject.vs.repository.UserRepository;
import com.ject.vs.util.JwtProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Transactional
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    public User findOrCreate(String sub) {
        return userRepository.findBySub(sub)
                .orElseGet(() -> userRepository.save(User.createWithSub(sub)));
    }

    public User getBySub(String sub) {      // 추후에 예외처리 통합
        return userRepository.findBySub(sub)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }

    // 닉네임 중복체크
    public boolean checkNickname(String nickName) {
        return userRepository.existsUser(nickName);
    }

    // 추가정보 기입
    public UserProfileResponse setupAdditionalInfo(UserExtraInfo userInfo, String accessToken) {
        User user = jwtProvider.getUser(accessToken);
        // 토큰 예외처리 해야함

        user.updateInfo(userInfo);

        return UserProfileResponse.from(user);
    }
}
