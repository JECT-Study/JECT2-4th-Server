package com.ject.vs.image.adapter.web;

import com.ject.vs.image.adapter.web.dto.ImageUploadResponse;
import com.ject.vs.image.port.ImageService;
import com.ject.vs.vote.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@ConditionalOnBean(ImageService.class)
public class ImageController {

    private final ImageService imageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImageUploadResponse upload(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file) {
        if (userId == null) throw new UnauthorizedException();
        String imageUrl = imageService.upload(file);
        return new ImageUploadResponse(imageUrl);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal Long userId,
            @RequestParam("url") String imageUrl) {
        if (userId == null) throw new UnauthorizedException();
        imageService.delete(imageUrl);
    }
}
