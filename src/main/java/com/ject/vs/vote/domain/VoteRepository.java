package com.ject.vs.vote.domain;

import com.ject.vs.user.domain.User;
import org.checkerframework.checker.units.qual.A;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    @Query("SELECT v FROM Vote v WHERE v.endAt < :now")
    List<Vote> findExpiredOngoing(@Param("now") Instant now);

    List<Vote> findAllByIdIn(List<Long> ids);

    Slice<Vote> findByTypeOrderByEndAtDesc(VoteType type, Pageable pageable);

    Slice<Vote> findByTypeAndIdLessThanOrderByEndAtDesc(VoteType type, Long cursor, Pageable pageable);

    @Query("select v from Vote v join VoteParticipation vp on vp.voteId = v.id " +
            "where vp.userId = :userId and v.endAt > current_timestamp " +
            "order by v.createdAt desc")
    List<Vote> findVotesByOrderByLatest(@Param("userId") Long userId);

    @Query("select v from Vote v join VoteParticipation vp on vp.voteId = v.id " +
            "where vp.userId = :userId and v.endAt > current_timestamp " +
            "order by v.endAt asc")
    List<Vote> findVotesByOrderByDeadLine(@Param("userId") Long userId);

    @Query("select v from Vote v " +
            "left join VoteEmojiReaction ver on ver.id = v.id " +
            "where v.endAt > current_timestamp " +
            "group by v.id " +
            "order by count(ver.id) desc, v.createdAt desc")
    List<Vote> findVotesByOrderByPopularity(@Param("userId") Long userId);

    @Query("select v from Vote v " +
            "join VoteParticipation vp on vp.voteId = v.id " +
            "where vp.userId = :userId and v.endAt < current_timestamp " +
            "order by v.createdAt desc")
    List<Vote> findVotesEndByLatest(@Param("userId") Long userId);

    @Query("select v from Vote v " +
            "join VoteParticipation vp on v.id = vp.voteId " +
            "where vp.userId = :userId and v.endAt < current_timestamp " +
            "order by v.createdAt asc")
    List<Vote> findVotesEndByDeadLine(@Param("userId") Long userId);
    // 진행 중인 투표 조회 (핫토픽 등에서 사용)
    @Query("SELECT v FROM Vote v WHERE v.endAt > :now")
    List<Vote> findOngoingVotes(@Param("now") Instant now);

    // ===== 홈 화면 전체 투표 목록 조회용 단일 쿼리 (cursor null 처리 포함) =====

    /**
     * 홈 화면용 최신순 조회 (단일 쿼리)
     * cursor가 null이면 첫 페이지, 값이 있으면 해당 커서 이후 데이터
     */
    @Query("""
        SELECT v FROM Vote v
        WHERE (:cursor IS NULL OR v.id < :cursor)
          AND (:excludeEnded = FALSE OR v.endAt > :now)
        ORDER BY v.id DESC
        """)
    Slice<Vote> findForHomeByLatest(
            @Param("cursor") Long cursor,
            @Param("now") Instant now,
            @Param("excludeEnded") boolean excludeEnded,
            Pageable pageable
    );

    /**
     * 홈 화면용 인기순 조회 (단일 쿼리, 조회수 기준)
     * cursor가 null이면 첫 페이지, 값이 있으면 해당 커서 이후 데이터
     */
    @Query("""
        SELECT v FROM Vote v
        LEFT JOIN VoteStatistics vs ON v.id = vs.voteId
        WHERE (:cursor IS NULL OR v.id < :cursor)
          AND (:excludeEnded = FALSE OR v.endAt > :now)
        ORDER BY COALESCE(vs.viewCount, 0) DESC, v.id DESC
        """)
    Slice<Vote> findForHomeByPopular(
            @Param("cursor") Long cursor,
            @Param("now") Instant now,
            @Param("excludeEnded") boolean excludeEnded,
            Pageable pageable
    );

    /**
     * 홈 화면용 종료임박순 조회 (단일 쿼리, 항상 진행 중인 투표만)
     * cursor가 null이면 첫 페이지, 값이 있으면 해당 커서 이후 데이터
     */
    @Query("""
        SELECT v FROM Vote v
        WHERE v.endAt > :now
          AND (:cursor IS NULL OR v.id < :cursor)
        ORDER BY v.endAt ASC
        """)
    Slice<Vote> findForHomeByEndingSoon(
            @Param("now") Instant now,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
