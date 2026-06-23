package com.ject.vs.user.adapter.web;

import com.ject.vs.config.CookieProperties;
import com.ject.vs.user.adapter.web.dto.*;
import com.ject.vs.user.port.UserProfileQueryService;
import com.ject.vs.user.port.UserService;
import com.ject.vs.vote.exception.UnauthorizedException;
import com.ject.vs.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자", description = "사용자 프로필 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;
    private final UserProfileQueryService userProfileQueryService;
    private final CookieProperties cookieProperties;

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

    @PostMapping("/nickname/slang")
    public ResponseEntity<NicknameCheckResponse> checkNicknameSlang(@AuthenticationPrincipal Long userId, @RequestBody UserNicknameRec nickname) {
        return ResponseEntity.ok(userService.checkNicknameSlang(nickname.nickname(), userId));
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

    @Operation(
            summary = "다른 유저 프로필 바텀시트 조회",
            description = "채팅 화면에서 닉네임/프로필 탭 시 노출되는 다른 유저 프로필 정보를 조회합니다. 회원 전용입니다."
    )
    @GetMapping("/{userId}/profile-sheet")
    public ResponseEntity<UserProfileBottomSheetResponse> getProfileBottomSheet(
            @AuthenticationPrincipal Long viewerUserId,
            @PathVariable Long userId) {
        if (viewerUserId == null) {
            throw new UnauthorizedException();
        }
        return ResponseEntity.ok(userProfileQueryService.getProfileBottomSheet(userId, viewerUserId));
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

    @Operation(summary = "회원 탈퇴", description = "회원을 탈퇴 처리하고 인증 쿠키를 만료시킵니다.")
    @DeleteMapping("/profile/delete")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal Long userId, @RequestBody UserDeleteReq req) {
        try {
            log.info("=== 회원 탈퇴 요청 시작 === userId: {}, req: {}", userId, req);

            userService.deleteAccount(userId, req);

            ResponseCookie accessTokenCookie = expiredCookie(CookieUtil.CookieType.ACCESS_TOKEN);
            ResponseCookie refreshTokenCookie = expiredCookie(CookieUtil.CookieType.REFRESH_TOKEN);

            log.info("=== 회원 탈퇴 성공 ===");
            return ResponseEntity.noContent()
                    .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                    .build();

        } catch (Exception e) {
            // 👈 2. 여기가 핵심입니다. 에러가 나면 이 블록으로 들어와 로그를 남깁니다.
            log.error("❌ [회원 탈퇴 에러 발생] 원인 메시지: {}", e.getMessage());
            log.error("❌ 상세 에러 스택트레이스: ", e); // e 뒤에 아무것도 붙이지 않아야 전체 에러 줄번호가 찍힙니다.

            throw e; // 로그만 찍고 에러는 원래대로 500으로 던져줍니다.
        }
    }

    private ResponseCookie expiredCookie(String name) {
        return ResponseCookie.from(name, "")
                .path("/")
                .domain(".vs.io.kr")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .maxAge(0)
                .build();
    }

    @GetMapping("/imagecolor/suggest")
    public UserImageResponse getImageColor(@AuthenticationPrincipal Long userId) {
        return userService.getRandomColor(userId);
    }
}
