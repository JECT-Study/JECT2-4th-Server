package com.ject.vs.vote.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface RecommendedVoteRepository extends JpaRepository<RecommendedVote, Long> {

    @Query("""
            SELECT rv FROM RecommendedVote rv
            JOIN FETCH rv.vote v
            WHERE rv.recommendedDate = :date
            AND v.endAt > :now
            ORDER BY rv.displayOrder ASC
            """)
    List<RecommendedVote> findByDateWithOngoingVotes(
            @Param("date") LocalDate date,
            @Param("now") Instant now
    );
}
