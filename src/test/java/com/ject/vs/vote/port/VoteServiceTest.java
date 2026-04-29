package com.ject.vs.vote.port;

import com.ject.vs.vote.domain.VoteParticipation;
import com.ject.vs.vote.domain.VoteParticipationRepository;
import com.ject.vs.vote.port.in.dto.VoteStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @InjectMocks
    private VoteService voteService;

    @Mock
    private VoteParticipationRepository voteParticipationRepository;

    @Nested
    class findAllVoteIdsByUserId {

        @Test
        void 유저가_참여한_voteId_목록을_반환한다() {
            // given
            given(voteParticipationRepository.findAllByUserId(1L))
                    .willReturn(List.of(VoteParticipation.of(10L, 1L), VoteParticipation.of(20L, 1L)));

            // when
            List<Long> result = voteService.findAllVoteIdsByUserId(1L);

            // then
            assertThat(result).containsExactly(10L, 20L);
        }

        @Test
        void 참여한_투표가_없으면_빈_목록을_반환한다() {
            // given
            given(voteParticipationRepository.findAllByUserId(1L)).willReturn(List.of());

            // when
            List<Long> result = voteService.findAllVoteIdsByUserId(1L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class countParticipantsByVoteId {

        @Test
        void 투표_참여자_수를_반환한다() {
            // given
            given(voteParticipationRepository.countByVoteId(1L)).willReturn(25L);

            // when
            long result = voteService.countParticipantsByVoteId(1L);

            // then
            assertThat(result).isEqualTo(25L);
            verify(voteParticipationRepository).countByVoteId(1L);
        }
    }

    @Nested
    class findAllUserIdsByVoteId {

        @Test
        void 투표에_참여한_userId_목록을_반환한다() {
            // given
            given(voteParticipationRepository.findAllByVoteId(1L))
                    .willReturn(List.of(VoteParticipation.of(1L, 100L), VoteParticipation.of(1L, 200L)));

            // when
            List<Long> result = voteService.findAllUserIdsByVoteId(1L);

            // then
            assertThat(result).containsExactly(100L, 200L);
        }
    }

    @Nested
    class findAllVoteIdsByStatus {

        @Test
        void ONGOING_상태로_조회하면_전달된_voteId_목록을_그대로_반환한다() {
            // given
            List<Long> voteIds = List.of(1L, 2L, 3L);

            // when
            List<Long> result = voteService.findAllVoteIdsByStatus(voteIds, VoteStatus.ONGOING);

            // then
            // TODO: Vote 도메인 연동 후 실제 status 필터링 검증으로 교체
            assertThat(result).isEqualTo(voteIds);
        }

        @Test
        void 빈_목록을_전달하면_빈_목록을_반환한다() {
            // given
            List<Long> voteIds = List.of();

            // when
            List<Long> result = voteService.findAllVoteIdsByStatus(voteIds, VoteStatus.ENDED);

            // then
            assertThat(result).isEmpty();
        }
    }
}
