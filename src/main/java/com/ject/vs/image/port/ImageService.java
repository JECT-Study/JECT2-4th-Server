package com.ject.vs.image.port;

import com.ject.vs.config.S3Properties;
import com.ject.vs.image.exception.ImageUploadFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(S3Client.class)
public class ImageService {

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    private static final String IMAGE_PATH = "images/";

    public String upload(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String key = IMAGE_PATH + UUID.randomUUID() + extension;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Properties.bucket())
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return buildUrl(key);
        } catch (IOException e) {
            throw new ImageUploadFailedException();
        }
    }

    public void delete(String imageUrl) {
        String key = extractKey(imageUrl);

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(key)
                .build();

        s3Client.deleteObject(request);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String buildUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                s3Properties.bucket(), s3Properties.region(), key);
    }

    private String extractKey(String imageUrl) {
        String prefix = String.format("https://%s.s3.%s.amazonaws.com/",
                s3Properties.bucket(), s3Properties.region());
        return imageUrl.replace(prefix, "");
    }
}
