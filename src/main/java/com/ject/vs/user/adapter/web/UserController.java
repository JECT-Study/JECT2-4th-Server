package com.ject.vs.user.adapter.web;

import com.ject.vs.user.adapter.web.dto.*;
import com.ject.vs.user.port.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자", description = "사용자 프로필 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(summary = "추가 정보 설정", description = "사용자 추가 정보(닉네임, 성별, 생년월일)를 설정합니다.")
    @PostMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> setupInfo(@AuthenticationPrincipal Long userId, @RequestBody UserExtraInfo userExtraInfo) {
        UserProfileResponse response = userService.setupAdditionalInfo(userExtraInfo, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "닉네임 중복 확인", description = "닉네임 사용 가능 여부를 확인합니다.")
    @PostMapping("/nickname/check")
    public ResponseEntity<NicknameCheckResponse> isUniqueNickname(@AuthenticationPrincipal Long userId, @RequestBody UserNicknameRec nickname) {
        NicknameCheckResponse response = userService.checkNickname(nickname.nickname(), userId);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "닉네임 추천", description = "사용 가능한 랜덤 닉네임을 추천합니다.")
    @GetMapping("/nickname/suggest")
    public ResponseEntity<UserNicknameRec> suggestNickname(@AuthenticationPrincipal Long userId) {
        UserNicknameRec response = userService.suggestNickname(userId);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 프로필 조회", description = "로그인한 사용자의 프로필 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal Long userId) {
        UserProfileResponse response = userService.getUserProfile(userId);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "기본 프로필 초기화", description = "사용자 기본 프로필 정보를 초기화합니다.")
    @PostMapping("/info")
    public ResponseEntity<UserProfileDefaultResponse> initializeDefaultProfile(@AuthenticationPrincipal Long userId, @RequestBody UserProfileRequest userInfo) {
        return ResponseEntity.ok(userService.initializeDefaultProfile(userId, userInfo));
    }

    @PatchMapping("/change/info")
    public ResponseEntity<?> modifyNickname(@AuthenticationPrincipal Long userId, @RequestBody UserModifyInfoRequest req) {
        return ResponseEntity.ok(userService.modifyInfo(userId, req));
    }

    @DeleteMapping("/profile/delete")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal Long userId, @RequestBody UserDeleteReq req) {
        userService.deleteAccount(userId, req);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/imagecolor/suggest")
    public UserImageResponse getImageColor(@AuthenticationPrincipal Long userId) {
        return userService.getRandomColor(userId);
    }
}
