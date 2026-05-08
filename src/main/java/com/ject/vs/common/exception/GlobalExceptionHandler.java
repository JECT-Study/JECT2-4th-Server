package com.ject.vs.common.exception;

import com.ject.vs.vote.exception.*;
import org.springframework.http.ResponseEntity;
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

    @ExceptionHandler(InvalidDurationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDuration(InvalidDurationException e) {
        return ResponseEntity.status(400).body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(ImageRequiredException.class)
    public ResponseEntity<ErrorResponse> handleImageRequired(ImageRequiredException e) {
        return ResponseEntity.status(400).body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }

    public record ErrorResponse(String code, String message) {
    }
}
