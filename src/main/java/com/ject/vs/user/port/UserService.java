package com.ject.vs.user.port;

import com.ject.vs.auth.domain.Token;
import com.ject.vs.auth.domain.TokenRepository;
import com.ject.vs.common.exception.BusinessException;
import com.ject.vs.user.adapter.web.dto.*;
import com.ject.vs.user.domain.*;
import com.ject.vs.user.exception.UserErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Transactional
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final WordService wordService;
    private final TokenRepository tokenRepository;
    private final UserDeleteRepository userDeleteRepository;

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

   public UserProfileDefaultResponse initializeDefaultProfile(Long userId, UserProfileRequest req) {
       User user = userRepository.findById(userId)
               .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        UserNicknameRec nickname = suggestNickname(userId);

        user.initializeDefault(req.email(), req.birthYear(), req.gender(), nickname.nickname());

        return UserProfileDefaultResponse.from(nickname.nickname(), ImageColor.GREEN.name());
   }

   public UserMyPageResponse getMyPage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        return new UserMyPageResponse(user.getEmail(), user.getNickname(), user.getImageColor());
   }

   public UserMyPageResponse modifyInfo(Long userId, UserModifyInfoRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if(userRepository.isNicknameAvailable(req.nickname())) {
            User.modifyAccount(user, req.nickname(), req.imageColor());
        }

        return new UserMyPageResponse(user.getEmail(), user.getNickname(), user.getImageColor());
   }

   public Void deleteAccount(Long userId, UserDeleteReq req) {
       User user = userRepository.findById(userId)
               .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

       List<Token> list = tokenRepository.findByUserId(user);

       UserDelete delAccount = UserDelete.from(user.getEmail(), req);

       tokenRepository.deleteAll(list);
       userRepository.delete(user);
       userDeleteRepository.save(delAccount);

       return null;
   }
}
