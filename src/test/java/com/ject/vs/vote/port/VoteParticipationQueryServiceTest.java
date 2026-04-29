package com.ject.vs.vote.port;

import com.ject.vs.vote.domain.VoteParticipation;
import com.ject.vs.vote.domain.VoteParticipationRepository;
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
class VoteParticipationQueryServiceTest {

    @InjectMocks
    private VoteParticipationQueryService voteParticipationQueryService;

    @Mock
    private VoteParticipationRepository voteParticipationRepository;

    @Nested
    class findAllVoteIdsByUserId {

        @Test
        void 유저가_참여한_voteId_목록을_반환한다() {
            // given
            VoteParticipation p1 = VoteParticipation.of(10L, 1L);
            VoteParticipation p2 = VoteParticipation.of(20L, 1L);
            given(voteParticipationRepository.findAllByUserId(1L)).willReturn(List.of(p1, p2));

            // when
            List<Long> result = voteParticipationQueryService.findAllVoteIdsByUserId(1L);

            // then
            assertThat(result).containsExactly(10L, 20L);
        }

        @Test
        void 참여한_투표가_없으면_빈_목록을_반환한다() {
            // given
            given(voteParticipationRepository.findAllByUserId(1L)).willReturn(List.of());

            // when
            List<Long> result = voteParticipationQueryService.findAllVoteIdsByUserId(1L);

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
            long result = voteParticipationQueryService.countParticipantsByVoteId(1L);

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
            VoteParticipation p1 = VoteParticipation.of(1L, 100L);
            VoteParticipation p2 = VoteParticipation.of(1L, 200L);
            given(voteParticipationRepository.findAllByVoteId(1L)).willReturn(List.of(p1, p2));

            // when
            List<Long> result = voteParticipationQueryService.findAllUserIdsByVoteId(1L);

            // then
            assertThat(result).containsExactly(100L, 200L);
        }
    }
}
