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
}
