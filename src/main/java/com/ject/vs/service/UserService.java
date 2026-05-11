package com.ject.vs.service;

import com.ject.vs.domain.ImageColor;
import com.ject.vs.domain.User;
import com.ject.vs.dto.*;
import com.ject.vs.exception.CustomException;
import com.ject.vs.exception.UserErrorCode;
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
    private final WordService wordService;

    public User findOrCreate(String email) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(User.createWithEmail(email)));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    // 닉네임 중복체크
    public NicknameCheckResponse checkNickname(String nickName, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        return new NicknameCheckResponse(userRepository.isNicknameAvailable(nickName));
    }

    // 추가정보 기입
    public UserProfileResponse setupAdditionalInfo(UserExtraInfo userInfo, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        user.updateInfo(userInfo);
        return UserProfileResponse.from(user);
    }

    // 닉네임 추천
    public UserNicknameRec suggestNickname(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        return new UserNicknameRec(wordService.generateNickname());
   }

   public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        return UserProfileResponse.from(user);
   }

   // 추천 닉네임 생성 + 기본 이미지 선택
   public UserProfileDefaultResponse initializeDefaultProfile(Long userId, UserProfileRequest request) {
       User user = userRepository.findById(userId)
               .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        UserNicknameRec nickname = suggestNickname(userId);

        user.initializeDefault(request, nickname.nickname());

        return UserProfileDefaultResponse.from(nickname.nickname(), ImageColor.GREEN.name());
   }
}
