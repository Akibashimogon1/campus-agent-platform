package com.campus.agent.mcp.api;

import com.campus.agent.mcp.dto.McpToolQueryRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class McpApiRequestValidationTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void acceptsSupportedToolStatus() {
        assertThat(VALIDATOR.validate(new McpToolStatusRequest("ENABLED"))).isEmpty();
        assertThat(VALIDATOR.validate(new McpToolStatusRequest("DISABLED"))).isEmpty();
    }

    @Test
    void rejectsUnsupportedToolStatus() {
        Set<ConstraintViolation<McpToolStatusRequest>> violations =
                VALIDATOR.validate(new McpToolStatusRequest("PENDING"));

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("status");
    }

    @Test
    void enforcesQueryPageAndSizeBounds() {
        McpToolQueryRequest request = new McpToolQueryRequest();
        request.setPage(0);
        request.setSize(101);

        Set<ConstraintViolation<McpToolQueryRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("page", "size");
    }
}