package com.campus.agent.mcp.error;

import com.campus.agent.mcp.api.McpApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

@RestControllerAdvice
public class McpGlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpGlobalExceptionHandler.class);

    @ExceptionHandler(McpBusinessException.class)
    public ResponseEntity<McpApiResponse<Void>> handleBusinessException(McpBusinessException exception) {
        return response(exception.getErrorCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<McpApiResponse<Void>> handleInvalidArguments(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .distinct()
                .collect(Collectors.joining("; "));
        return response(McpErrorCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<McpApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return response(McpErrorCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<McpApiResponse<Void>> handleMethodValidation(HandlerMethodValidationException exception) {
        return response(McpErrorCode.VALIDATION_ERROR, "Request parameters are invalid");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<McpApiResponse<Void>> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return response(McpErrorCode.INVALID_REQUEST, "Request body is missing or malformed");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<McpApiResponse<Void>> handleUnexpectedException(Exception exception) {
        LOGGER.error("Unhandled MCP API exception", exception);
        return response(McpErrorCode.INTERNAL_ERROR, "An unexpected error occurred");
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<McpApiResponse<Void>> response(McpErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.httpStatus()).body(McpApiResponse.error(errorCode, message));
    }
}