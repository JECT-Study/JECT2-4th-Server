package com.ject.vs.vote.exception;

import com.ject.vs.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VoteErrorCode implements ErrorCode {
    IMAGE_REQUIRED(
            "몰입형 투표에는 이미지가 필요합니다.",
            400
    ),
    INVALID_DURATION(
            "유효하지 않은 시간입니다.",
            400
    );

    private final String message;
    private final Integer statusCode;

    @Override
    public String getCode() {
        return this.name();
    }

}
