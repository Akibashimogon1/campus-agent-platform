package com.campus.agent.mcp.context;

import com.campus.agent.mcp.error.McpBusinessException;
import com.campus.agent.mcp.error.McpErrorCode;
import org.springframework.stereotype.Component;

@Component
public class TenantContext {

    public static final String TENANT_ID_ATTRIBUTE = "tenantId";

    private static final ThreadLocal<String> TENANT_ID_HOLDER = new ThreadLocal<>();

    public void setTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            TENANT_ID_HOLDER.remove();
            return;
        }
        TENANT_ID_HOLDER.set(tenantId.trim());
    }

    public String requireTenantId() {
        String tenantId = TENANT_ID_HOLDER.get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new McpBusinessException(McpErrorCode.TENANT_CONTEXT_MISSING,
                    "Tenant context was not provided by the trusted gateway");
        }
        return tenantId;
    }

    public void clear() {
        TENANT_ID_HOLDER.remove();
    }
}