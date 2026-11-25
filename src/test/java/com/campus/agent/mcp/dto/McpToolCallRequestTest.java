package com.campus.agent.mcp.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class McpToolCallRequestTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void acceptsValidToolCall() {
        assertThat(VALIDATOR.validate(validRequest())).isEmpty();
    }

    @Test
    void rejectsBlankRequiredFieldsAndNonPositiveVersions() {
        McpToolCallRequest request = validRequest();
        request.setTenantId(" ");
        request.setTraceId("");
        request.setToolName(" ");
        request.setToolVersion(0);
        request.setTimeoutMs(-1);

        Set<ConstraintViolation<McpToolCallRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("tenantId", "traceId", "toolName", "toolVersion", "timeoutMs");
    }

    @Test
    void rejectsValuesOverDeclaredMaximumLengths() {
        McpToolCallRequest request = validRequest();
        request.setTenantId("t".repeat(65));
        request.setToolName("n".repeat(129));

        Set<ConstraintViolation<McpToolCallRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("tenantId", "toolName");
    }

    private static McpToolCallRequest validRequest() {
        McpToolCallRequest request = new McpToolCallRequest();
        // These values satisfy the DTO's declared required-field constraints.
        request.setTenantId("test-tenant");
        request.setTraceId("test-trace");
        request.setToolName("test-tool");
        request.setToolVersion(1);
        request.setTimeoutMs(1000);
        return request;
    }
}