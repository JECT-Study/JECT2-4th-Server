package com.ject.vs.controller;

import com.ject.vs.dto.NicknameCheckResponse;
import com.ject.vs.dto.UserExtraInfo;
import com.ject.vs.dto.UserNicknameRec;
import com.ject.vs.dto.UserProfileResponse;
import com.ject.vs.service.UserService;
import com.ject.vs.util.CookieUtil;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final CookieUtil cookieUtil;

    @PostMapping("/users/me/profile")
    public ResponseEntity<UserProfileResponse> setupInfo(HttpServletRequest request, @RequestBody UserExtraInfo userExtraInfo) {
        String accessToken = cookieUtil.getCookieValue(request, CookieUtil.CookieType.ACCESS_TOKEN);
        UserProfileResponse response = userService.setupAdditionalInfo(userExtraInfo, accessToken);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/users/nickname/check")
    public ResponseEntity<NicknameCheckResponse> isUniqueNickname(HttpServletRequest request, @RequestBody UserNicknameRec nickname) {
        String accessToken = cookieUtil.getCookieValue(request, CookieUtil.CookieType.ACCESS_TOKEN);
        NicknameCheckResponse response = userService.checkNickname(nickname.nickname(), accessToken);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/nickname/suggest")
    public ResponseEntity<UserNicknameRec> suggestNickname(HttpServletRequest request) {
        String accessToken = cookieUtil.getCookieValue(request, CookieUtil.CookieType.ACCESS_TOKEN);

        UserNicknameRec response = userService.suggestNickname(accessToken);

        return ResponseEntity.ok(response);
    }
}
