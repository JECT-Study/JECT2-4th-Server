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
    public List<Long> findAllVoteIdsByUserId(Long userId) {
        return voteParticipationRepository.findAllByUserId(userId)
                .stream()
                .map(p -> p.getVoteId())
                .toList();
    }

    @Override
    public long countParticipantsByVoteId(Long voteId) {
        return voteParticipationRepository.countByVoteId(voteId);
    }

    @Override
    public List<Long> findAllUserIdsByVoteId(Long voteId) {
        return voteParticipationRepository.findAllByVoteId(voteId)
                .stream()
                .map(p -> p.getUserId())
                .toList();
    }

    @Override
    public List<Long> findAllVoteIdsByStatus(List<Long> voteIds, VoteStatus status) {
        // TODO: Vote 도메인 연동 후 실제 status 기반 필터링으로 교체
        return voteIds;
    }
}
