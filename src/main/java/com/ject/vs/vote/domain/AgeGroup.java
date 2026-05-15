package com.ject.vs.vote.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;

@Getter
@RequiredArgsConstructor
public enum AgeGroup {
    TEENS("10s"),
    TWENTIES("20s"),
    THIRTIES("30s"),
    FORTIES("40s"),
    FIFTIES_PLUS("50s_PLUS");

    private final String label;

    public static AgeGroup fromBirthYear(Year birthYear, Clock clock) {
        int age = LocalDate.now(clock).getYear() - birthYear.getValue();
        if (age < 20) return TEENS;
        if (age < 30) return TWENTIES;
        if (age < 40) return THIRTIES;
        if (age < 50) return FORTIES;
        return FIFTIES_PLUS;
    }
}
