package com.ject.vs.ai.port;

import com.ject.vs.ai.port.in.AiInsightUseCase.PersonalizedVoteInsightRequest;
import com.ject.vs.user.domain.Gender;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.domain.UserRepository;
import com.ject.vs.vote.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PersonalizedInsightDataCollector {

    private final VoteRepository voteRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteParticipationRepository voteParticipationRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public PersonalizedVoteInsightRequest collect(Long voteId, Long userId, Long selectedOptionId) {
        Vote vote = voteRepository.findById(voteId).orElseThrow();
        List<VoteOption> options = voteOptionRepository.findByVoteIdOrderByPosition(voteId);

        VoteOption optionA = options.stream().filter(o -> o.getPosition() == 0).findFirst().orElse(null);
        VoteOption optionB = options.stream().filter(o -> o.getPosition() == 1).findFirst().orElse(null);

        long total = voteParticipationRepository.countByVoteId(voteId);

        long optionACount = optionA != null
                ? voteParticipationRepository.countByVoteIdAndOptionId(voteId, optionA.getId()) : 0;
        long optionBCount = optionB != null
                ? voteParticipationRepository.countByVoteIdAndOptionId(voteId, optionB.getId()) : 0;

        int optionARatio = total == 0 ? 0 : (int) Math.round(optionACount * 100.0 / total);
        int optionBRatio = total == 0 ? 0 : 100 - optionARatio;

        List<GenderCount> genderCounts = voteParticipationRepository.findGenderDistributionByVote(voteId);
        long femaleCount = genderCounts.stream()
                .filter(gc -> Gender.FEMALE == gc.gender())
                .mapToLong(GenderCount::count).sum();
        int femaleRatio = total == 0 ? 0 : (int) Math.round(femaleCount * 100.0 / total);
        int maleRatio = 100 - femaleRatio;

        String majorityAgeGroup = findMajorityAgeGroup(voteId);

        User user = userRepository.findById(userId).orElse(null);
        String userGender = user != null && user.getGender() != null ? user.getGender().name() : null;
        AgeGroup userAgeGroup = user != null && user.getBirthYear() != null
                ? AgeGroup.fromBirthYear(user.getBirthYear(), clock) : null;

        String userSelectedOptionLabel = findOptionLabel(options, selectedOptionId);

        int sameGenderRatio = 0;
        String sameGenderMajorityOption = null;
        if (user != null && user.getGender() != null) {
            sameGenderRatio = calculateSameGenderRatio(voteId, selectedOptionId, user.getGender());
            sameGenderMajorityOption = findMajorityOptionByGender(voteId, user.getGender(), options);
        }

        int sameAgeGroupRatio = 0;
        String sameAgeGroupMajorityOption = null;
        if (userAgeGroup != null) {
            sameAgeGroupRatio = calculateSameAgeGroupRatio(voteId, selectedOptionId, userAgeGroup);
            sameAgeGroupMajorityOption = findMajorityOptionByAgeGroup(voteId, userAgeGroup, options);
        }

        return new PersonalizedVoteInsightRequest(
                vote.getTitle(),
                optionA != null ? optionA.getLabel() : "A",
                optionACount,
                optionARatio,
                optionB != null ? optionB.getLabel() : "B",
                optionBCount,
                optionBRatio,
                total,
                femaleRatio,
                maleRatio,
                majorityAgeGroup,
                userSelectedOptionLabel,
                userGender,
                userAgeGroup != null ? userAgeGroup.getLabel() : null,
                sameGenderRatio,
                sameAgeGroupRatio,
                sameGenderMajorityOption,
                sameAgeGroupMajorityOption
        );
    }

    private String findOptionLabel(List<VoteOption> options, Long optionId) {
        return options.stream()
                .filter(o -> o.getId().equals(optionId))
                .findFirst()
                .map(VoteOption::getLabel)
                .orElse(null);
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
                .orElse(null);
    }

    private int calculateSameGenderRatio(Long voteId, Long selectedOptionId, Gender gender) {
        long sameGenderSelectedOption = voteParticipationRepository
                .countByVoteIdAndOptionIdAndGender(voteId, selectedOptionId, gender);
        long totalSameGender = voteParticipationRepository.countByVoteIdAndGender(voteId, gender);

        return totalSameGender == 0 ? 0 : (int) Math.round(sameGenderSelectedOption * 100.0 / totalSameGender);
    }

    private String findMajorityOptionByGender(Long voteId, Gender gender, List<VoteOption> options) {
        List<Object[]> optionCounts = voteParticipationRepository.findOptionCountsByVoteIdAndGender(voteId, gender);
        if (optionCounts.isEmpty()) {
            return null;
        }

        Long majorityOptionId = (Long) optionCounts.get(0)[0];
        return findOptionLabel(options, majorityOptionId);
    }

    private int calculateSameAgeGroupRatio(Long voteId, Long selectedOptionId, AgeGroup ageGroup) {
        List<Long> userIds = voteParticipationRepository.findUserIdsByVoteIdAndOptionId(voteId, selectedOptionId);
        List<User> users = userRepository.findAllById(userIds);

        long sameAgeGroupCount = users.stream()
                .filter(u -> u.getBirthYear() != null)
                .filter(u -> AgeGroup.fromBirthYear(u.getBirthYear(), clock) == ageGroup)
                .count();

        List<Long> allUserIds = voteParticipationRepository.findAllUserIdsByVoteId(voteId);
        List<User> allUsers = userRepository.findAllById(allUserIds);

        long totalSameAgeGroup = allUsers.stream()
                .filter(u -> u.getBirthYear() != null)
                .filter(u -> AgeGroup.fromBirthYear(u.getBirthYear(), clock) == ageGroup)
                .count();

        return totalSameAgeGroup == 0 ? 0 : (int) Math.round(sameAgeGroupCount * 100.0 / totalSameAgeGroup);
    }

    private String findMajorityOptionByAgeGroup(Long voteId, AgeGroup ageGroup, List<VoteOption> options) {
        List<Long> allUserIds = voteParticipationRepository.findAllUserIdsByVoteId(voteId);
        List<User> allUsers = userRepository.findAllById(allUserIds);

        Map<Long, List<User>> usersByOption = allUsers.stream()
                .filter(u -> u.getBirthYear() != null)
                .filter(u -> AgeGroup.fromBirthYear(u.getBirthYear(), clock) == ageGroup)
                .collect(Collectors.groupingBy(u -> {
                    return voteParticipationRepository.findByVoteIdAndUserId(voteId, u.getId())
                            .map(VoteParticipation::getOptionId)
                            .orElse(null);
                }));

        return usersByOption.entrySet().stream()
                .filter(e -> e.getKey() != null)
                .max(Map.Entry.comparingByValue((a, b) -> Integer.compare(a.size(), b.size())))
                .map(e -> findOptionLabel(options, e.getKey()))
                .orElse(null);
    }
}
