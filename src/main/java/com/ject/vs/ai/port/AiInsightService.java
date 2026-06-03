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

    private final Optional<VertexAI> vertexAI;
    private final GeminiProperties properties;

    @Override
    public Optional<AiInsightResult> generateVoteInsight(VoteInsightRequest request) {
        if (vertexAI.isEmpty() || !properties.enabled()) {
            log.warn("AI insight generation is disabled or Gemini is not configured");
            return Optional.empty();
        }

        try {
            String prompt = buildPrompt(request);

            GenerativeModel model = new GenerativeModel(properties.model(), vertexAI.get());
            GenerateContentResponse response = model.generateContent(prompt);
            String responseText = ResponseHandler.getText(response);

            return parseResponse(responseText);

        } catch (Exception e) {
            log.error("Failed to generate AI insight for vote: {}", request.voteTitle(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<AiInsightResult> generatePersonalizedInsight(PersonalizedVoteInsightRequest request) {
        if (vertexAI.isEmpty() || !properties.enabled()) {
            log.warn("AI insight generation is disabled or Gemini is not configured");
            return Optional.empty();
        }

        try {
            String prompt = buildPersonalizedPrompt(request);

            GenerativeModel model = new GenerativeModel(properties.model(), vertexAI.get());
            GenerateContentResponse response = model.generateContent(prompt);
            String responseText = ResponseHandler.getText(response);

            return parseResponse(responseText);

        } catch (Exception e) {
            log.error("Failed to generate personalized AI insight for vote: {}", request.voteTitle(), e);
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

    private String buildPersonalizedPrompt(PersonalizedVoteInsightRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 투표 결과를 분석하는 전문가입니다. 한국어로 친근하고 자연스럽게 \"당신\"에게 개인화된 분석 결과를 작성해주세요.\n\n");

        sb.append("[투표 정보]\n");
        sb.append(String.format("투표 주제: %s\n", request.voteTitle()));
        sb.append(String.format("선택지 A: \"%s\" - %d명 (%d%%)\n",
                request.optionALabel(), request.optionACount(), request.optionARatio()));
        sb.append(String.format("선택지 B: \"%s\" - %d명 (%d%%)\n",
                request.optionBLabel(), request.optionBCount(), request.optionBRatio()));
        sb.append(String.format("총 참여자: %d명\n\n", request.totalParticipants()));

        sb.append("[당신의 선택]\n");
        sb.append(String.format("선택한 옵션: %s\n\n",
                request.userSelectedOption() != null ? request.userSelectedOption() : "알 수 없음"));

        sb.append("[당신의 프로필]\n");
        sb.append(String.format("성별: %s\n",
                formatGender(request.userGender())));
        sb.append(String.format("연령대: %s\n\n",
                request.userAgeGroup() != null ? request.userAgeGroup() : "알 수 없음"));

        sb.append("[그룹 비교]\n");
        if (request.userGender() != null) {
            sb.append(String.format("- 같은 성별(%s) 중 %d%%가 당신과 같은 선택\n",
                    formatGender(request.userGender()), request.sameGenderRatio()));
            if (request.sameGenderMajorityOption() != null) {
                sb.append(String.format("- %s의 다수 선택: %s\n",
                        formatGender(request.userGender()), request.sameGenderMajorityOption()));
            }
        }
        if (request.userAgeGroup() != null) {
            sb.append(String.format("- 같은 연령대(%s) 중 %d%%가 당신과 같은 선택\n",
                    request.userAgeGroup(), request.sameAgeGroupRatio()));
            if (request.sameAgeGroupMajorityOption() != null) {
                sb.append(String.format("- %s의 다수 선택: %s\n",
                        request.userAgeGroup(), request.sameAgeGroupMajorityOption()));
            }
        }

        sb.append("\n위 정보를 바탕으로 \"당신\"에게 개인화된 흥미로운 인사이트를 작성해주세요.\n");
        sb.append("2인칭(\"당신\")을 사용하여 친근하게 작성해주세요.\n\n");

        sb.append("응답은 반드시 다음 형식으로 작성해주세요:\n");
        sb.append("HEADLINE: (한 줄 요약, 50자 이내)\n");
        sb.append("BODY: (상세 분석, 100자 이내)\n");

        return sb.toString();
    }

    private String formatGender(String gender) {
        if (gender == null) return "알 수 없음";
        return switch (gender) {
            case "MALE" -> "남성";
            case "FEMALE" -> "여성";
            default -> gender;
        };
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
