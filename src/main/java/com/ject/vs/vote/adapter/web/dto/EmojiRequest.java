package com.ject.vs.vote.adapter.web.dto;

import com.ject.vs.vote.domain.VoteEmoji;

public record EmojiRequest(VoteEmoji emoji) {
    // emoji == null 이면 취소
}
