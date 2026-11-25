package com.campus.agent.mcp.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class McpPrimitiveRegisterRequestTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void acceptsValidPrimitiveRegistration() {
        assertThat(VALIDATOR.validate(validRequest())).isEmpty();
    }

    @Test
    void rejectsUnsupportedTypeBlankNameAndNonPositiveNumbers() {
        McpPrimitiveRegisterRequest request = validRequest();
        request.setServerId(0L);
        request.setPrimitiveType("FUNCTION");
        request.setName(" ");
        request.setTimeoutMs(0);

        Set<ConstraintViolation<McpPrimitiveRegisterRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("serverId", "primitiveType", "name", "timeoutMs");
    }

    @Test
    void rejectsValuesOverDeclaredMaximumLengths() {
        McpPrimitiveRegisterRequest request = validRequest();
        request.setCategory("c".repeat(65));
        request.setUriTemplate("u".repeat(513));

        Set<ConstraintViolation<McpPrimitiveRegisterRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("category", "uriTemplate");
    }

    private static McpPrimitiveRegisterRequest validRequest() {
        McpPrimitiveRegisterRequest request = new McpPrimitiveRegisterRequest();
        // These literals match the allowed values declared by the DTO's validation constraints.
        request.setServerId(1L);
        request.setPrimitiveType("TOOL");
        request.setName("test-tool");
        request.setCategory("utilities");
        request.setUriTemplate("resource/{id}");
        request.setTimeoutMs(1000);
        request.setMaxOutputSize(4096);
        return request;
    }
}