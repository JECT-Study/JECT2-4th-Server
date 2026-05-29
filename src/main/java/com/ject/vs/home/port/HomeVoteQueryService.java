package com.ject.vs.home.port;

import com.ject.vs.home.port.in.HomeVoteQueryUseCase;
import com.ject.vs.vote.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeVoteQueryService implements HomeVoteQueryUseCase {

    private final VoteRepository voteRepository;
    private final VoteParticipationRepository voteParticipationRepository;
    private final VoteStatisticsRepository voteStatisticsRepository;
    private final RecommendedVoteRepository recommendedVoteRepository;
    private final Clock clock;

    private static final double PARTICIPANT_WEIGHT = 0.7;
    private static final double VIEW_WEIGHT = 0.3;

    @Override
    public RecommendationResult getRecommendations() {
        LocalDate today = LocalDate.now(clock);
        Instant now = Instant.now(clock);

        List<RecommendedVote> recommendedVotes = recommendedVoteRepository
                .findByDateWithOngoingVotes(today, now);

        List<RecommendationItem> items = recommendedVotes.stream()
                .map(rv -> {
                    Vote vote = rv.getVote();
                    return new RecommendationItem(
                            vote.getId(),
                            vote.getThumbnailUrl(),
                            vote.getTitle(),
                            vote.getContent(),
                            vote.getEndAt()
                    );
                })
                .toList();

        return new RecommendationResult(items);
    }

    @Override
    public HotTopicResult getHotTopics() {
        Instant now = Instant.now(clock);

        // 진행 중인 투표만 조회
        List<Vote> ongoingVotes = voteRepository.findOngoingVotes(now);

        if (ongoingVotes.isEmpty()) {
            return new HotTopicResult(List.of());
        }

        List<Long> voteIds = ongoingVotes.stream().map(Vote::getId).toList();

        // 참여 수 조회
        Map<Long, Long> participantCounts = voteParticipationRepository
                .countByVoteIds(voteIds)
                .stream()
                .collect(Collectors.toMap(
                        VoteParticipationRepository.VoteParticipantCount::voteId,
                        VoteParticipationRepository.VoteParticipantCount::count
                ));

        // 조회 수 조회
        Map<Long, Long> viewCounts = voteStatisticsRepository
                .findAllByVoteIdIn(voteIds)
                .stream()
                .collect(Collectors.toMap(
                        VoteStatistics::getVoteId,
                        VoteStatistics::getViewCount
                ));

        // 인기 점수 계산 및 정렬
        List<Vote> top3 = ongoingVotes.stream()
                .sorted((v1, v2) -> {
                    double score1 = calculatePopularityScore(
                            participantCounts.getOrDefault(v1.getId(), 0L),
                            viewCounts.getOrDefault(v1.getId(), 0L)
                    );
                    double score2 = calculatePopularityScore(
                            participantCounts.getOrDefault(v2.getId(), 0L),
                            viewCounts.getOrDefault(v2.getId(), 0L)
                    );
                    // 동점인 경우 최신 투표 우선
                    if (score1 == score2) {
                        return v2.getId().compareTo(v1.getId());
                    }
                    return Double.compare(score2, score1);
                })
                .limit(3)
                .toList();

        List<HotTopicItem> items = new java.util.ArrayList<>();
        for (int i = 0; i < top3.size(); i++) {
            Vote vote = top3.get(i);
            items.add(new HotTopicItem(
                    i + 1,
                    vote.getId(),
                    vote.getThumbnailUrl(),
                    vote.getTitle(),
                    vote.getContent(),
                    participantCounts.getOrDefault(vote.getId(), 0L),
                    vote.getEndAt()
            ));
        }

        return new HotTopicResult(items);
    }

    @Override
    public VoteListResult getVoteList(String cursor, int size, VoteSortType sortType, boolean excludeEnded) {
        PageRequest pageable = PageRequest.of(0, size);
        Instant now = Instant.now(clock);

        boolean effectiveExcludeEnded = excludeEnded || sortType == VoteSortType.ENDING_SOON;

        Slice<Vote> slice = switch (sortType) {
            case LATEST -> {
                Long idCursor = parseLongCursor(cursor);
                yield voteRepository.findForHomeByLatest(idCursor, now, effectiveExcludeEnded, pageable);
            }
            case POPULAR -> {
                PopularCursor popularCursor = parsePopularCursor(cursor);
                yield voteRepository.findForHomeByPopularWithKeyset(
                        popularCursor.lastViewCount(),
                        popularCursor.lastId(),
                        now,
                        effectiveExcludeEnded,
                        pageable
                );
            }
            case ENDING_SOON -> {
                EndingSoonCursor endingCursor = parseEndingSoonCursor(cursor);
                yield voteRepository.findForHomeByEndingSoonWithKeyset(
                        endingCursor.lastEndAt(),
                        endingCursor.lastId(),
                        now,
                        pageable
                );
            }
        };

        List<Vote> votes = slice.getContent();

        List<VoteListItem> items = votes.stream()
                .map(vote -> new VoteListItem(
                        vote.getId(),
                        vote.getThumbnailUrl(),
                        vote.getStatus(clock),
                        vote.getTitle(),
                        vote.getContent(),
                        vote.getEndAt()
                ))
                .toList();

        String nextCursor = (slice.hasNext() && !votes.isEmpty())
                ? encodeNextCursor(sortType, votes)
                : null;

        return new VoteListResult(items, nextCursor, slice.hasNext());
    }

    // === Cursor Parsing ===

    private Long parseLongCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record PopularCursor(Long lastViewCount, Long lastId) {}

    private PopularCursor parsePopularCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return new PopularCursor(null, null);
        String[] parts = cursor.split(":");
        if (parts.length != 2) return new PopularCursor(null, null);
        try {
            return new PopularCursor(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
        } catch (NumberFormatException e) {
            return new PopularCursor(null, null);
        }
    }

    private record EndingSoonCursor(Instant lastEndAt, Long lastId) {}

    private EndingSoonCursor parseEndingSoonCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return new EndingSoonCursor(null, null);
        String[] parts = cursor.split(":");
        if (parts.length != 2) return new EndingSoonCursor(null, null);
        try {
            Instant endAt = Instant.ofEpochMilli(Long.parseLong(parts[0]));
            Long id = Long.parseLong(parts[1]);
            return new EndingSoonCursor(endAt, id);
        } catch (Exception e) {
            return new EndingSoonCursor(null, null);
        }
    }

    // === Next Cursor Encoding ===

    private String encodeNextCursor(VoteSortType sortType, List<Vote> votes) {
        if (votes.isEmpty()) return null;

        Vote last = votes.get(votes.size() - 1);

        return switch (sortType) {
            case LATEST -> String.valueOf(last.getId());
            case POPULAR -> {
                // VoteStatistics를 조회해서 viewCount를 가져와야 정확함.
                // 현재 구조에서는 간단히 ID로 fallback (추후 개선)
                yield String.valueOf(last.getId());
            }
            case ENDING_SOON -> {
                long endAtMillis = last.getEndAt().toEpochMilli();
                yield endAtMillis + ":" + last.getId();
            }
        };
    }

    private double calculatePopularityScore(long participantCount, long viewCount) {
        return (participantCount * PARTICIPANT_WEIGHT) + (viewCount * VIEW_WEIGHT);
    }
}
