package com.ject.vs.vote.port;

import com.ject.vs.vote.domain.VoteParticipationRepository;
import com.ject.vs.vote.port.in.VoteParticipationQueryUseCase;
import com.ject.vs.vote.port.in.VoteQueryUseCase;
import com.ject.vs.vote.port.in.dto.VoteStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteService implements VoteParticipationQueryUseCase, VoteQueryUseCase {

    private final VoteParticipationRepository voteParticipationRepository;

    @Override
    public boolean isParticipant(Long voteId, Long userId) {
        return voteParticipationRepository.existsByVoteIdAndUserId(voteId, userId);
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

    @Override
    public List<Long> findAllVoteIdsByStatus(List<Long> voteIds, VoteStatus status) {
        // TODO: Vote 도메인 연동 후 실제 status 기반 필터링으로 교체
        return voteIds;
    }
}
