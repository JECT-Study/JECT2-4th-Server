package com.ject.vs.vote.domain;

import com.ject.vs.vote.exception.InvalidDurationException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VoteDurationTest {

    @Test
    void fromHours_12() {
        VoteDuration duration = VoteDuration.fromHours(12);
        assertThat(duration).isEqualTo(VoteDuration.HOURS_12);
        assertThat(duration.getValue()).isEqualTo(Duration.ofHours(12));
    }

    @Test
    void fromHours_24() {
        VoteDuration duration = VoteDuration.fromHours(24);
        assertThat(duration).isEqualTo(VoteDuration.HOURS_24);
        assertThat(duration.getValue()).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void 유효하지_않은_시간은_InvalidDurationException() {
        assertThatThrownBy(() -> VoteDuration.fromHours(13))
                .isInstanceOf(InvalidDurationException.class);
    }
}
