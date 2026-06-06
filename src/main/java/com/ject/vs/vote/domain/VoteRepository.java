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

    @Query("""
        SELECT v FROM Vote v
         WHERE v.endAt < :now
           AND v.endedProcessedAt IS NULL
        """)
    List<Vote> findUnprocessedExpired(@Param("now") Instant now);

    List<Vote> findAllByIdIn(List<Long> ids);

    @Query("SELECT v FROM Vote v WHERE v.endAt > :now ORDER BY v.id DESC")
    Slice<Vote> findByEndAtAfterOrderByIdDesc(@Param("now") Instant now, Pageable pageable);

    @Query("SELECT v FROM Vote v WHERE v.id < :cursor AND v.endAt > :now ORDER BY v.id DESC")
    Slice<Vote> findByIdLessThanAndEndAtAfterOrderByIdDesc(@Param("cursor") Long cursor, @Param("now") Instant now, Pageable pageable);

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
            "join VoteParticipation vp on v.id = vp.voteId " +
            "where vp.userId = :userId and v.endAt < current_timestamp " +
            "order by v.createdAt desc")
    List<Vote> findVotesEndByLatest(@Param("userId") Long userId);

    @Query("select v from Vote v " +
            "join VoteParticipation vp on v.id = vp.voteId " +
            "where vp.userId = :userId and v.endAt < current_timestamp " +
            "order by v.endAt asc")
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
     * 인기순 Keyset Pagination 전용 (복합 커서)
     */
    @Query("""
        SELECT v FROM Vote v
        LEFT JOIN VoteStatistics vs ON v.id = vs.voteId
        WHERE (:lastViewCount IS NULL 
               OR COALESCE(vs.viewCount, 0) < :lastViewCount
               OR (COALESCE(vs.viewCount, 0) = :lastViewCount AND v.id < :lastId))
          AND (:excludeEnded = FALSE OR v.endAt > :now)
        ORDER BY COALESCE(vs.viewCount, 0) DESC, v.id DESC
        """)
    Slice<Vote> findForHomeByPopularWithKeyset(
            @Param("lastViewCount") Long lastViewCount,
            @Param("lastId") Long lastId,
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

    /**
     * 종료임박순 - 첫 페이지 전용 (cursor 없음)
     * null 커서 파라미터 + IS NULL 표현식을 제거하여 Postgres/Neon 환경에서 안정적으로 동작하게 함.
     */
    @Query("""
        SELECT v FROM Vote v
        WHERE v.endAt > :now
        ORDER BY v.endAt ASC, v.id ASC
        """)
    Slice<Vote> findFirstPageForHomeByEndingSoon(
            @Param("now") Instant now,
            Pageable pageable
    );

    /**
     * 종료임박순 Keyset Pagination 전용 (복합 커서, 두 번째 페이지 이후)
     */
    @Query("""
        SELECT v FROM Vote v
        WHERE v.endAt > :now
          AND (v.endAt > :lastEndAt
               OR (v.endAt = :lastEndAt AND v.id > :lastId))
        ORDER BY v.endAt ASC, v.id ASC
        """)
    Slice<Vote> findForHomeByEndingSoonWithKeyset(
            @Param("lastEndAt") Instant lastEndAt,
            @Param("lastId") Long lastId,
            @Param("now") Instant now,
            Pageable pageable
    );

    // ===== 몰입형 투표 랜덤 조회 =====

    /**
     * 진행 중인 투표 중 excludeIds를 제외하고 랜덤으로 조회
     */
    @Query("""
        SELECT v FROM Vote v
        WHERE v.endAt > :now
          AND v.id NOT IN :excludeIds
        ORDER BY FUNCTION('RANDOM')
        """)
    Slice<Vote> findRandomExcluding(
            @Param("now") Instant now,
            @Param("excludeIds") List<Long> excludeIds,
            Pageable pageable
    );

    /**
     * 진행 중인 투표 랜덤 조회 (excludeIds 없는 첫 조회용)
     */
    @Query("""
        SELECT v FROM Vote v
        WHERE v.endAt > :now
        ORDER BY FUNCTION('RANDOM')
        """)
    Slice<Vote> findRandom(
            @Param("now") Instant now,
            Pageable pageable
    );

    /**
     * 진행 중인 투표 총 개수 조회 (무한 순환 판단용)
     */
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.endAt > :now")
    long countOngoing(@Param("now") Instant now);
}
