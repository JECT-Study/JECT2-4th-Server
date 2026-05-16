package com.ject.vs.vote.port;

import com.ject.vs.vote.domain.*;
import com.ject.vs.vote.exception.VoteNotFoundException;
import com.ject.vs.vote.port.in.VoteParticipationQueryUseCase;
import com.ject.vs.vote.port.in.VoteQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteQueryService implements VoteQueryUseCase, VoteParticipationQueryUseCase {

    private final VoteRepository voteRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteParticipationRepository voteParticipationRepository;
    private final Clock clock;

    @Override
    public boolean isParticipated(Long voteId, Long userId) {
        return voteParticipationRepository.existsByVoteIdAndUserId(voteId, userId);
    }

    @Override
    public Optional<Long> getSelectedOptionId(Long voteId, Long userId) {
        return voteParticipationRepository.findByVoteIdAndUserId(voteId, userId)
                .map(VoteParticipation::getOptionId);
    }

    @Override
    public VoteSummary getVoteSummary(Long voteId) {
        Vote vote = voteRepository.findById(voteId).orElseThrow(VoteNotFoundException::new);
        return new VoteSummary(vote.getId(), vote.getTitle(), vote.getStatus(clock), vote.getEndAt());
    }

    @Override
    public VoteChatSummary getVoteChatSummary(Long voteId) {
        Vote vote = voteRepository.findById(voteId).orElseThrow(VoteNotFoundException::new);

        return new VoteChatSummary(
                vote.getId(),
                vote.getTitle(),
                vote.getThumbnailUrl(),
                vote.getStatus(clock),
                vote.getEndAt(),
                vote.getOptionA().getLabel(),
                vote.getOptionB().getLabel()
        );
    }

    @Override
    public VoteRatio getRatio(Long voteId) {
        List<VoteOption> options = voteOptionRepository.findByVoteIdOrderByPosition(voteId);
        if (options.size() != 2) throw new IllegalStateException("Vote must have exactly 2 options");
        long total = voteParticipationRepository.countByVoteId(voteId);
        long aCount = voteParticipationRepository.countByVoteIdAndOptionId(voteId, options.get(0).getId());
        int aRatio = total == 0 ? 0 : (int) Math.round(aCount * 100.0 / total);
        return new VoteRatio(aRatio, 100 - aRatio, (int) total);
    }

    @Override
    public VoteOption getSelectedOption(Long voteId, Long userId) {
        Optional<Long> selectedOptionId = getSelectedOptionId(voteId, userId);

        Vote vote = voteRepository.findById(voteId).orElseThrow(VoteNotFoundException::new);
        return vote.getOption(selectedOptionId.get());
    }

    @Override
    public int getParticipantCount(Long voteId) {
        return (int) voteParticipationRepository.countByVoteId(voteId);
    }

    @Override
    public List<Long> findAllVoteIdsByStatus(List<Long> voteIds, VoteStatus status) {
        if (voteIds.isEmpty()) return List.of();
        return voteRepository.findAllByIdIn(voteIds).stream()
                .filter(vote -> {
                    boolean ongoing = vote.isOngoing(clock);
                    return status == VoteStatus.ONGOING ? ongoing : !ongoing;
                })
                .map(Vote::getId)
                .toList();
    }

    // VoteParticipationQueryUseCase 구현 (채팅 도메인 호환)

    @Override
    public boolean isParticipant(Long voteId, Long userId) {
        return isParticipated(voteId, userId);
    }

    @Override
    public List<Long> findAllVoteIdsByUserId(Long userId) {
        return voteParticipationRepository.findAllVoteIdsByUserId(userId);
    }

    @Override
    public long countParticipantsByVoteId(Long voteId) {
        return voteParticipationRepository.countByVoteId(voteId);
    }

    @Override
    public List<Long> findAllUserIdsByVoteId(Long voteId) {
        return voteParticipationRepository.findAllUserIdsByVoteId(voteId);
    }
}
