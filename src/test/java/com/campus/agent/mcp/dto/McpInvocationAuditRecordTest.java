package com.campus.agent.mcp.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class McpInvocationAuditRecordTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void acceptsValidAuditRecord() {
        assertThat(VALIDATOR.validate(validRecord())).isEmpty();
    }

    @Test
    void rejectsBlankRequiredAuditFields() {
        McpInvocationAuditRecord record = validRecord();
        record.setTenantId(" ");
        record.setPrimitiveType("");
        record.setPrimitiveName(" ");
        record.setStatus("");

        Set<ConstraintViolation<McpInvocationAuditRecord>> violations = VALIDATOR.validate(record);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("tenantId", "primitiveType", "primitiveName", "status");
    }

    @Test
    void rejectsAuditValuesOverDeclaredMaximumLengths() {
        McpInvocationAuditRecord record = validRecord();
        record.setTraceId("t".repeat(65));
        record.setErrorCode("e".repeat(65));

        Set<ConstraintViolation<McpInvocationAuditRecord>> violations = VALIDATOR.validate(record);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("traceId", "errorCode");
    }

    private static McpInvocationAuditRecord validRecord() {
        McpInvocationAuditRecord record = new McpInvocationAuditRecord();
        // These values satisfy the DTO's declared required-field constraints.
        record.setTenantId("test-tenant");
        record.setTraceId("test-trace");
        record.setPrimitiveType("TOOL");
        record.setPrimitiveName("test-tool");
        record.setStatus("SUCCEEDED");
        return record;
    }
}