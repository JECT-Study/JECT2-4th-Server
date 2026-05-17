package com.ject.vs.vote.adapter.web;

import com.ject.vs.config.OAuth2LoginSuccessHandler;
import com.ject.vs.config.TestPropertiesConfig;
import org.springframework.context.annotation.Import;
import com.ject.vs.auth.port.CustomOAuth2UserService;
import com.ject.vs.util.CookieUtil;
import com.ject.vs.util.JwtProvider;
import com.ject.vs.vote.domain.InsightScope;
import com.ject.vs.vote.domain.VoteStatus;
import com.ject.vs.vote.exception.VoteNotFoundException;
import com.ject.vs.vote.exception.VoteNotEndedException;
import com.ject.vs.vote.port.in.VoteCommandUseCase.OptionResult;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase.AgeDistribution;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase.AiInsightView;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase.GenderDistribution;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase.Insight;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase.ShareLinkResult;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase.VoteResultDetail;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VoteResultController.class)
@Import(TestPropertiesConfig.class)
class VoteResultControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean VoteResultQueryUseCase voteResultQueryUseCase;
    @MockBean JwtProvider jwtProvider;
    @MockBean CookieUtil cookieUtil;
    @MockBean OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    @MockBean CustomOAuth2UserService customOAuth2UserService;

    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList());

    private VoteResultDetail sampleResult() {
        return new VoteResultDetail(1L, "제목", Instant.parse("2025-01-01T00:00:00Z"),
                "내용", "https://cdn.example.com/thumb.jpg", VoteStatus.ENDED,
                Instant.parse("2025-01-01T01:00:00Z"), 10, List.of(), false, null,
                VoteResultQueryUseCase.Insight.ofLocked(), VoteResultQueryUseCase.AiInsightView.unavailable());
    }

    @Nested
    class getResult {

        @Test
        void 회원_결과_200_반환() throws Exception {
            given(voteResultQueryUseCase.getResult(eq(1L), eq(1L))).willReturn(sampleResult());

            mockMvc.perform(get("/api/votes/1/result").with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.voteId").value(1))
                    .andExpect(jsonPath("$.title").value("제목"))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.content").value("내용"))
                    .andExpect(jsonPath("$.thumbnailUrl").exists());
        }

        @Test
        @WithMockUser
        void 비회원_결과_200_반환() throws Exception {
            given(voteResultQueryUseCase.getResult(eq(1L), isNull())).willReturn(sampleResult());

            mockMvc.perform(get("/api/votes/1/result"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        void 진행중인_투표_403() throws Exception {
            given(voteResultQueryUseCase.getResult(any(), any())).willThrow(new VoteNotEndedException());

            mockMvc.perform(get("/api/votes/1/result"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("VOTE_NOT_ENDED"));
        }

        @Test
        @WithMockUser
        void 존재하지_않는_투표_404() throws Exception {
            given(voteResultQueryUseCase.getResult(any(), any())).willThrow(new VoteNotFoundException());

            mockMvc.perform(get("/api/votes/99/result"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class getShareLink {

        @Test
        @WithMockUser
        void 공유링크_200_반환() throws Exception {
            given(voteResultQueryUseCase.getShareLink(1L))
                    .willReturn(new ShareLinkResult("https://vs.app/poll/result/1", "제목", "https://cdn.vs.app/thumb/1.jpg"));

            mockMvc.perform(get("/api/votes/1/share"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.shareUrl").value("https://vs.app/poll/result/1"))
                    .andExpect(jsonPath("$.title").value("제목"))
                    .andExpect(jsonPath("$.thumbnailUrl").value("https://cdn.vs.app/thumb/1.jpg"));
        }

        @Test
        @WithMockUser
        void 존재하지_않는_투표_공유링크_404() throws Exception {
            given(voteResultQueryUseCase.getShareLink(999L))
                    .willThrow(new VoteNotFoundException());

            mockMvc.perform(get("/api/votes/999/share"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("VOTE_NOT_FOUND"));
        }
    }

    @Nested
    class 회원_참여자_결과 {

        @Test
        void 회원_참여O_MY_SELECTION_스코프() throws Exception {
            List<OptionResult> options = List.of(
                    new OptionResult(10L, "혼밥이 편하다", 364L, 70),
                    new OptionResult(11L, "같이 먹기", 156L, 30)
            );
            Insight insight = new Insight(
                    false,
                    InsightScope.MY_SELECTION,
                    156,
                    new GenderDistribution(60, 38, 96, 62),
                    List.of(
                            new AgeDistribution("20s", 28, true),
                            new AgeDistribution("30s", 52, false),
                            new AgeDistribution("40s", 20, false)
                    )
            );
            AiInsightView aiInsight = AiInsightView.of(
                    "20대 여성 그룹에서 \"같이 밥먹기\"를 선택한 비율이 71%로 가장 높게 나타났어요.",
                    "MZ 세대를 중심으로 혼밥 문화가 확산되는 트렌드가 반영된 결과예요."
            );
            VoteResultDetail result = new VoteResultDetail(
                    1L, "직장인 점심시간 혼밥 vs 같이 먹기",
                    Instant.parse("2026-04-14T04:49:00Z"), "내용", "https://cdn.example.com/thumb.jpg",
                    VoteStatus.ENDED, Instant.parse("2026-04-14T14:59:00Z"), 520, options, true, 11L, insight, aiInsight
            );
            given(voteResultQueryUseCase.getResult(eq(1L), eq(1L))).willReturn(result);

            mockMvc.perform(get("/api/votes/1/result").with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.voteId").value(1))
                    .andExpect(jsonPath("$.status").value("ENDED"))
                    .andExpect(jsonPath("$.participantCount").value(520))
                    .andExpect(jsonPath("$.result.options[0].optionId").value(10))
                    .andExpect(jsonPath("$.myVote.voted").value(true))
                    .andExpect(jsonPath("$.myVote.selectedOptionId").value(11))
                    .andExpect(jsonPath("$.insight.locked").value(false))
                    .andExpect(jsonPath("$.insight.scope").value("MY_SELECTION"))
                    .andExpect(jsonPath("$.insight.selectionCount").value(156))
                    .andExpect(jsonPath("$.insight.genderDistribution.female.count").value(96))
                    .andExpect(jsonPath("$.insight.genderDistribution.female.ratio").value(62))
                    .andExpect(jsonPath("$.insight.genderDistribution.male.count").value(60))
                    .andExpect(jsonPath("$.insight.genderDistribution.male.ratio").value(38))
                    .andExpect(jsonPath("$.insight.ageDistribution[0].ageGroup").value("20s"))
                    .andExpect(jsonPath("$.insight.ageDistribution[0].isMyGroup").value(true))
                    .andExpect(jsonPath("$.aiInsight.available").value(true))
                    .andExpect(jsonPath("$.aiInsight.headline").exists());
        }
    }

    @Nested
    class 회원_미참여자_결과 {

        @Test
        void 회원_참여X_TOTAL_스코프() throws Exception {
            List<OptionResult> options = List.of(
                    new OptionResult(10L, "혼밥이 편하다", 364L, 70),
                    new OptionResult(11L, "같이 먹기", 156L, 30)
            );
            Insight insight = new Insight(
                    false,
                    InsightScope.TOTAL,
                    364,
                    new GenderDistribution(139, 38, 225, 62),
                    List.of(
                            new AgeDistribution("20s", 28, false),
                            new AgeDistribution("30s", 52, false),
                            new AgeDistribution("40s", 20, false)
                    )
            );
            VoteResultDetail result = new VoteResultDetail(
                    1L, "직장인 점심시간 혼밥 vs 같이 먹기",
                    Instant.parse("2026-04-14T04:49:00Z"), "내용", "https://cdn.example.com/thumb.jpg",
                    VoteStatus.ENDED, Instant.parse("2026-04-14T14:59:00Z"), 520, options, false, null, insight, AiInsightView.unavailable()
            );
            given(voteResultQueryUseCase.getResult(eq(1L), eq(1L))).willReturn(result);

            mockMvc.perform(get("/api/votes/1/result").with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.myVote.voted").value(false))
                    .andExpect(jsonPath("$.myVote.selectedOptionId").doesNotExist())
                    .andExpect(jsonPath("$.insight.scope").value("TOTAL"))
                    .andExpect(jsonPath("$.insight.ageDistribution[0].isMyGroup").value(false))
                    .andExpect(jsonPath("$.aiInsight.available").value(false))
                    .andExpect(jsonPath("$.aiInsight.headline").doesNotExist());
        }
    }

    @Nested
    class 비회원_결과 {

        @Test
        @WithMockUser
        void 비회원_insight_locked() throws Exception {
            List<OptionResult> options = List.of(
                    new OptionResult(10L, "혼밥이 편하다", 364L, 70),
                    new OptionResult(11L, "같이 먹기", 156L, 30)
            );
            VoteResultDetail result = new VoteResultDetail(
                    1L, "직장인 점심시간 혼밥 vs 같이 먹기",
                    Instant.parse("2026-04-14T04:49:00Z"), "내용", "https://cdn.example.com/thumb.jpg",
                    VoteStatus.ENDED, Instant.parse("2026-04-14T14:59:00Z"), 520, options, false, null,
                    Insight.ofLocked(), AiInsightView.unavailable()
            );
            given(voteResultQueryUseCase.getResult(eq(1L), isNull())).willReturn(result);

            mockMvc.perform(get("/api/votes/1/result"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.insight.locked").value(true))
                    .andExpect(jsonPath("$.insight.scope").doesNotExist())
                    .andExpect(jsonPath("$.insight.genderDistribution").doesNotExist())
                    .andExpect(jsonPath("$.aiInsight.available").value(false));
        }
    }
}
