package io.petcare.backend.ai.exception;

import io.petcare.backend.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "io.petcare.backend.ai")
public class AiExceptionHandler {

    @ExceptionHandler(AiUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnavailable(AiUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure(HttpStatus.SERVICE_UNAVAILABLE.value(), exception.getMessage()));
    }

    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<ApiResponse<Void>> handleProviderFailure(AiProviderException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure(HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "智能助手暂时不可用，请稍后再试"));
    }
}
