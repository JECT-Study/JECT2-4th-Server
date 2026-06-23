package com.ject.vs.user.port;

import com.ject.vs.user.adapter.web.dto.UserProfileBottomSheetResponse;
import com.ject.vs.user.adapter.web.dto.UserProfileBottomSheetResponse.ParticipatedVoteCard;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.port.in.UserProfileQueryUseCase;
import com.ject.vs.user.port.in.UserQueryUseCase;
import com.ject.vs.vote.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileQueryService implements UserProfileQueryUseCase {

    private static final int RECENT_VOTE_LIMIT = 3;

    private final UserQueryUseCase userQueryUseCase;
    private final VoteParticipationRepository voteParticipationRepository;
    private final VoteRepository voteRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final Clock clock;

    @Override
    public UserProfileBottomSheetResponse getProfileBottomSheet(Long targetUserId, Long viewerUserId) {
        User user = userQueryUseCase.getUser(targetUserId);

        long participatedVoteCount = voteParticipationRepository.countByUserId(targetUserId);
        List<Long> recentVoteIds = voteParticipationRepository
                .findTopVoteIdsByRecentActivity(targetUserId, RECENT_VOTE_LIMIT);

        List<ParticipatedVoteCard> recentVotes = buildRecentVoteCards(
                targetUserId, viewerUserId, recentVoteIds);

        return new UserProfileBottomSheetResponse(
                user.getId(),
                user.getNickname(),
                user.getImageColor(),
                participatedVoteCount,
                recentVotes
        );
    }

    private List<ParticipatedVoteCard> buildRecentVoteCards(
            Long targetUserId, Long viewerUserId, List<Long> voteIds) {
        if (voteIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Vote> votesById = voteRepository.findAllByIdIn(voteIds).stream()
                .collect(Collectors.toMap(Vote::getId, Function.identity()));

        Map<Long, VoteParticipation> participationsByVoteId = voteIds.stream()
                .map(voteId -> voteParticipationRepository.findByVoteIdAndUserId(voteId, targetUserId))
                .flatMap(java.util.Optional::stream)
                .collect(Collectors.toMap(VoteParticipation::getVoteId, Function.identity()));

        List<Long> optionIds = participationsByVoteId.values().stream()
                .map(VoteParticipation::getOptionId)
                .toList();
        Map<Long, VoteOption> optionsById = voteOptionRepository.findAllById(optionIds).stream()
                .collect(Collectors.toMap(VoteOption::getId, Function.identity()));

        return voteIds.stream()
                .map(voteId -> toCard(voteId, votesById, participationsByVoteId, optionsById, viewerUserId))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private ParticipatedVoteCard toCard(
            Long voteId,
            Map<Long, Vote> votesById,
            Map<Long, VoteParticipation> participationsByVoteId,
            Map<Long, VoteOption> optionsById,
            Long viewerUserId) {
        Vote vote = votesById.get(voteId);
        if (vote == null) {
            return null;
        }

        VoteParticipation participation = participationsByVoteId.get(voteId);
        VoteOption selectedOption = optionsById.get(participation.getOptionId());
        String selectedOptionLabel = selectedOption.getLabel();

        return new ParticipatedVoteCard(
                voteId,
                vote.getTitle(),
                vote.getStatus(clock),
                selectedOptionLabel,
                voteParticipationRepository.existsByVoteIdAndUserId(voteId, viewerUserId)
        );
    }
}