package com.ject.vs.image.exception;

import com.ject.vs.common.exception.BusinessException;

public class ImageUploadFailedException extends BusinessException {
    public ImageUploadFailedException() {
        super(ImageErrorCode.IMAGE_UPLOAD_FAILED);
    }
}
