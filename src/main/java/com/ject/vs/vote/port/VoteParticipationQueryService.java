package com.ject.vs.vote.port;

import com.ject.vs.vote.domain.VoteParticipationRepository;
import com.ject.vs.vote.port.in.VoteParticipationQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteParticipationQueryService implements VoteParticipationQueryUseCase {

    private final VoteParticipationRepository voteParticipationRepository;

    @Override
    public List<Long> findAllVoteIdsByUserId(Long userId) {
        return voteParticipationRepository.findAllByUserId(userId)
                .stream()
                .map(p -> p.getVoteId())
                .toList();
    }

    @Override
    public long getParticipantCountByVoteId(Long voteId) {
        return voteParticipationRepository.countByVoteId(voteId);
    }

    @Override
    public List<Long> findAllUserIdsByVoteId(Long voteId) {
        return voteParticipationRepository.findAllByVoteId(voteId)
                .stream()
                .map(p -> p.getUserId())
                .toList();
    }
}
