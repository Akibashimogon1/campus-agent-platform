package com.campus.agent.mcp.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class McpServerRegisterRequestTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void acceptsValidRegistrationValues() {
        McpServerRegisterRequest request = validRequest();

        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @Test
    void rejectsBlankRequiredFieldsAndUnsupportedEnums() {
        McpServerRegisterRequest request = validRequest();
        request.setServerCode(" ");
        request.setTransportType("HTTP");
        request.setStatus("UNKNOWN");

        Set<ConstraintViolation<McpServerRegisterRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("serverCode", "transportType", "status");
    }

    @Test
    void rejectsValuesOverDeclaredMaximumLengths() {
        McpServerRegisterRequest request = validRequest();
        request.setServerName("n".repeat(129));
        request.setEndpoint("e".repeat(1025));

        Set<ConstraintViolation<McpServerRegisterRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("serverName", "endpoint");
    }

    private static McpServerRegisterRequest validRequest() {
        McpServerRegisterRequest request = new McpServerRegisterRequest();
        // These literals match the allowed values declared by the DTO's validation constraints.
        request.setServerCode("test-server");
        request.setServerName("Test server");
        request.setTransportType("HTTP_SSE");
        request.setStatus("REGISTERED");
        return request;
    }
}