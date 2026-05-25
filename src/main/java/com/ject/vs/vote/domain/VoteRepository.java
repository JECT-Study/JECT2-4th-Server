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
    // 홈 화면용 쿼리 메서드

    // 진행 중인 투표 조회
    @Query("SELECT v FROM Vote v WHERE v.endAt > :now")
    List<Vote> findOngoingVotes(@Param("now") Instant now);

    // 최신순 (전체)
    Slice<Vote> findAllByOrderByIdDesc(Pageable pageable);

    Slice<Vote> findByIdLessThanOrderByIdDesc(Long cursor, Pageable pageable);

    // 종료임박순 (진행 중인 것만)
    @Query("SELECT v FROM Vote v WHERE v.endAt > :now ORDER BY v.endAt ASC")
    Slice<Vote> findOngoingOrderByEndAtAsc(@Param("now") Instant now, Pageable pageable);

    @Query("SELECT v FROM Vote v WHERE v.endAt > :now AND v.id < :cursor ORDER BY v.endAt ASC")
    Slice<Vote> findOngoingByIdLessThanOrderByEndAtAsc(@Param("now") Instant now, @Param("cursor") Long cursor, Pageable pageable);

    // 인기순 (조회수 기준)
    @Query("""
            SELECT v FROM Vote v
            LEFT JOIN VoteStatistics vs ON v.id = vs.voteId
            ORDER BY COALESCE(vs.viewCount, 0) DESC, v.id DESC
            """)
    Slice<Vote> findAllOrderByViewCountDesc(Pageable pageable);

    @Query("""
            SELECT v FROM Vote v
            LEFT JOIN VoteStatistics vs ON v.id = vs.voteId
            WHERE v.id < :cursor
            ORDER BY COALESCE(vs.viewCount, 0) DESC, v.id DESC
            """)
    Slice<Vote> findByIdLessThanOrderByViewCountDesc(@Param("cursor") Long cursor, Pageable pageable);
}
