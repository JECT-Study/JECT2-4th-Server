package com.ject.vs.vote.port;

import com.ject.vs.vote.port.in.dto.VoteStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VoteQueryServiceTest {

    private final VoteQueryService voteQueryService = new VoteQueryService();

    @Nested
    class findAllVoteIdsByStatus {

        @Test
        void ONGOING_상태로_조회하면_전달된_voteId_목록을_그대로_반환한다() {
            // given
            List<Long> voteIds = List.of(1L, 2L, 3L);

            // when
            List<Long> result = voteQueryService.findAllVoteIdsByStatus(voteIds, VoteStatus.ONGOING);

            // then
            // TODO: Vote 도메인 연동 후 실제 status 필터링 검증으로 교체
            assertThat(result).isEqualTo(voteIds);
        }

        @Test
        void ENDED_상태로_조회하면_전달된_voteId_목록을_그대로_반환한다() {
            // given
            List<Long> voteIds = List.of(4L, 5L);

            // when
            List<Long> result = voteQueryService.findAllVoteIdsByStatus(voteIds, VoteStatus.ENDED);

            // then
            // TODO: Vote 도메인 연동 후 실제 status 필터링 검증으로 교체
            assertThat(result).isEqualTo(voteIds);
        }

        @Test
        void 빈_목록을_전달하면_빈_목록을_반환한다() {
            // given
            List<Long> voteIds = List.of();

            // when
            List<Long> result = voteQueryService.findAllVoteIdsByStatus(voteIds, VoteStatus.ONGOING);

            // then
            assertThat(result).isEmpty();
        }
    }
}
