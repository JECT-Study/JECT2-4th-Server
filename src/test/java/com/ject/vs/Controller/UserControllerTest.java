package com.ject.vs.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ject.vs.controller.UserController;
import com.ject.vs.domain.Gender;
import com.ject.vs.domain.ImageColor;
import com.ject.vs.dto.UserExtraInfo;
import com.ject.vs.dto.UserProfileResponse;
import com.ject.vs.config.OAuth2LoginSuccessHandler;
import com.ject.vs.service.CustomOAuth2UserService;
import com.ject.vs.service.UserService;
import com.ject.vs.util.CookieUtil;
import com.ject.vs.util.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Year;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CookieUtil cookieUtil;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList());

    @Test
    @DisplayName("추가 정보 설정 API - 성공")
    void setupInfo_Success() throws Exception {
        UserExtraInfo extraInfo = new UserExtraInfo(Year.of(2001), Gender.MALE, "홍길동", ImageColor.GREEN);
        UserProfileResponse response = new UserProfileResponse(extraInfo.birthDate(), extraInfo.gender(), extraInfo.nickName(), extraInfo.imageColor());

        given(userService.setupAdditionalInfo(any(), any(Long.class))).willReturn(response);

        mockMvc.perform(post("/api/users/me/profile")
                .with(authentication(AUTH))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(extraInfo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nickname").value(extraInfo.nickName()))
                .andExpect(jsonPath("$.gender").value(extraInfo.gender().name()))
                .andExpect(jsonPath("$.imageColor").value(extraInfo.imageColor().toString()));
    }
}
