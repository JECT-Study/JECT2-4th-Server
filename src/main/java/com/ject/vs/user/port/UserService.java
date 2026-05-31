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

import java.time.Instant;
import java.util.List;

@Transactional
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final WordService wordService;
    private final TokenRepository tokenRepository;
    private final UserDeleteRepository userDeleteRepository;
    private final UserImageService userImageService;

    public User findOrCreate(String email) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return userRepository.save(User.createWithEmail(email));
        }

        if (user.isWithdrawn()) {
            if (user.isReregisterRestricted(Instant.now())) {
                throw new BusinessException(UserErrorCode.REREGISTRATION_RESTRICTED);
            }
            // 제한 기간이 지난 탈퇴 계정은 미등록 상태로 되돌려 재온보딩을 진행한다.
            user.reactivate();
        }

        return user;
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

        if(user.getNickname() == null) throw new BusinessException(UserErrorCode.USER_NOT_REGISTER);

        return UserProfileResponse.from(user);
   }

   public UserProfileDefaultResponse initializeDefaultProfile(Long userId, UserProfileRequest req) {
       User user = userRepository.findById(userId)
               .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        UserNicknameRec nickname = suggestNickname(userId);

        user.initializeDefault(req.email(), req.birthYear(), req.gender(), nickname.nickname());

        return UserProfileDefaultResponse.from(nickname.nickname(), ImageColor.GREEN.name());
   }

   public UserMyPageResponse modifyInfo(Long userId, UserModifyInfoRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        User.modifyAccount(user, req.nickname(), req.imageColor());

        return new UserMyPageResponse(user.getEmail(), user.getNickname(), user.getImageColor());
   }

   public Void deleteAccount(Long userId, UserDeleteReq req) {
       User user = userRepository.findById(userId)
               .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

       List<Token> list = tokenRepository.findByUserId(user);

       UserDelete delAccount = UserDelete.from(user.getEmail(), req);

       // soft delete: 토큰만 제거(로그아웃)하고 사용자 행은 익명화하여 보존한다.
       // 투표/채팅 등 user_id FK 참조 데이터를 유지하기 위함(물리 삭제 시 FK 제약 위반으로 실패).
       tokenRepository.deleteAll(list);
       user.withdraw(Instant.now());
       userDeleteRepository.save(delAccount);

       return null;
   }

   public UserImageResponse getRandomColor(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        return new UserImageResponse(userImageService.getRandomColor());
   }
}
