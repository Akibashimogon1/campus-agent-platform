package com.campus.agent.mcp.error;

import com.campus.agent.mcp.api.McpApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import com.campus.agent.mcp.api.McpToolStatusRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class McpGlobalExceptionHandlerTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    private final McpGlobalExceptionHandler handler = new McpGlobalExceptionHandler();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void mapsConstraintViolationsToUnifiedValidationError() {
        Set<ConstraintViolation<McpToolStatusRequest>> violations =
                VALIDATOR.validate(new McpToolStatusRequest("PENDING"));

        ResponseEntity<McpApiResponse<Void>> response = handler.handleConstraintViolation(
                new jakarta.validation.ConstraintViolationException(violations));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().message()).contains("status");
    }

    @Test
    void mapsBusinessErrorCodeToItsHttpStatusAndUnifiedBody() {
        ResponseEntity<McpApiResponse<Void>> response = handler.handleBusinessException(
                new McpBusinessException(McpErrorCode.RESOURCE_CONFLICT, "Server code already exists"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RESOURCE_CONFLICT");
        assertThat(response.getBody().message()).isEqualTo("Server code already exists");
        assertThat(response.getBody().data()).isNull();
    }

    @Test
    void doesNotExposeUnexpectedExceptionDetails() {
        ResponseEntity<McpApiResponse<Void>> response = handler.handleUnexpectedException(
                new IllegalStateException("private implementation detail"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).doesNotContain("private implementation detail");
    }
}