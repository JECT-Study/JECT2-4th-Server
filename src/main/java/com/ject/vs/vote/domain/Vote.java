package com.ject.vs.vote.domain;

import com.ject.vs.common.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "vote")
@NoArgsConstructor(access = PROTECTED)
public class Vote extends BaseEntity {
}
