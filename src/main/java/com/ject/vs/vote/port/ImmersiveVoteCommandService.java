package com.ject.vs.vote.port;

import com.ject.vs.vote.domain.*;
import com.ject.vs.vote.exception.InvalidOptionException;
import com.ject.vs.vote.exception.VoteEndedException;
import com.ject.vs.vote.exception.VoteNotFoundException;
import com.ject.vs.vote.port.in.ImmersiveVoteCommandUseCase;
import com.ject.vs.vote.port.in.VoteCommandUseCase.OptionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ImmersiveVoteCommandService implements ImmersiveVoteCommandUseCase {

    private final VoteRepository voteRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteParticipationRepository voteParticipationRepository;
    private final GuestFreeVoteService guestFreeVoteService;
    private final Clock clock;

    @Override
    public ImmersiveParticipateResult participateOrCancel(
            Long voteId, Long userId, String anonymousId, Long optionId) {

        Vote vote = voteRepository.findById(voteId).orElseThrow(VoteNotFoundException::new);
        if (vote.isEnded(clock)) throw new VoteEndedException();

        if (!voteOptionRepository.existsByIdAndVoteId(optionId, voteId)) {
            throw new InvalidOptionException();
        }

        Optional<VoteParticipation> existing = userId != null
                ? voteParticipationRepository.findByVoteIdAndUserId(voteId, userId)
                : voteParticipationRepository.findByVoteIdAndAnonymousId(voteId, anonymousId);

        // 같은 옵션 재클릭 → 취소
        if (existing.isPresent() && existing.get().getOptionId().equals(optionId)) {
            voteParticipationRepository.delete(existing.get());
            return buildResult(voteId, ImmersiveVoteAction.CANCELED, null, remaining(userId, anonymousId));
        }

        // 옵션 변경
        if (existing.isPresent()) {
            existing.get().changeOption(optionId);
            return buildResult(voteId, ImmersiveVoteAction.VOTED, optionId, remaining(userId, anonymousId));
        }

        // 신규 참여
        if (userId != null) {
            voteParticipationRepository.save(VoteParticipation.ofMember(voteId, userId, optionId));
        } else {
            guestFreeVoteService.consume(anonymousId);
            voteParticipationRepository.save(VoteParticipation.ofGuest(voteId, anonymousId, optionId));
        }
        return buildResult(voteId, ImmersiveVoteAction.VOTED, optionId, remaining(userId, anonymousId));
    }

    private ImmersiveParticipateResult buildResult(Long voteId, ImmersiveVoteAction action,
                                                    Long selectedOptionId, Integer remainingFreeVotes) {
        List<VoteOption> options = voteOptionRepository.findByVoteIdOrderByPosition(voteId);
        long total = voteParticipationRepository.countByVoteId(voteId);

        List<OptionResult> optionResults = options.stream().map(opt -> {
            long count = voteParticipationRepository.countByVoteIdAndOptionId(voteId, opt.getId());
            int ratio = total == 0 ? 0 : (int) Math.round(count * 100.0 / total);
            return new OptionResult(opt.getId(), opt.getLabel(), count, ratio);
        }).toList();

        return new ImmersiveParticipateResult(voteId, action, selectedOptionId, optionResults, remainingFreeVotes);
    }

    private Integer remaining(Long userId, String anonymousId) {
        if (userId != null) return null;
        return guestFreeVoteService.remaining(anonymousId);
    }
}
