package com.ject.vs.common.exception;

import com.ject.vs.analytics.AnalyticsEvent;
import com.ject.vs.analytics.AnalyticsEventLogger;
import com.ject.vs.vote.exception.VoteFreeLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final AnalyticsEventLogger analytics;

    /**
     * 무료 투표 한도 초과. 행동 로그(free_limit_exceeded)를 남긴 뒤 공통 비즈니스 예외 처리로 위임한다.
     */
    @ExceptionHandler(VoteFreeLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleFreeLimitExceeded(VoteFreeLimitExceededException e,
                                                                 HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        analytics.log(AnalyticsEvent.of("free_limit_exceeded")
                .put("vote_id", extractVoteId(request))
                .put("error_code", errorCode.getCode())
                .put("remaining_free_votes", 0));
        return ResponseEntity.status(errorCode.getStatusCode())
                .body(new ErrorResponse(errorCode.getCode(), errorCode.getMessage()));
    }

    /** /api/votes/{voteId}/... 또는 /api/immersive-votes/{voteId}/... 경로에서 voteId 추출. */
    private Long extractVoteId(HttpServletRequest request) {
        if (request == null) return null;
        String uri = request.getRequestURI();
        if (uri == null) return null;
        String[] parts = uri.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].endsWith("votes")) {
                try {
                    return Long.parseLong(parts[i + 1]);
                } catch (NumberFormatException ignored) {
                    // 숫자가 아니면 다음 후보 탐색
                }
            }
        }
        return null;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatusCode())
                .body(new ErrorResponse(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("입력값이 올바르지 않습니다");
        return ResponseEntity.status(400).body(new ErrorResponse("INVALID_INPUT", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(400).body(new ErrorResponse("INVALID_ARGUMENT", e.getMessage()));
    }
}
