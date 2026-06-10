package com.ject.vs.vote.domain;

/**
 * 이모지 반응 동작 분류. 행동 로그(emoji_reacted)의 {@code action} 변수로 사용된다.
 * 사용자의 이전 반응 상태와 비교하여 결정된다.
 */
public enum EmojiAction {
    /** 기존 반응이 없던 상태에서 새로 등록 */
    CREATED,
    /** 다른 이모지로 교체 */
    CHANGED,
    /** 기존 반응을 취소(같은 이모지 재클릭 또는 null 전송) */
    CANCELED
}
