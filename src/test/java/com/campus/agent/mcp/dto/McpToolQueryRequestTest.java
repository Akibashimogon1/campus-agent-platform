package com.campus.agent.mcp.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class McpToolQueryRequestTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void initializesDefaultPagination() {
        McpToolQueryRequest request = new McpToolQueryRequest();

        assertThat(request.getPage()).isEqualTo(1);
        assertThat(request.getSize()).isEqualTo(20);
        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @Test
    void acceptsStringsAtTheirMaximumLengths() {
        McpToolQueryRequest request = new McpToolQueryRequest();
        request.setCategory("c".repeat(64));
        request.setScope("s".repeat(32));
        request.setStatus("t".repeat(32));
        request.setKeyword("k".repeat(128));

        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @Test
    void rejectsStringsOverTheirMaximumLengths() {
        McpToolQueryRequest request = new McpToolQueryRequest();
        request.setCategory("c".repeat(65));
        request.setScope("s".repeat(33));
        request.setStatus("t".repeat(33));
        request.setKeyword("k".repeat(129));

        Set<ConstraintViolation<McpToolQueryRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("category", "scope", "status", "keyword");
    }
}