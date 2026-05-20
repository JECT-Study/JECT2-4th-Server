package com.ject.vs.ai.port;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.ject.vs.ai.config.GeminiProperties;
import com.ject.vs.ai.port.in.AiInsightUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiInsightService implements AiInsightUseCase {

    private final VertexAI vertexAI;
    private final GeminiProperties properties;

    @Override
    public Optional<AiInsightResult> generateVoteInsight(VoteInsightRequest request) {
        if (vertexAI == null || !properties.enabled()) {
            log.warn("AI insight generation is disabled or Gemini is not configured");
            return Optional.empty();
        }

        try {
            String prompt = buildPrompt(request);

            GenerativeModel model = new GenerativeModel(properties.model(), vertexAI);
            GenerateContentResponse response = model.generateContent(prompt);
            String responseText = ResponseHandler.getText(response);

            return parseResponse(responseText);

        } catch (Exception e) {
            log.error("Failed to generate AI insight for vote: {}", request.voteTitle(), e);
            return Optional.empty();
        }
    }

    private String buildPrompt(VoteInsightRequest request) {
        return String.format("""
                당신은 투표 결과를 분석하는 전문가입니다. 한국어로 친근하고 자연스럽게 분석 결과를 작성해주세요.

                다음 투표 결과를 분석해주세요:

                투표 주제: %s

                선택지 A: "%s" - %d명 (%d%%)
                선택지 B: "%s" - %d명 (%d%%)

                총 참여자: %d명
                성별 분포: 여성 %d%%, 남성 %d%%
                가장 많이 참여한 연령대: %s

                위 결과를 바탕으로 흥미로운 인사이트를 작성해주세요.

                응답은 반드시 다음 형식으로 작성해주세요:
                HEADLINE: (한 줄 요약, 50자 이내)
                BODY: (상세 분석, 100자 이내)
                """,
                request.voteTitle(),
                request.optionALabel(), request.optionACount(), request.optionARatio(),
                request.optionBLabel(), request.optionBCount(), request.optionBRatio(),
                request.totalParticipants(),
                request.femaleRatio(), request.maleRatio(),
                request.majorityAgeGroup()
        );
    }

    private Optional<AiInsightResult> parseResponse(String responseText) {
        if (responseText == null || responseText.isBlank()) {
            return Optional.empty();
        }

        String headline = "";
        String body = "";

        String[] lines = responseText.split("\n");
        for (String line : lines) {
            if (line.startsWith("HEADLINE:")) {
                headline = line.substring("HEADLINE:".length()).trim();
            } else if (line.startsWith("BODY:")) {
                body = line.substring("BODY:".length()).trim();
            }
        }

        if (headline.isEmpty() || body.isEmpty()) {
            log.warn("Failed to parse AI response: {}", responseText);
            return Optional.empty();
        }

        return Optional.of(new AiInsightResult(headline, body));
    }
}
