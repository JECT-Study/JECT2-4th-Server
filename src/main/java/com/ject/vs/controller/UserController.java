package com.ject.vs.controller;

import com.ject.vs.dto.*;
import com.ject.vs.service.UserService;
import com.ject.vs.util.CookieUtil;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final CookieUtil cookieUtil;

    @PostMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> setupInfo(@AuthenticationPrincipal Long userId, @RequestBody UserExtraInfo userExtraInfo) {
        UserProfileResponse response = userService.setupAdditionalInfo(userExtraInfo, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/nickname/check")
    public ResponseEntity<NicknameCheckResponse> isUniqueNickname(@AuthenticationPrincipal Long userId, @RequestBody UserNicknameRec nickname) {
        NicknameCheckResponse response = userService.checkNickname(nickname.nickname(), userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/nickname/suggest")
    public ResponseEntity<UserNicknameRec> suggestNickname(@AuthenticationPrincipal Long userId) {
        UserNicknameRec response = userService.suggestNickname(userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal Long userId) {
        UserProfileResponse response = userService.getUserProfile(userId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/info")
    public ResponseEntity<UserProfileDefaultResponse> initializeDefaultProfile(@AuthenticationPrincipal Long userId, @RequestBody UserProfileRequest userInfo) {
        return ResponseEntity.ok(userService.initializeDefaultProfile(userId, userInfo));
    }
}
