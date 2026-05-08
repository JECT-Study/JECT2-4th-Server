package com.ject.vs.vote.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    @Query("SELECT v FROM Vote v WHERE v.status = com.ject.vs.vote.domain.VoteStatus.ONGOING AND v.endAt < :now")
    List<Vote> findExpiredOngoing(@Param("now") Instant now);

    List<Vote> findAllByIdIn(List<Long> ids);

    Slice<Vote> findByTypeOrderByEndAtDesc(VoteType type, Pageable pageable);

    Slice<Vote> findByTypeAndIdLessThanOrderByEndAtDesc(VoteType type, Long cursor, Pageable pageable);
}
