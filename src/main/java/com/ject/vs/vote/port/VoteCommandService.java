package com.ject.vs.vote.port;

import com.ject.vs.vote.domain.*;
import com.ject.vs.vote.exception.InvalidOptionException;
import com.ject.vs.vote.exception.VoteEndedException;
import com.ject.vs.vote.exception.VoteNotFoundException;
import com.ject.vs.vote.port.in.VoteCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class VoteCommandService implements VoteCommandUseCase {

    private final VoteRepository voteRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteParticipationRepository voteParticipationRepository;
    private final GuestFreeVoteService guestFreeVoteService;
    private final Clock clock;

    @Override
    public VoteCreateResult create(VoteCreateCommand cmd) {
        Vote vote = Vote.create(
                cmd.type(), cmd.title(), cmd.content(),
                cmd.thumbnailUrl(), cmd.imageUrl(),
                cmd.duration().getValue(),
                clock
        );
        Vote saved = voteRepository.save(vote);
        voteOptionRepository.save(VoteOption.of(saved, cmd.optionA(), 0));
        voteOptionRepository.save(VoteOption.of(saved, cmd.optionB(), 1));
        return VoteCreateResult.from(saved, clock);
    }

    @Override
    public ParticipateResult participateAsMember(Long voteId, Long userId, Long optionId) {
        loadOngoingVote(voteId);
        validateOption(voteId, optionId);

        Optional<VoteParticipation> existing =
                voteParticipationRepository.findByVoteIdAndUserId(voteId, userId);

        if (existing.isPresent()) {
            existing.get().changeOption(optionId);
        } else {
            voteParticipationRepository.save(VoteParticipation.ofMember(voteId, userId, optionId));
        }
        return buildResult(voteId, optionId, null);
    }

    @Override
    public ParticipateResult participateAsGuest(Long voteId, String anonymousId, Long optionId) {
        loadOngoingVote(voteId);
        validateOption(voteId, optionId);

        Optional<VoteParticipation> existing =
                voteParticipationRepository.findByVoteIdAndAnonymousId(voteId, anonymousId);

        if (existing.isPresent()) {
            existing.get().changeOption(optionId);
        } else {
            guestFreeVoteService.consume(anonymousId);
            voteParticipationRepository.save(VoteParticipation.ofGuest(voteId, anonymousId, optionId));
        }
        return buildResult(voteId, optionId, guestFreeVoteService.remaining(anonymousId));
    }

    @Override
    public void cancel(Long voteId, Long userId) {
        loadOngoingVote(voteId);
        voteParticipationRepository.deleteByVoteIdAndUserId(voteId, userId);
    }

    private Vote loadOngoingVote(Long voteId) {
        Vote vote = voteRepository.findById(voteId).orElseThrow(VoteNotFoundException::new);
        if (vote.isEnded(clock)) throw new VoteEndedException();
        return vote;
    }

    private void validateOption(Long voteId, Long optionId) {
        if (!voteOptionRepository.existsByIdAndVoteId(optionId, voteId)) {
            throw new InvalidOptionException();
        }
    }

    private ParticipateResult buildResult(Long voteId, Long selectedOptionId, Integer remaining) {
        List<VoteOption> options = voteOptionRepository.findByVoteIdOrderByPosition(voteId);
        long total = voteParticipationRepository.countByVoteId(voteId);

        List<OptionResult> optionResults = options.stream().map(opt -> {
            long count = voteParticipationRepository.countByVoteIdAndOptionId(voteId, opt.getId());
            int ratio = total == 0 ? 0 : (int) Math.round(count * 100.0 / total);
            return new OptionResult(opt.getId(), opt.getLabel(), count, ratio);
        }).toList();

        return new ParticipateResult(voteId, selectedOptionId, optionResults, (int) total, remaining);
    }
}
