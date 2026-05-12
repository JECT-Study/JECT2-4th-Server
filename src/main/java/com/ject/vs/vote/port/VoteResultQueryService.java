package com.ject.vs.vote.port;

import com.ject.vs.domain.Gender;
import com.ject.vs.domain.User;
import com.ject.vs.repository.UserRepository;
import com.ject.vs.vote.domain.*;
import com.ject.vs.vote.exception.VoteNotFoundException;
import com.ject.vs.vote.exception.VoteNotEndedException;
import com.ject.vs.vote.port.in.VoteCommandUseCase.OptionResult;
import com.ject.vs.vote.port.in.VoteResultQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Arrays;
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
            return new VoteResultDetail(voteId, vote.getTitle(), VoteStatus.ENDED,
                    vote.getEndAt(), (int) total, optionResults, null,
                    Insight.ofLocked(), AiInsightView.unavailable());
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

        return new VoteResultDetail(voteId, vote.getTitle(), VoteStatus.ENDED,
                vote.getEndAt(), (int) total, optionResults, mySelectedOptionId, insight, aiInsight);
    }

    @Override
    public ShareLinkResult getShareLink(Long voteId) {
        if (!voteRepository.existsById(voteId)) throw new VoteNotFoundException();
        return new ShareLinkResult("https://vs.app/poll/result/" + voteId);
    }

    private Insight buildMySelectionInsight(Long voteId, Long optionId, Long userId) {
        int selectionCount = (int) voteParticipationRepository.countByVoteIdAndOptionId(voteId, optionId);

        List<GenderCount> genderCounts = voteParticipationRepository.findGenderDistribution(voteId, optionId);
        GenderDistribution genderDistribution = computeGenderDistribution(genderCounts);

        AgeGroup myGroup = resolveMyAgeGroup(userId);
        List<AgeDistribution> ageDistribution = computeAgeDistributionByOption(voteId, optionId, myGroup);

        return new Insight(false, InsightScope.MY_SELECTION, selectionCount, genderDistribution, ageDistribution);
    }

    private Insight buildTotalInsight(Long voteId, long total, Long userId) {
        List<GenderCount> genderCounts = voteParticipationRepository.findGenderDistributionByVote(voteId);
        GenderDistribution genderDistribution = computeGenderDistribution(genderCounts);

        AgeGroup myGroup = resolveMyAgeGroup(userId);
        List<AgeDistribution> ageDistribution = computeAgeDistributionByVote(voteId, myGroup);

        return new Insight(false, InsightScope.TOTAL, (int) total, genderDistribution, ageDistribution);
    }

    private AgeGroup resolveMyAgeGroup(Long userId) {
        return userRepository.findById(userId)
                .map(u -> u.getBirthYear() != null ? AgeGroup.fromBirthYear(u.getBirthYear(), clock) : null)
                .orElse(null);
    }

    private GenderDistribution computeGenderDistribution(List<GenderCount> genderCounts) {
        long totalGender = genderCounts.stream().mapToLong(GenderCount::count).sum();
        if (totalGender == 0) return new GenderDistribution(0, 0);

        long maleCount = genderCounts.stream()
                .filter(gc -> Gender.MALE == gc.gender()).mapToLong(GenderCount::count).sum();
        int maleRatio = (int) Math.round(maleCount * 100.0 / totalGender);
        return new GenderDistribution(maleRatio, 100 - maleRatio);
    }

    private List<AgeDistribution> computeAgeDistributionByOption(Long voteId, Long optionId, AgeGroup myGroup) {
        List<Long> userIds = voteParticipationRepository.findUserIdsByVoteIdAndOptionId(voteId, optionId);
        return buildAgeDistributions(userIds, myGroup);
    }

    private List<AgeDistribution> computeAgeDistributionByVote(Long voteId, AgeGroup myGroup) {
        List<Long> userIds = voteParticipationRepository.findAllUserIdsByVoteId(voteId);
        return buildAgeDistributions(userIds, myGroup);
    }

    private List<AgeDistribution> buildAgeDistributions(List<Long> userIds, AgeGroup myGroup) {
        List<User> users = userRepository.findAllById(userIds);

        Map<AgeGroup, Long> groupCounts = users.stream()
                .filter(u -> u.getBirthYear() != null)
                .collect(Collectors.groupingBy(
                        u -> AgeGroup.fromBirthYear(u.getBirthYear(), clock),
                        Collectors.counting()));

        long total = groupCounts.values().stream().mapToLong(Long::longValue).sum();

        return Arrays.stream(AgeGroup.values())
                .map(group -> {
                    long count = groupCounts.getOrDefault(group, 0L);
                    int ratio = total == 0 ? 0 : (int) Math.round(count * 100.0 / total);
                    return new AgeDistribution(group.getLabel(), ratio, group == myGroup);
                })
                .toList();
    }
}
