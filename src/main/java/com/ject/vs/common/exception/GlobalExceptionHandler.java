package com.ject.vs.common.exception;

import com.ject.vs.vote.exception.*;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(VoteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVoteNotFound(VoteNotFoundException e) {
        return ResponseEntity.status(404).body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(VoteEndedException.class)
    public ResponseEntity<ErrorResponse> handleVoteEnded(VoteEndedException e) {
        return ResponseEntity.status(403).body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(VoteNotEndedException.class)
    public ResponseEntity<ErrorResponse> handleVoteNotEnded(VoteNotEndedException e) {
        return ResponseEntity.status(403).body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(VoteFreeLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleVoteFreeLimit(VoteFreeLimitExceededException e) {
        return ResponseEntity.status(403).body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(InvalidOptionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOption(InvalidOptionException e) {
        return ResponseEntity.status(400).body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(InvalidEmojiException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEmoji(InvalidEmojiException e) {
        return ResponseEntity.status(400).body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(new ErrorResponse(e.getErrorCode(), e.getErrorMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException e) {
        return ResponseEntity.status(401).body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("입력값이 올바르지 않습니다");
        return ResponseEntity.status(400).body(new ErrorResponse("INVALID_INPUT", message));
    }

    public record ErrorResponse(String code, String message) {
    }
}
