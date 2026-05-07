package com.ject.vs.service;

import com.ject.vs.domain.User;
import com.ject.vs.dto.NicknameCheckResponse;
import com.ject.vs.dto.UserExtraInfo;
import com.ject.vs.dto.UserNicknameRec;
import com.ject.vs.dto.UserProfileResponse;
import com.ject.vs.exception.CustomException;
import com.ject.vs.exception.ErrorCode;
import com.ject.vs.repository.TokenRepository;
import com.ject.vs.repository.UserRepository;
import com.ject.vs.util.JwtProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Transactional
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final WordService wordService;
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
    public NicknameCheckResponse checkNickname(String nickName, String accessToken) {
        User user = jwtProvider.getUser(accessToken);

        if(user == null) throw new CustomException(ErrorCode.USER_NOT_FOUND);

        return new NicknameCheckResponse(userRepository.existsUser(nickName));
    }

    // 추가정보 기입
    public UserProfileResponse setupAdditionalInfo(UserExtraInfo userInfo, String accessToken) {
        User user = jwtProvider.getUser(accessToken);

        if(user == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        user.updateInfo(userInfo);
        return UserProfileResponse.from(user);
    }

    // 닉네임 추천
    public UserNicknameRec suggestNickname(String accessToken) {
        User user = jwtProvider.getUser(accessToken);

        if(user == null) throw new CustomException(ErrorCode.USER_NOT_FOUND);

        return new UserNicknameRec(wordService.generateNickname());
   }

   public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return UserProfileResponse.from(user);
   }
}
