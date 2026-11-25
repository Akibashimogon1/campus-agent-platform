package com.campus.agent.mcp.context;

import com.campus.agent.mcp.error.McpBusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextInterceptorTest {

    @Test
    void injectsRequestAttributeAndClearsItAfterCompletion() throws Exception {
        TenantContext tenantContext = new TenantContext();
        TenantContextInterceptor interceptor = new TenantContextInterceptor(tenantContext);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(TenantContext.TENANT_ID_ATTRIBUTE, "tenant-a");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(tenantContext.requireTenantId()).isEqualTo("tenant-a");

        interceptor.afterCompletion(request, response, new Object(), new IllegalStateException("test"));

        assertThatThrownBy(tenantContext::requireTenantId).isInstanceOf(McpBusinessException.class);
    }

    @Test
    void missingAttributeContinuesWithoutTenantAndClearsPreviousValue() throws Exception {
        TenantContext tenantContext = new TenantContext();
        TenantContextInterceptor interceptor = new TenantContextInterceptor(tenantContext);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        tenantContext.setTenantId("stale-tenant");

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        assertThatThrownBy(tenantContext::requireTenantId).isInstanceOf(McpBusinessException.class);

        interceptor.afterCompletion(request, response, new Object(), null);
        assertThatThrownBy(tenantContext::requireTenantId).isInstanceOf(McpBusinessException.class);
    }
}