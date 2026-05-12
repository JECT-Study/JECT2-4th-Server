package com.ject.vs.vote.adapter.web.dto;

import com.ject.vs.vote.domain.VoteEmoji;
import org.jspecify.annotations.Nullable;

public record EmojiRequest(@Nullable VoteEmoji emoji) {
    // emoji == null 이면 취소
}
