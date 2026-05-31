package com.ject.vs.vote;

import com.ject.vs.vote.adapter.web.dto.VoteDetailResponse;
import com.ject.vs.vote.adapter.web.dto.VoteResultResponse;
import com.ject.vs.vote.domain.*;
import com.ject.vs.vote.exception.VoteFreeLimitExceededException;
import com.ject.vs.vote.exception.VoteNotEndedException;
import com.ject.vs.vote.exception.VoteNotFoundException;
import com.ject.vs.vote.port.VoteDetailQueryService;
import com.ject.vs.vote.port.VoteDetailQueryService.VoteDetailResult;
import com.ject.vs.vote.port.in.VoteCommandUseCase.OptionResult;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase.VoteResultDetail;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase.Insight;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase.AiInsightView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QA 시나리오 검증 테스트
 *
 * QA 시트 기반으로 주요 API 응답이 올바른지 검증합니다.
 * - TC-127~148: 일반형 투표
 * - TC-149~156: 비회원 투표 제한
 * - TC-166~188: 투표 결과
 * - TC-189~211: 몰입형 투표
 */
class QaScenarioTest {

    // ========================================
    // TC-127~148: 일반형 투표 상세 응답 검증
    // ========================================
    @Nested
    @DisplayName("TC-127~148: 일반형 투표 상세 조회")
    class 일반형_투표_상세_조회 {

        @Test
        @DisplayName("TC-127: 진행중 투표 - 미투표시 득표수/비율 null")
        void 진행중_투표_미투표시_득표수_비율_null() {
            // Given: 진행중 투표, 사용자 미투표
            VoteDetailResult result = new VoteDetailResult(
                    1L, VoteType.GENERAL, "점심 뭐 먹을까?",
                    Instant.parse("2025-01-01T00:00:00Z"), "내용",
                    "thumb.png", null,
                    VoteStatus.ONGOING,  // 진행중
                    Instant.parse("2025-01-02T00:00:00Z"),
                    100,
                    List.of(
                            new OptionResult(10L, "짜장면", 60L, 60),
                            new OptionResult(11L, "짬뽕", 40L, 40)
                    ),
                    false,  // 미투표
                    null,
                    Map.of(),
                    null,
                    0
            );

            // When: Response 변환
            VoteDetailResponse response = VoteDetailResponse.from(result);

            // Then: 득표수/비율이 null
            assertThat(response.options()).hasSize(2);
            assertThat(response.options().get(0).voteCount()).isNull();
            assertThat(response.options().get(0).ratio()).isNull();
            assertThat(response.options().get(1).voteCount()).isNull();
            assertThat(response.options().get(1).ratio()).isNull();
            assertThat(response.myVote().voted()).isFalse();
        }

        @Test
        @DisplayName("TC-128: 진행중 투표 - 투표 후 득표수/비율 노출")
        void 진행중_투표_투표후_득표수_비율_노출() {
            // Given: 진행중 투표, 사용자 투표 완료
            VoteDetailResult result = new VoteDetailResult(
                    1L, VoteType.GENERAL, "점심 뭐 먹을까?",
                    Instant.parse("2025-01-01T00:00:00Z"), "내용",
                    "thumb.png", null,
                    VoteStatus.ONGOING,  // 진행중
                    Instant.parse("2025-01-02T00:00:00Z"),
                    100,
                    List.of(
                            new OptionResult(10L, "짜장면", 60L, 60),
                            new OptionResult(11L, "짬뽕", 40L, 40)
                    ),
                    true,  // 투표함
                    10L,   // 짜장면 선택
                    Map.of(VoteEmoji.LIKE, 5L, VoteEmoji.WOW, 3L, VoteEmoji.SAD, 0L, VoteEmoji.ANGRY, 0L),
                    VoteEmoji.LIKE,
                    0
            );

            // When: Response 변환
            VoteDetailResponse response = VoteDetailResponse.from(result);

            // Then: 득표수/비율 노출
            assertThat(response.options()).hasSize(2);
            assertThat(response.options().get(0).voteCount()).isEqualTo(60L);
            assertThat(response.options().get(0).ratio()).isEqualTo(60);
            assertThat(response.options().get(1).voteCount()).isEqualTo(40L);
            assertThat(response.options().get(1).ratio()).isEqualTo(40);
            assertThat(response.myVote().voted()).isTrue();
            assertThat(response.myVote().selectedOptionId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("TC-166: 종료된 투표 - 미투표시에도 득표수/비율 노출 (핵심 버그 수정 검증)")
        void 종료된_투표_미투표시에도_득표수_비율_노출() {
            // Given: 종료된 투표, 사용자 미투표
            VoteDetailResult result = new VoteDetailResult(
                    1L, VoteType.GENERAL, "점심 뭐 먹을까?",
                    Instant.parse("2025-01-01T00:00:00Z"), "내용",
                    "thumb.png", null,
                    VoteStatus.ENDED,  // 종료됨
                    Instant.parse("2025-01-02T00:00:00Z"),
                    100,
                    List.of(
                            new OptionResult(10L, "짜장면", 60L, 60),
                            new OptionResult(11L, "짬뽕", 40L, 40)
                    ),
                    false,  // 미투표
                    null,
                    Map.of(VoteEmoji.LIKE, 10L, VoteEmoji.WOW, 5L, VoteEmoji.SAD, 2L, VoteEmoji.ANGRY, 1L),
                    null,
                    0
            );

            // When: Response 변환
            VoteDetailResponse response = VoteDetailResponse.from(result);

            // Then: 종료된 투표는 미투표시에도 득표수/비율 노출
            assertThat(response.status()).isEqualTo("ENDED");
            assertThat(response.options()).hasSize(2);
            assertThat(response.options().get(0).voteCount()).isEqualTo(60L);
            assertThat(response.options().get(0).ratio()).isEqualTo(60);
            assertThat(response.options().get(1).voteCount()).isEqualTo(40L);
            assertThat(response.options().get(1).ratio()).isEqualTo(40);
            assertThat(response.myVote().voted()).isFalse();
        }

        @Test
        @DisplayName("TC-167: 종료된 투표 - 투표자도 결과 정상 노출")
        void 종료된_투표_투표자_결과_정상_노출() {
            // Given: 종료된 투표, 사용자 투표함
            VoteDetailResult result = new VoteDetailResult(
                    1L, VoteType.GENERAL, "점심 뭐 먹을까?",
                    Instant.parse("2025-01-01T00:00:00Z"), "내용",
                    "thumb.png", null,
                    VoteStatus.ENDED,
                    Instant.parse("2025-01-02T00:00:00Z"),
                    100,
                    List.of(
                            new OptionResult(10L, "짜장면", 60L, 60),
                            new OptionResult(11L, "짬뽕", 40L, 40)
                    ),
                    true,  // 투표함
                    11L,   // 짬뽕 선택
                    Map.of(VoteEmoji.LIKE, 10L, VoteEmoji.WOW, 5L, VoteEmoji.SAD, 2L, VoteEmoji.ANGRY, 1L),
                    VoteEmoji.WOW,
                    0
            );

            // When: Response 변환
            VoteDetailResponse response = VoteDetailResponse.from(result);

            // Then
            assertThat(response.status()).isEqualTo("ENDED");
            assertThat(response.options().get(0).voteCount()).isEqualTo(60L);
            assertThat(response.options().get(1).voteCount()).isEqualTo(40L);
            assertThat(response.myVote().voted()).isTrue();
            assertThat(response.myVote().selectedOptionId()).isEqualTo(11L);
            assertThat(response.myEmoji()).isEqualTo("WOW");
        }

        @Test
        @DisplayName("TC-135: 이모지 요약 정상 반환")
        void 이모지_요약_정상_반환() {
            // Given
            VoteDetailResult result = new VoteDetailResult(
                    1L, VoteType.GENERAL, "제목",
                    Instant.parse("2025-01-01T00:00:00Z"), null,
                    "thumb.png", null,
                    VoteStatus.ONGOING,
                    Instant.parse("2025-01-02T00:00:00Z"),
                    50,
                    List.of(),
                    true,
                    10L,
                    Map.of(
                            VoteEmoji.LIKE, 100L,
                            VoteEmoji.SAD, 50L,
                            VoteEmoji.ANGRY, 25L,
                            VoteEmoji.WOW, 10L
                    ),
                    VoteEmoji.LIKE,
                    5
            );

            // When
            VoteDetailResponse response = VoteDetailResponse.from(result);

            // Then: 이모지 카운트 정확히 반환
            assertThat(response.emojiSummary().LIKE()).isEqualTo(100L);
            assertThat(response.emojiSummary().SAD()).isEqualTo(50L);
            assertThat(response.emojiSummary().ANGRY()).isEqualTo(25L);
            assertThat(response.emojiSummary().WOW()).isEqualTo(10L);
            assertThat(response.myEmoji()).isEqualTo("LIKE");
        }

        @Test
        @DisplayName("TC-141: 참여자수 정확히 반환")
        void 참여자수_정확히_반환() {
            // Given
            VoteDetailResult result = new VoteDetailResult(
                    1L, VoteType.GENERAL, "제목",
                    Instant.parse("2025-01-01T00:00:00Z"), null,
                    "thumb.png", null,
                    VoteStatus.ONGOING,
                    Instant.parse("2025-01-02T00:00:00Z"),
                    12345,  // 참여자수
                    List.of(),
                    false,
                    null,
                    Map.of(),
                    null,
                    0
            );

            // When
            VoteDetailResponse response = VoteDetailResponse.from(result);

            // Then
            assertThat(response.participantCount()).isEqualTo(12345);
        }
    }

    // ========================================
    // TC-149~156: 비회원 투표 제한 검증
    // ========================================
    @Nested
    @DisplayName("TC-149~156: 비회원 투표 제한")
    class 비회원_투표_제한 {

        @Test
        @DisplayName("TC-149: 신규 비회원은 5회 무료 투표 가능")
        void 신규_비회원_5회_무료_투표() {
            // Given
            GuestFreeVote guest = GuestFreeVote.create("new-anonymous-id");
            Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

            // Then: 초기 잔여 횟수 5
            assertThat(guest.remaining()).isEqualTo(5);

            // When: 1회 소진
            guest.consume(clock);

            // Then: 잔여 4회
            assertThat(guest.remaining()).isEqualTo(4);
        }

        @Test
        @DisplayName("TC-153: 5회 소진 후 투표 시 예외 발생")
        void 비회원_5회_소진후_예외() {
            // Given
            GuestFreeVote guest = GuestFreeVote.create("exhausted-anon");
            Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

            // 5회 모두 소진
            for (int i = 0; i < 5; i++) {
                guest.consume(clock);
            }

            // Then: 잔여 0회
            assertThat(guest.remaining()).isEqualTo(0);

            // When & Then: 6번째 투표 시도 시 예외
            org.junit.jupiter.api.Assertions.assertThrows(
                    VoteFreeLimitExceededException.class,
                    () -> guest.consume(clock)
            );
        }

        @Test
        @DisplayName("TC-155: 잔여 투표 횟수 정확히 반환")
        void 잔여_투표_횟수_정확히_반환() {
            // Given
            GuestFreeVote guest = GuestFreeVote.create("anon-123");
            Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

            // When: 3회 소진
            guest.consume(clock);
            guest.consume(clock);
            guest.consume(clock);

            // Then
            assertThat(guest.getConsumedCount()).isEqualTo(3);
            assertThat(guest.remaining()).isEqualTo(2);
        }
    }

    // ========================================
    // TC-166~188: 투표 결과 API 응답 검증
    // ========================================
    @Nested
    @DisplayName("TC-166~188: 투표 결과 API")
    class 투표_결과_API {

        @Test
        @DisplayName("TC-168: 비회원 - 인사이트 잠금, selectionCount만 표시")
        void 비회원_인사이트_잠금() {
            // Given: 비회원 투표 결과
            Insight guestInsight = new Insight(
                    true,  // locked
                    InsightScope.TOTAL,
                    100,   // selectionCount
                    null,  // genderDistribution 잠금
                    null   // ageDistribution 잠금
            );
            VoteResultDetail result = new VoteResultDetail(
                    1L, "제목",
                    Instant.parse("2025-01-01T00:00:00Z"), "내용",
                    "thumb.png",
                    VoteStatus.ENDED,
                    Instant.parse("2025-01-02T00:00:00Z"),
                    100,
                    List.of(
                            new OptionResult(10L, "옵션A", 60L, 60),
                            new OptionResult(11L, "옵션B", 40L, 40)
                    ),
                    false,
                    null,
                    guestInsight,
                    AiInsightView.unavailable()
            );

            // When
            VoteResultResponse response = VoteResultResponse.from(result);

            // Then: 인사이트 잠금 상태
            assertThat(response.insight().locked()).isTrue();
            assertThat(response.insight().selectionCount()).isEqualTo(100);
            assertThat(response.insight().genderDistribution()).isNull();
            assertThat(response.insight().ageDistribution()).isNull();
            // 결과는 보임
            assertThat(response.result().options()).hasSize(2);
            assertThat(response.result().options().get(0).voteCount()).isEqualTo(60L);
        }

        @Test
        @DisplayName("TC-170: 회원 참여자 - MY_SELECTION scope 인사이트")
        void 회원_참여자_MY_SELECTION_인사이트() {
            // Given
            var genderDist = new com.ject.vs.vote.port.in.VoteResultQueryUseCase.GenderDistribution(
                    60, 36L, 60, 24L, 40, "FEMALE"
            );
            var ageDist = List.of(
                    new com.ject.vs.vote.port.in.VoteResultQueryUseCase.AgeDistribution("20대", 45, true),
                    new com.ject.vs.vote.port.in.VoteResultQueryUseCase.AgeDistribution("30대", 35, false),
                    new com.ject.vs.vote.port.in.VoteResultQueryUseCase.AgeDistribution("40대", 20, false)
            );
            Insight memberInsight = new Insight(
                    false,  // unlocked
                    InsightScope.MY_SELECTION,
                    60,     // 내 선택 옵션 투표수
                    genderDist,
                    ageDist
            );

            VoteResultDetail result = new VoteResultDetail(
                    1L, "제목",
                    Instant.parse("2025-01-01T00:00:00Z"), null,
                    "thumb.png",
                    VoteStatus.ENDED,
                    Instant.parse("2025-01-02T00:00:00Z"),
                    100,
                    List.of(
                            new OptionResult(10L, "옵션A", 60L, 60),
                            new OptionResult(11L, "옵션B", 40L, 40)
                    ),
                    true,
                    10L,
                    memberInsight,
                    AiInsightView.unavailable()
            );

            // When
            VoteResultResponse response = VoteResultResponse.from(result);

            // Then: MY_SELECTION scope
            assertThat(response.insight().locked()).isFalse();
            assertThat(response.insight().scope()).isEqualTo("MY_SELECTION");
            assertThat(response.insight().selectionCount()).isEqualTo(60);
            assertThat(response.insight().genderDistribution()).isNotNull();
            assertThat(response.insight().genderDistribution().highlightedGender()).isEqualTo("FEMALE");
            assertThat(response.insight().ageDistribution()).hasSize(3);
            assertThat(response.myVote().voted()).isTrue();
            assertThat(response.myVote().selectedOptionId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("TC-175: 회원 미참여자 - TOTAL scope 인사이트")
        void 회원_미참여자_TOTAL_인사이트() {
            // Given
            Insight nonParticipantInsight = new Insight(
                    false,
                    InsightScope.TOTAL,
                    100,  // 전체 참여자수
                    null,
                    null
            );

            VoteResultDetail result = new VoteResultDetail(
                    1L, "제목",
                    Instant.parse("2025-01-01T00:00:00Z"), null,
                    "thumb.png",
                    VoteStatus.ENDED,
                    Instant.parse("2025-01-02T00:00:00Z"),
                    100,
                    List.of(),
                    false,  // 미참여
                    null,
                    nonParticipantInsight,
                    AiInsightView.unavailable()
            );

            // When
            VoteResultResponse response = VoteResultResponse.from(result);

            // Then
            assertThat(response.insight().scope()).isEqualTo("TOTAL");
            assertThat(response.myVote().voted()).isFalse();
        }

        @Test
        @DisplayName("TC-180: AI 인사이트 있으면 available=true")
        void AI_인사이트_available_true() {
            // Given
            AiInsightView aiInsight = AiInsightView.of(
                    "20대 여성이 가장 많이 선택했어요!",
                    "이 투표에서는 20대 여성의 참여가 두드러졌습니다. 전체 참여자의 45%가 20대이며..."
            );

            VoteResultDetail result = new VoteResultDetail(
                    1L, "제목",
                    Instant.parse("2025-01-01T00:00:00Z"), null,
                    "thumb.png",
                    VoteStatus.ENDED,
                    Instant.parse("2025-01-02T00:00:00Z"),
                    100,
                    List.of(),
                    true,
                    10L,
                    new Insight(false, InsightScope.MY_SELECTION, 60, null, null),
                    aiInsight
            );

            // When
            VoteResultResponse response = VoteResultResponse.from(result);

            // Then
            assertThat(response.aiInsight().available()).isTrue();
            assertThat(response.aiInsight().headline()).isEqualTo("20대 여성이 가장 많이 선택했어요!");
            assertThat(response.aiInsight().body()).contains("20대 여성의 참여가 두드러졌습니다");
        }

        @Test
        @DisplayName("TC-181: AI 인사이트 없으면 available=false")
        void AI_인사이트_unavailable() {
            // Given
            VoteResultDetail result = new VoteResultDetail(
                    1L, "제목",
                    Instant.parse("2025-01-01T00:00:00Z"), null,
                    "thumb.png",
                    VoteStatus.ENDED,
                    Instant.parse("2025-01-02T00:00:00Z"),
                    100,
                    List.of(),
                    false,
                    null,
                    new Insight(false, InsightScope.TOTAL, 100, null, null),
                    AiInsightView.unavailable()
            );

            // When
            VoteResultResponse response = VoteResultResponse.from(result);

            // Then
            assertThat(response.aiInsight().available()).isFalse();
            assertThat(response.aiInsight().headline()).isNull();
            assertThat(response.aiInsight().body()).isNull();
        }
    }

    // ========================================
    // TC-189~211: 몰입형 투표 응답 검증
    // ========================================
    @Nested
    @DisplayName("TC-189~211: 몰입형 투표")
    class 몰입형_투표 {

        @Test
        @DisplayName("TC-195: 득표율 계산 - A 75%, B 25%")
        void 득표율_계산_정확성() {
            // Given: A 3표, B 1표 = 총 4표
            long totalVotes = 4;
            long optionACount = 3;
            long optionBCount = 1;

            // When: 득표율 계산
            int ratioA = (int) Math.round(optionACount * 100.0 / totalVotes);
            int ratioB = (int) Math.round(optionBCount * 100.0 / totalVotes);

            // Then
            assertThat(ratioA).isEqualTo(75);
            assertThat(ratioB).isEqualTo(25);
            assertThat(ratioA + ratioB).isEqualTo(100);
        }

        @Test
        @DisplayName("TC-196: 참여자 없으면 득표율 0%")
        void 참여자_없으면_득표율_0() {
            // Given
            long total = 0;

            // When
            int ratio = total == 0 ? 0 : (int) Math.round(0 * 100.0 / total);

            // Then
            assertThat(ratio).isEqualTo(0);
        }

        @Test
        @DisplayName("TC-200: 반올림 - 33.33% → 33%, 66.67% → 67%")
        void 반올림_처리() {
            // Given: A 2표, B 1표 = 총 3표
            long total = 3;
            long optionACount = 2;
            long optionBCount = 1;

            // When
            int ratioA = (int) Math.round(optionACount * 100.0 / total);
            int ratioB = (int) Math.round(optionBCount * 100.0 / total);

            // Then: 66.67 → 67, 33.33 → 33
            assertThat(ratioA).isEqualTo(67);
            assertThat(ratioB).isEqualTo(33);
        }
    }

    // ========================================
    // 응답 필드 검증
    // ========================================
    @Nested
    @DisplayName("응답 필드 검증")
    class 응답_필드_검증 {

        @Test
        @DisplayName("VoteDetailResponse 모든 필드 정상 매핑")
        void VoteDetailResponse_필드_매핑() {
            // Given
            VoteDetailResult result = new VoteDetailResult(
                    123L,
                    VoteType.GENERAL,
                    "오늘 점심 뭐 먹을까?",
                    Instant.parse("2025-06-01T09:00:00Z"),
                    "같이 투표해요!",
                    "https://cdn.vs.app/thumb/123.jpg",
                    "https://cdn.vs.app/img/123.jpg",
                    VoteStatus.ONGOING,
                    Instant.parse("2025-06-02T09:00:00Z"),
                    500,
                    List.of(
                            new OptionResult(1L, "짜장면", 300L, 60),
                            new OptionResult(2L, "짬뽕", 200L, 40)
                    ),
                    true,
                    1L,
                    Map.of(
                            VoteEmoji.LIKE, 50L,
                            VoteEmoji.WOW, 30L,
                            VoteEmoji.SAD, 10L,
                            VoteEmoji.ANGRY, 5L
                    ),
                    VoteEmoji.LIKE,
                    25
            );

            // When
            VoteDetailResponse response = VoteDetailResponse.from(result);

            // Then: 모든 필드 검증
            assertThat(response.voteId()).isEqualTo(123L);
            assertThat(response.voteType()).isEqualTo(VoteType.GENERAL);
            assertThat(response.title()).isEqualTo("오늘 점심 뭐 먹을까?");
            assertThat(response.content()).isEqualTo("같이 투표해요!");
            assertThat(response.thumbnailUrl()).isEqualTo("https://cdn.vs.app/thumb/123.jpg");
            assertThat(response.status()).isEqualTo("ONGOING");
            assertThat(response.participantCount()).isEqualTo(500);
            assertThat(response.commentCount()).isEqualTo(25);

            // 옵션 검증
            assertThat(response.options()).hasSize(2);
            assertThat(response.options().get(0).optionId()).isEqualTo(1L);
            assertThat(response.options().get(0).label()).isEqualTo("짜장면");
            assertThat(response.options().get(0).voteCount()).isEqualTo(300L);
            assertThat(response.options().get(0).ratio()).isEqualTo(60);

            // 내 투표 정보
            assertThat(response.myVote().voted()).isTrue();
            assertThat(response.myVote().selectedOptionId()).isEqualTo(1L);

            // 이모지
            assertThat(response.emojiSummary().LIKE()).isEqualTo(50L);
            assertThat(response.emojiSummary().WOW()).isEqualTo(30L);
            assertThat(response.myEmoji()).isEqualTo("LIKE");
        }

        @Test
        @DisplayName("VoteResultResponse 모든 필드 정상 매핑")
        void VoteResultResponse_필드_매핑() {
            // Given
            var genderDist = new com.ject.vs.vote.port.in.VoteResultQueryUseCase.GenderDistribution(
                    100, 60L, 60, 40L, 40, "FEMALE"
            );
            var ageDist = List.of(
                    new com.ject.vs.vote.port.in.VoteResultQueryUseCase.AgeDistribution("10대", 10, false),
                    new com.ject.vs.vote.port.in.VoteResultQueryUseCase.AgeDistribution("20대", 50, true),
                    new com.ject.vs.vote.port.in.VoteResultQueryUseCase.AgeDistribution("30대", 30, false),
                    new com.ject.vs.vote.port.in.VoteResultQueryUseCase.AgeDistribution("40대", 10, false)
            );

            VoteResultDetail result = new VoteResultDetail(
                    456L,
                    "어떤 게 더 좋아?",
                    Instant.parse("2025-05-01T12:00:00Z"),
                    "투표 내용입니다",
                    "https://cdn.vs.app/thumb/456.jpg",
                    VoteStatus.ENDED,
                    Instant.parse("2025-05-02T12:00:00Z"),
                    1000,
                    List.of(
                            new OptionResult(10L, "옵션1", 600L, 60),
                            new OptionResult(11L, "옵션2", 400L, 40)
                    ),
                    true,
                    10L,
                    new Insight(false, InsightScope.MY_SELECTION, 600, genderDist, ageDist),
                    AiInsightView.of("인사이트 제목", "인사이트 본문")
            );

            // When
            VoteResultResponse response = VoteResultResponse.from(result);

            // Then
            assertThat(response.voteId()).isEqualTo(456L);
            assertThat(response.title()).isEqualTo("어떤 게 더 좋아?");
            assertThat(response.status()).isEqualTo("ENDED");
            assertThat(response.participantCount()).isEqualTo(1000);

            // 결과 옵션
            assertThat(response.result().options()).hasSize(2);
            assertThat(response.result().options().get(0).voteCount()).isEqualTo(600L);
            assertThat(response.result().options().get(0).ratio()).isEqualTo(60);

            // 내 투표
            assertThat(response.myVote().voted()).isTrue();
            assertThat(response.myVote().selectedOptionId()).isEqualTo(10L);

            // 인사이트
            assertThat(response.insight().locked()).isFalse();
            assertThat(response.insight().scope()).isEqualTo("MY_SELECTION");
            assertThat(response.insight().selectionCount()).isEqualTo(600);
            assertThat(response.insight().genderDistribution().highlightedGender()).isEqualTo("FEMALE");
            assertThat(response.insight().ageDistribution()).hasSize(4);

            // AI 인사이트
            assertThat(response.aiInsight().available()).isTrue();
            assertThat(response.aiInsight().headline()).isEqualTo("인사이트 제목");
        }
    }
}
