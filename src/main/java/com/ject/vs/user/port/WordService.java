package com.ject.vs.user.port;

import com.ject.vs.user.domain.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordService {
    private final ResourceLoader resourceLoader;
    private final UserRepository userRepository;

    private final Random random = new Random();

    private List<String> words2 = new ArrayList<>();
    private List<String> words3 = new ArrayList<>();

    @PostConstruct
    public void init() {
        try {
            words2 = loadWords("classpath:data/ko_words_2.txt");
            words3 = loadWords("classpath:data/ko_words_3.txt");
        } catch (IOException e) {
            log.error("단어 로딩 실패", e);
        }
    }

    public List<String> loadWords(String location) throws IOException {
        Resource resource = resourceLoader.getResource(location);
        try(BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .toList();
        }
    }

    public String generateCombinedWord() {
        if(words2.isEmpty() || words3.isEmpty()) {
            return "데이터 없음";
        }

        String first = words2.get(random.nextInt(words2.size()));
        String second = words3.get(random.nextInt(words3.size()));

        return first + second;
    }

    public String generateNumber() {
        int number = ThreadLocalRandom.current().nextInt(10000);
        return String.format("%04d", number);
    }

    public String generateNickname() {
        String ret;
        do {
            String nickname = generateCombinedWord();
            String number = generateNumber();

            ret = nickname + '_' + number;
        } while(!userRepository.isNicknameAvailable(ret));

        return ret;
    }
}
