package com.ject.vs.vote.adapter.web.dto;

import com.ject.vs.vote.port.in.VoteResultQueryUseCase.ShareLinkResult;

public record ShareLinkResponse(String shareUrl, String title, String thumbnailUrl) {

    public static ShareLinkResponse from(ShareLinkResult result) {
        return new ShareLinkResponse(result.shareUrl(), result.title(), result.thumbnailUrl());
    }
}
