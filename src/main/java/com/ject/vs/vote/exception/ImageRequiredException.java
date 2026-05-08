package com.ject.vs.vote.exception;

public class ImageRequiredException extends RuntimeException {

    public ImageRequiredException() {
        super("몰입형 투표에는 이미지가 필요합니다.");
    }

    public String getErrorCode() {
        return "IMAGE_REQUIRED";
    }
}
