package com.ject.vs.vote.port;

import com.ject.vs.user.domain.Gender;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.domain.UserRepository;
import com.ject.vs.vote.domain.*;
import com.ject.vs.vote.exception.VoteNotFoundException;
import com.ject.vs.vote.exception.VoteNotEndedException;
import com.ject.vs.vote.port.in.VoteCommandUseCase.OptionResult;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteResultQueryService implements VoteResultQueryUseCase {

    private final VoteRepository voteRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteParticipationRepository voteParticipationRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Override
    public VoteResultDetail getResult(Long voteId, Long userId) {
        Vote vote = voteRepository.findById(voteId).orElseThrow(VoteNotFoundException::new);
        if (vote.isOngoing(clock)) throw new VoteNotEndedException();

        List<VoteOption> options = voteOptionRepository.findByVoteIdOrderByPosition(voteId);
        long total = voteParticipationRepository.countByVoteId(voteId);

        List<OptionResult> optionResults = options.stream().map(opt -> {
            long count = voteParticipationRepository.countByVoteIdAndOptionId(voteId, opt.getId());
            int ratio = total == 0 ? 0 : (int) Math.round(count * 100.0 / total);
            return new OptionResult(opt.getId(), opt.getLabel(), count, ratio);
        }).toList();

        if (userId == null) {
            // 비회원: 분석 인사이트(selectionCount)는 보여주고, 성별/연령대는 잠금
            Insight guestInsight = new Insight(true, InsightScope.TOTAL, (int) total, null, null);
            return new VoteResultDetail(voteId, vote.getTitle(), vote.getCreatedAt(),
                    vote.getContent(), vote.getThumbnailUrl(), VoteStatus.ENDED,
                    vote.getEndAt(), (int) total, optionResults, false, null,
                    guestInsight, AiInsightView.unavailable());
        }

        Optional<VoteParticipation> myParticipation =
                voteParticipationRepository.findByVoteIdAndUserId(voteId, userId);

        Long mySelectedOptionId = myParticipation.map(VoteParticipation::getOptionId).orElse(null);

        Insight insight;
        AiInsightView aiInsight;

        if (myParticipation.isPresent()) {
            insight = buildMySelectionInsight(voteId, mySelectedOptionId, userId);
            aiInsight = vote.hasAiInsight()
                    ? AiInsightView.of(vote.getAiInsightHeadline(), vote.getAiInsightBody())
                    : AiInsightView.unavailable();
        } else {
            insight = buildTotalInsight(voteId, total, userId);
            aiInsight = AiInsightView.unavailable();
        }

        boolean voted = myParticipation.isPresent();
        return new VoteResultDetail(voteId, vote.getTitle(), vote.getCreatedAt(),
                vote.getContent(), vote.getThumbnailUrl(), VoteStatus.ENDED,
                vote.getEndAt(), (int) total, optionResults, voted, mySelectedOptionId, insight, aiInsight);
    }

    @Override
    public ShareLinkResult getShareLink(Long voteId) {
        Vote vote = voteRepository.findById(voteId).orElseThrow(VoteNotFoundException::new);
        return new ShareLinkResult(
                "https://vs.app/poll/result/" + voteId,
                vote.getTitle(),
                vote.getThumbnailUrl()
        );
    }

    private Insight buildMySelectionInsight(Long voteId, Long optionId, Long userId) {
        int selectionCount = (int) voteParticipationRepository.countByVoteIdAndOptionId(voteId, optionId);

        // 해당 선택 기준 성별 카운트
        List<GenderCount> selectionGenderCounts = voteParticipationRepository.findGenderDistribution(voteId, optionId);
        // 전체 투표 기준 성별 카운트 (비율 계산용)
        List<GenderCount> totalGenderCounts = voteParticipationRepository.findGenderDistributionByVote(voteId);
        // 사용자 성별
        String myGender = resolveMyGender(userId);

        GenderDistribution genderDistribution = computeGenderDistribution(
                selectionCount, selectionGenderCounts, totalGenderCounts, myGender);

        AgeGroup myGroup = resolveMyAgeGroup(userId);
        List<AgeDistribution> ageDistribution = computeAgeDistributionByOption(voteId, optionId, myGroup);

        return new Insight(false, InsightScope.MY_SELECTION, selectionCount, genderDistribution, ageDistribution);
    }

    private Insight buildTotalInsight(Long voteId, long total, Long userId) {
        List<GenderCount> genderCounts = voteParticipationRepository.findGenderDistributionByVote(voteId);
        // 미참여자: 다수 성별 강조
        String majorityGender = resolveMajorityGender(genderCounts);
        GenderDistribution genderDistribution = computeGenderDistributionForTotal((int) total, genderCounts, majorityGender);

        // 미참여자: 다수 연령대 강조
        List<AgeDistribution> ageDistribution = computeAgeDistributionByVoteWithMajority(voteId);

        return new Insight(false, InsightScope.TOTAL, (int) total, genderDistribution, ageDistribution);
    }

    private AgeGroup resolveMyAgeGroup(Long userId) {
        return userRepository.findById(userId)
                .map(u -> u.getBirthYear() != null ? AgeGroup.fromBirthYear(u.getBirthYear(), clock) : null)
                .orElse(null);
    }

    private String resolveMyGender(Long userId) {
        return userRepository.findById(userId)
                .map(u -> u.getGender() != null ? u.getGender().name() : null)
                .orElse(null);
    }

    private String resolveMajorityGender(List<GenderCount> genderCounts) {
        long femaleCount = genderCounts.stream()
                .filter(gc -> Gender.FEMALE == gc.gender())
                .mapToLong(GenderCount::count).sum();
        long maleCount = genderCounts.stream()
                .filter(gc -> Gender.MALE == gc.gender())
                .mapToLong(GenderCount::count).sum();

        if (femaleCount == 0 && maleCount == 0) return null;
        return femaleCount >= maleCount ? Gender.FEMALE.name() : Gender.MALE.name();
    }

    /**
     * 나의 선택 기준 성별 분포 계산
     * @param selectionCount 해당 선택에 참여한 전체 인원
     * @param selectionGenderCounts 해당 선택 기준 성별 카운트
     * @param totalGenderCounts 전체 투표 기준 성별 카운트 (비율 계산용)
     * @param myGender 사용자 성별
     */
    private GenderDistribution computeGenderDistribution(
            int selectionCount,
            List<GenderCount> selectionGenderCounts,
            List<GenderCount> totalGenderCounts,
            String myGender) {

        // 해당 선택 기준 성별별 인원 수
        long femaleCountInSelection = selectionGenderCounts.stream()
                .filter(gc -> Gender.FEMALE == gc.gender())
                .mapToLong(GenderCount::count).sum();
        long maleCountInSelection = selectionGenderCounts.stream()
                .filter(gc -> Gender.MALE == gc.gender())
                .mapToLong(GenderCount::count).sum();

        // 전체 투표 기준 성별 비율
        long totalInVote = totalGenderCounts.stream().mapToLong(GenderCount::count).sum();
        long femaleInVote = totalGenderCounts.stream()
                .filter(gc -> Gender.FEMALE == gc.gender())
                .mapToLong(GenderCount::count).sum();
        long maleInVote = totalInVote - femaleInVote;

        int femaleRatioInVote = totalInVote == 0 ? 0 : (int) Math.round(femaleInVote * 100.0 / totalInVote);
        int maleRatioInVote = totalInVote == 0 ? 0 : 100 - femaleRatioInVote;

        return new GenderDistribution(
                selectionCount,
                femaleCountInSelection,
                femaleRatioInVote,
                maleCountInSelection,
                maleRatioInVote,
                myGender
        );
    }

    /**
     * 전체 투표 기준 성별 분포 계산 (투표 미참여자용)
     */
    private GenderDistribution computeGenderDistributionForTotal(
            int total,
            List<GenderCount> totalGenderCounts,
            String myGender) {

        long femaleCount = totalGenderCounts.stream()
                .filter(gc -> Gender.FEMALE == gc.gender())
                .mapToLong(GenderCount::count).sum();
        long maleCount = totalGenderCounts.stream()
                .filter(gc -> Gender.MALE == gc.gender())
                .mapToLong(GenderCount::count).sum();

        int femaleRatio = total == 0 ? 0 : (int) Math.round(femaleCount * 100.0 / total);
        int maleRatio = total == 0 ? 0 : 100 - femaleRatio;

        return new GenderDistribution(
                total,
                femaleCount,
                femaleRatio,
                maleCount,
                maleRatio,
                myGender
        );
    }

    private List<AgeDistribution> computeAgeDistributionByOption(Long voteId, Long optionId, AgeGroup myGroup) {
        List<Long> userIds = voteParticipationRepository.findUserIdsByVoteIdAndOptionId(voteId, optionId);
        return buildAgeDistributions(userIds, myGroup);
    }

    private List<AgeDistribution> computeAgeDistributionByVote(Long voteId, AgeGroup myGroup) {
        List<Long> userIds = voteParticipationRepository.findAllUserIdsByVoteId(voteId);
        return buildAgeDistributions(userIds, myGroup);
    }

    private List<AgeDistribution> computeAgeDistributionByVoteWithMajority(Long voteId) {
        List<Long> userIds = voteParticipationRepository.findAllUserIdsByVoteId(voteId);
        return buildAgeDistributionsWithMajority(userIds);
    }

    // 10대 → 20대, 50대+ → 40대로 병합 (UI 표시 그룹: 20s, 30s, 40s)
    private static final List<AgeGroup> DISPLAY_AGE_GROUPS = List.of(
            AgeGroup.TWENTIES, AgeGroup.THIRTIES, AgeGroup.FORTIES);

    private AgeGroup normalizeAgeGroup(AgeGroup group) {
        if (group == AgeGroup.TEENS) return AgeGroup.TWENTIES;
        if (group == AgeGroup.FIFTIES_PLUS) return AgeGroup.FORTIES;
        return group;
    }

    private List<AgeDistribution> buildAgeDistributions(List<Long> userIds, AgeGroup myGroup) {
        List<User> users = userRepository.findAllById(userIds);

        Map<AgeGroup, Long> groupCounts = users.stream()
                .filter(u -> u.getBirthYear() != null)
                .collect(Collectors.groupingBy(
                        u -> normalizeAgeGroup(AgeGroup.fromBirthYear(u.getBirthYear(), clock)),
                        Collectors.counting()));

        long total = groupCounts.values().stream().mapToLong(Long::longValue).sum();
        AgeGroup myDisplayGroup = myGroup != null ? normalizeAgeGroup(myGroup) : null;

        return DISPLAY_AGE_GROUPS.stream()
                .map(group -> {
                    long count = groupCounts.getOrDefault(group, 0L);
                    int ratio = total == 0 ? 0 : (int) Math.round(count * 100.0 / total);
                    return new AgeDistribution(group.getLabel(), ratio, group == myDisplayGroup);
                })
                .toList();
    }

    private List<AgeDistribution> buildAgeDistributionsWithMajority(List<Long> userIds) {
        List<User> users = userRepository.findAllById(userIds);

        Map<AgeGroup, Long> groupCounts = users.stream()
                .filter(u -> u.getBirthYear() != null)
                .collect(Collectors.groupingBy(
                        u -> normalizeAgeGroup(AgeGroup.fromBirthYear(u.getBirthYear(), clock)),
                        Collectors.counting()));

        long total = groupCounts.values().stream().mapToLong(Long::longValue).sum();

        // 비참여자는 "내 그룹"이 없으므로 isHighlighted 항상 false
        return DISPLAY_AGE_GROUPS.stream()
                .map(group -> {
                    long count = groupCounts.getOrDefault(group, 0L);
                    int ratio = total == 0 ? 0 : (int) Math.round(count * 100.0 / total);
                    return new AgeDistribution(group.getLabel(), ratio, false);
                })
                .toList();
    }
}
