package com.campus.agent.mcp.context;

import com.campus.agent.mcp.error.McpBusinessException;
import com.campus.agent.mcp.error.McpErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextTest {

    @AfterEach
    void clearTenantContext() {
        new TenantContext().clear();
    }

    @Test
    void storesAndTrimsTenantId() {
        TenantContext tenantContext = new TenantContext();
        tenantContext.setTenantId(" tenant-a ");

        assertThat(tenantContext.requireTenantId()).isEqualTo("tenant-a");
    }

    @Test
    void rejectsMissingTenantId() {
        TenantContext tenantContext = new TenantContext();

        assertThatThrownBy(tenantContext::requireTenantId)
                .isInstanceOf(McpBusinessException.class)
                .extracting(exception -> ((McpBusinessException) exception).getErrorCode())
                .isEqualTo(McpErrorCode.TENANT_CONTEXT_MISSING);
    }

    @Test
    void clearRemovesTenantIdFromCurrentThread() {
        TenantContext tenantContext = new TenantContext();
        tenantContext.setTenantId("tenant-a");

        tenantContext.clear();

        assertThatThrownBy(tenantContext::requireTenantId)
                .isInstanceOf(McpBusinessException.class);
    }
}