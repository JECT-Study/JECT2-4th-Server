package com.ject.vs.vote.exception;

import com.ject.vs.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VoteErrorCode implements ErrorCode {
    VOTE_NOT_FOUND("투표를 찾을 수 없습니다.", 404),
    VOTE_ENDED("이미 종료된 투표입니다.", 403),
    VOTE_NOT_ENDED("아직 진행 중인 투표입니다.", 403),
    VOTE_FREE_LIMIT_EXCEEDED("무료 투표 횟수를 초과했습니다.", 403),
    INVALID_OPTION("유효하지 않은 투표 선택지입니다.", 400),
    INVALID_EMOJI("유효하지 않은 이모지입니다.", 400),
    UNAUTHORIZED("인증이 필요합니다.", 401),
    IMAGE_REQUIRED("몰입형 투표에는 이미지가 필요합니다.", 400),
    INVALID_DURATION("유효하지 않은 시간입니다.", 400);

    private final String message;
    private final Integer statusCode;

    @Override
    public String getCode() {
        return this.name();
    }

}
