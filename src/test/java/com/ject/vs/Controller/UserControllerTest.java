package com.ject.vs.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ject.vs.controller.UserController;
import com.ject.vs.domain.Gender;
import com.ject.vs.domain.ImageColor;
import com.ject.vs.dto.UserExtraInfo;
import com.ject.vs.dto.UserProfileResponse;
import com.ject.vs.service.UserService;
import com.ject.vs.util.CookieUtil;
import com.ject.vs.util.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Year;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

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

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("추가 정보 설정 API - 성공")
    @WithMockUser
    void setupInfo_Success() throws Exception {
        // 가짜 토큰
        String mockToken = "mock-access-token";

        // 사용자가 보낼 데이터
        UserExtraInfo extraInfo = new UserExtraInfo(Year.of(2001), Gender.MALE, "홍길동", ImageColor.GREEN);

        // 서버에 보내줄 데이터
        UserProfileResponse response = new UserProfileResponse(extraInfo.birthDate(), extraInfo.gender(), extraInfo.nickName(), extraInfo.imageColor());

        given(cookieUtil.getCookieValue(any(), any())).willReturn(mockToken);
        given(userService.setupAdditionalInfo(any(), eq(mockToken))).willReturn(response);

        mockMvc.perform(post("/api/users/me/profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(extraInfo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nickname").value(extraInfo.nickName()))
                .andExpect(jsonPath("$.gender").value(extraInfo.gender().name()))
                .andExpect(jsonPath("$.imageColor").value(extraInfo.imageColor().toString()));
    }
}
