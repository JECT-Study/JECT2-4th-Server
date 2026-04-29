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

    // Vote 상세 필드(title, status 등)는 Vote 담당자가 추가 예정. 현재는 id만 관리.
    public static Vote of() {
        return new Vote();
    }
}
