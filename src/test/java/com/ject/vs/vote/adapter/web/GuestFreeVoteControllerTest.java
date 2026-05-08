package com.ject.vs.vote.adapter.web;

import com.ject.vs.config.OAuth2LoginSuccessHandler;
import com.ject.vs.util.CookieUtil;
import com.ject.vs.util.JwtProvider;
import org.springframework.security.test.context.support.WithMockUser;
import com.ject.vs.vote.port.GuestFreeVoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GuestFreeVoteController.class)
class GuestFreeVoteControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean GuestFreeVoteService guestFreeVoteService;
    @MockBean JwtProvider jwtProvider;
    @MockBean CookieUtil cookieUtil;
    @MockBean OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Test
    @WithMockUser
    void 잔여_무료_투표_200_반환() throws Exception {
        given(guestFreeVoteService.remaining(any())).willReturn(3);

        mockMvc.perform(get("/api/me/free-votes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remaining").value(3));
    }

    @Test
    @WithMockUser
    void anonymous_id_쿠키_없으면_Set_Cookie_헤더_발급() throws Exception {
        given(guestFreeVoteService.remaining(any())).willReturn(5);

        mockMvc.perform(get("/api/me/free-votes"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"));
    }
}
