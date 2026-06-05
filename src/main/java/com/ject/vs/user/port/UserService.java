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
        // 활성 사용자(탈퇴 제외)만 조회한다. 재가입은 기존 탈퇴 계정을 복구하지 않고 새 row를 생성한다.
        return userRepository.findByEmailAndUserStatusNot(email, UserStatus.WITHDRAWN)
                .orElseGet(() -> userRepository.save(User.createWithEmail(email)));
    }

    public NicknameCheckResponse checkNickname(String nickName, Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        return new NicknameCheckResponse(userRepository.isNicknameAvailable(nickName));
    }

    public NicknameCheckResponse checkNicknameSlang(String nickName, Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        return new NicknameCheckResponse(!wordService.containSlang(nickName));
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

        if(req.nickname().equals(user.getNickname())) {
            User.modifyImageColor(user, req.imageColor());

            return new UserMyPageResponse(user.getEmail(), user.getNickname(), user.getImageColor());
        }
        else if(!userRepository.isNicknameAvailable(req.nickname())) {
            throw new BusinessException(UserErrorCode.USER_NICKNAME_DUPLICATE);
        }

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
