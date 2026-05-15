package com.ject.vs.vote.port;

import com.ject.vs.vote.domain.GuestFreeVote;
import com.ject.vs.vote.domain.GuestFreeVoteRepository;
import com.ject.vs.vote.exception.VoteFreeLimitExceededException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GuestFreeVoteServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

    @InjectMocks
    private GuestFreeVoteService service;

    @Mock
    private GuestFreeVoteRepository repository;

    @Mock
    private Clock clock;

    @Nested
    class consume {

        @Test
        void 신규_anonymousId_진입_시_새_row를_생성하고_consume한다() {
            given(repository.findById("new-anon")).willReturn(Optional.empty());
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T00:00:00Z"));

            service.consume("new-anon");

            ArgumentCaptor<GuestFreeVote> captor = ArgumentCaptor.forClass(GuestFreeVote.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getConsumedCount()).isEqualTo(1);
        }

        @Test
        void 기존_anonymousId는_기존_row에_consume한다() {
            GuestFreeVote existing = GuestFreeVote.create("existing-anon");
            given(repository.findById("existing-anon")).willReturn(Optional.of(existing));
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(clock.instant()).willReturn(Instant.parse("2025-01-01T00:00:00Z"));

            service.consume("existing-anon");

            assertThat(existing.getConsumedCount()).isEqualTo(1);
        }

        @Test
        void 다섯번_소진된_anonymousId는_VoteFreeLimitExceededException을_던진다() {
            GuestFreeVote exhausted = GuestFreeVote.create("exhausted-anon");
            for (int i = 0; i < 5; i++) exhausted.consume(FIXED_CLOCK);
            given(repository.findById("exhausted-anon")).willReturn(Optional.of(exhausted));

            assertThatThrownBy(() -> service.consume("exhausted-anon"))
                    .isInstanceOf(VoteFreeLimitExceededException.class);
        }
    }

    @Nested
    class remaining {

        @Test
        void 신규_anonymousId는_총_5회를_반환한다() {
            given(repository.findById("unknown")).willReturn(Optional.empty());

            int result = service.remaining("unknown");

            assertThat(result).isEqualTo(5);
        }

        @Test
        void 두번_소진된_경우_잔여는_3을_반환한다() {
            GuestFreeVote g = GuestFreeVote.create("anon-2consumed");
            g.consume(FIXED_CLOCK);
            g.consume(FIXED_CLOCK);
            given(repository.findById("anon-2consumed")).willReturn(Optional.of(g));

            int result = service.remaining("anon-2consumed");

            assertThat(result).isEqualTo(3);
        }
    }
}
