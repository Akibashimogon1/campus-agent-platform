package com.campus.agent.mcp.context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantContextInterceptor implements HandlerInterceptor {

    public static final String DEV_TENANT_HEADER = "X-Tenant-ID";

    private final TenantContext tenantContext;
    private final boolean allowDevTenantHeader;

    public TenantContextInterceptor(TenantContext tenantContext) {
        this(tenantContext, false);
    }

    @Autowired
    public TenantContextInterceptor(
            TenantContext tenantContext,
            @Value("${mcp.dev.allow-tenant-header:false}") boolean allowDevTenantHeader) {
        this.tenantContext = tenantContext;
        this.allowDevTenantHeader = allowDevTenantHeader;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        tenantContext.clear();
        Object tenantId = request.getAttribute(TenantContext.TENANT_ID_ATTRIBUTE);
        if (tenantId instanceof String value && !value.isBlank()) {
            tenantContext.setTenantId(value);
        } else if (allowDevTenantHeader) {
            String devTenantId = request.getHeader(DEV_TENANT_HEADER);
            if (devTenantId != null && !devTenantId.isBlank()) {
                tenantContext.setTenantId(devTenantId);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        tenantContext.clear();
    }
}