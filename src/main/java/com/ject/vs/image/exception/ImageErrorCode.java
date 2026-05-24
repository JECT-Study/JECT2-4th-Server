package com.ject.vs.image.exception;

import com.ject.vs.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImageErrorCode implements ErrorCode {
    IMAGE_UPLOAD_FAILED("이미지 업로드에 실패했습니다.", 500),
    INVALID_IMAGE_TYPE("지원하지 않는 이미지 형식입니다.", 400),
    IMAGE_SIZE_EXCEEDED("이미지 크기가 제한을 초과했습니다.", 400);

    private final String message;
    private final Integer statusCode;

    @Override
    public String getCode() {
        return this.name();
    }
}
