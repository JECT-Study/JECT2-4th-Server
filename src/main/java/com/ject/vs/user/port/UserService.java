package com.ject.vs.user.port;

import com.ject.vs.common.exception.BusinessException;
import com.ject.vs.user.adapter.web.dto.NicknameCheckResponse;
import com.ject.vs.user.adapter.web.dto.UserExtraInfo;
import com.ject.vs.user.adapter.web.dto.UserNicknameRec;
import com.ject.vs.user.adapter.web.dto.UserProfileDefaultResponse;
import com.ject.vs.user.adapter.web.dto.UserProfileRequest;
import com.ject.vs.user.adapter.web.dto.UserProfileResponse;
import com.ject.vs.user.domain.ImageColor;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.domain.UserRepository;
import com.ject.vs.user.exception.UserErrorCode;
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
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }

    public NicknameCheckResponse checkNickname(String nickName, Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        return new NicknameCheckResponse(userRepository.isNicknameAvailable(nickName));
    }

    public UserProfileResponse setupAdditionalInfo(UserExtraInfo userInfo, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        user.updateInfo(userInfo);
        return UserProfileResponse.from(user);
    }

    public UserNicknameRec suggestNickname(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        return new UserNicknameRec(wordService.generateNickname());
   }

   public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        return UserProfileResponse.from(user);
   }

   public UserProfileDefaultResponse initializeDefaultProfile(Long userId, UserProfileRequest request) {
       User user = userRepository.findById(userId)
               .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        UserNicknameRec nickname = suggestNickname(userId);

        user.initializeDefault(request, nickname.nickname());

        return UserProfileDefaultResponse.from(nickname.nickname(), ImageColor.GREEN.name());
   }
}
