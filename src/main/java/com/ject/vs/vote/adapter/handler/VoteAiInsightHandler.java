package com.ject.vs.vote.adapter.handler;

import com.ject.vs.ai.port.in.AiInsightUseCase;
import com.ject.vs.ai.port.in.AiInsightUseCase.AiInsightResult;
import com.ject.vs.ai.port.in.AiInsightUseCase.VoteInsightRequest;
import com.ject.vs.user.domain.Gender;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.domain.UserRepository;
import com.ject.vs.vote.domain.*;
import com.ject.vs.vote.event.VoteEndedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @deprecated 이 핸들러는 기존 투표 종료 시 1회 생성 방식에서 실시간 개인화 인사이트 생성으로 변경되어 비활성화되었습니다.
 * 새로운 구현은 {@link com.ject.vs.ai.port.PersonalizedAiInsightService}를 참조하세요.
 */
@Deprecated
@Component
@ConditionalOnProperty(name = "app.ai.legacy-insight-handler.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class VoteAiInsightHandler {

    private final VoteRepository voteRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteParticipationRepository voteParticipationRepository;
    private final UserRepository userRepository;
    private final AiInsightUseCase aiInsightUseCase;
    private final Clock clock;

    @EventListener
    @Async("aiInsightExecutor")
    @Transactional
    public void on(VoteEndedEvent event) {
        Vote vote = voteRepository.findById(event.voteId()).orElse(null);
        if (vote == null) {
            log.warn("Vote not found for AI insight generation: {}", event.voteId());
            return;
        }

        if (vote.hasAiInsight()) {
            log.info("Vote already has AI insight: {}", event.voteId());
            return;
        }

        try {
            VoteInsightRequest request = buildRequest(vote);
            Optional<AiInsightResult> result = aiInsightUseCase.generateVoteInsight(request);

            result.ifPresentOrElse(
                    insight -> {
                        vote.cacheAiInsight(insight.headline(), insight.body());
                        voteRepository.save(vote);
                        log.info("AI insight generated for vote: {}", event.voteId());
                    },
                    () -> log.warn("Failed to generate AI insight for vote: {}", event.voteId())
            );
        } catch (Exception e) {
            log.error("Error generating AI insight for vote: {}", event.voteId(), e);
        }
    }

    private VoteInsightRequest buildRequest(Vote vote) {
        List<VoteOption> options = voteOptionRepository.findByVoteIdOrderByPosition(vote.getId());
        long total = voteParticipationRepository.countByVoteId(vote.getId());

        VoteOption optionA = options.stream().filter(o -> o.getPosition() == 0).findFirst().orElse(null);
        VoteOption optionB = options.stream().filter(o -> o.getPosition() == 1).findFirst().orElse(null);

        long optionACount = optionA != null ? voteParticipationRepository.countByVoteIdAndOptionId(vote.getId(), optionA.getId()) : 0;
        long optionBCount = optionB != null ? voteParticipationRepository.countByVoteIdAndOptionId(vote.getId(), optionB.getId()) : 0;

        int optionARatio = total == 0 ? 0 : (int) Math.round(optionACount * 100.0 / total);
        int optionBRatio = total == 0 ? 0 : 100 - optionARatio;

        // Gender distribution
        List<GenderCount> genderCounts = voteParticipationRepository.findGenderDistributionByVote(vote.getId());
        long femaleCount = genderCounts.stream()
                .filter(gc -> Gender.FEMALE == gc.gender())
                .mapToLong(GenderCount::count).sum();
        int femaleRatio = total == 0 ? 0 : (int) Math.round(femaleCount * 100.0 / total);
        int maleRatio = 100 - femaleRatio;

        // Majority age group
        String majorityAgeGroup = findMajorityAgeGroup(vote.getId());

        return new VoteInsightRequest(
                vote.getTitle(),
                optionA != null ? optionA.getLabel() : "선택지 A",
                optionACount,
                optionARatio,
                optionB != null ? optionB.getLabel() : "선택지 B",
                optionBCount,
                optionBRatio,
                total,
                femaleRatio,
                maleRatio,
                majorityAgeGroup
        );
    }

    private String findMajorityAgeGroup(Long voteId) {
        List<Long> userIds = voteParticipationRepository.findAllUserIdsByVoteId(voteId);
        List<User> users = userRepository.findAllById(userIds);

        Map<AgeGroup, Long> groupCounts = users.stream()
                .filter(u -> u.getBirthYear() != null)
                .collect(Collectors.groupingBy(
                        u -> AgeGroup.fromBirthYear(u.getBirthYear(), clock),
                        Collectors.counting()));

        return groupCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().getLabel())
                .orElse("알 수 없음");
    }
}
