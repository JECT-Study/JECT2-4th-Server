package com.ject.vs.vote.domain;

import com.ject.vs.user.domain.Gender;

public record GenderCount(Gender gender, long count) {
}
