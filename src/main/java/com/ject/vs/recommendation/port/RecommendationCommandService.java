package com.ject.vs.recommendation.port;

import com.ject.vs.config.AdminProperties;
import com.ject.vs.recommendation.port.in.RecommendationCommandUseCase;
import com.ject.vs.vote.domain.RecommendedVote;
import com.ject.vs.vote.domain.RecommendedVoteRepository;
import com.ject.vs.vote.domain.Vote;
import com.ject.vs.vote.domain.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class RecommendationCommandService implements RecommendationCommandUseCase {

    private final AdminProperties adminProperties;
    private final RecommendedVoteRepository recommendedVoteRepository;
    private final VoteRepository voteRepository;

    @Override
    @Transactional
    public void setTodayRecommendations(Long adminUserId, List<Long> voteIds) {
        validateAdmin(adminUserId);
        validateVotes(voteIds);

        LocalDate today = LocalDate.now();

        // 기존 오늘 추천 삭제
        recommendedVoteRepository.deleteAllByRecommendedDate(today);

        // 새로운 추천 저장 (순서대로 displayOrder 부여)
        List<RecommendedVote> recommendations = IntStream.range(0, voteIds.size())
                .mapToObj(i -> RecommendedVote.create(voteIds.get(i), i + 1, today))
                .toList();

        recommendedVoteRepository.saveAll(recommendations);
    }

    private void validateAdmin(Long adminUserId) {
        if (adminProperties.userIds() == null || !adminProperties.userIds().contains(adminUserId)) {
            throw new IllegalArgumentException("추천 투표 설정 권한이 없습니다.");
        }
    }

    private void validateVotes(List<Long> voteIds) {
        if (voteIds == null || voteIds.isEmpty()) {
            throw new IllegalArgumentException("추천할 투표를 하나 이상 지정해야 합니다.");
        }

        // 중복 체크
        Set<Long> uniqueIds = Set.copyOf(voteIds);
        if (uniqueIds.size() != voteIds.size()) {
            throw new IllegalArgumentException("중복된 투표 ID가 있습니다.");
        }

        // 존재 여부 및 진행 중 여부 체크
        List<Vote> votes = voteRepository.findAllById(voteIds);
        if (votes.size() != voteIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 투표가 있습니다.");
        }

        // TODO: 진행 중인 투표만 허용하는 로직 추가 가능 (endAt > now)
    }
}
