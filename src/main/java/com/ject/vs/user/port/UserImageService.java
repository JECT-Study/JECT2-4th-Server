package com.ject.vs.user.port;

import com.ject.vs.user.domain.ImageColor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class UserImageService {
    public ImageColor getRandomColor() {
        ImageColor[] colors = ImageColor.values();

        int randomIdx = ThreadLocalRandom.current().nextInt(colors.length);

        return colors[randomIdx];
    }
}
