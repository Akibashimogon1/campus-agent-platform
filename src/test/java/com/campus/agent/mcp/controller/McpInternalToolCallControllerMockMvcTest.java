package com.campus.agent.mcp.controller;

import com.campus.agent.mcp.context.TenantContext;
import com.campus.agent.mcp.context.TenantContextInterceptor;
import com.campus.agent.mcp.config.McpInternalServiceKeyInterceptor;
import com.campus.agent.mcp.dto.McpCallResponse;
import com.campus.agent.mcp.dto.McpToolCallRequest;
import com.campus.agent.mcp.error.McpGlobalExceptionHandler;
import com.campus.agent.mcp.service.McpInvocationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class McpInternalToolCallControllerMockMvcTest {

    private static final String INTERNAL_KEY = "internal-test-key";

    private final TenantContext tenantContext = new TenantContext();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RecordingInvocationService invocationService = new RecordingInvocationService(tenantContext);
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("mcpInvocationService", invocationService);
        ObjectProvider<McpInvocationService> serviceProvider = beanFactory.getBeanProvider(McpInvocationService.class);
        McpInternalToolCallController controller = new McpInternalToolCallController(serviceProvider);
        mockMvc = standaloneSetup(controller)
                .addInterceptors(
                        new TenantContextInterceptor(tenantContext),
                        new McpInternalServiceKeyInterceptor(INTERNAL_KEY, true))
                .setControllerAdvice(new McpGlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clearTenant() {
        tenantContext.clear();
    }

    @Test
    void acceptsInternalCallAndReturnsExistingResponseFormat() throws Exception {
        mockMvc.perform(post("/internal/v1/mcp/tools/calls")
                        .header(McpInternalServiceKeyInterceptor.HEADER_NAME, INTERNAL_KEY)
                        .requestAttr(TenantContext.TENANT_ID_ATTRIBUTE, "tenant-a")
                        .contentType("application/json")
                        .content("""
                                {"workflowId":"workflow-a","traceId":"trace-a","toolName":"search","arguments":{"q":"orders"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.traceId").value("trace-a"));
    }

    @Test
    void rejectsMissingInternalServiceKeyWithUnifiedErrorBody() throws Exception {
        mockMvc.perform(post("/internal/v1/mcp/tools/calls")
                        .requestAttr(TenantContext.TENANT_ID_ATTRIBUTE, "tenant-a")
                        .contentType("application/json")
                        .content("{\"traceId\":\"trace-a\",\"toolName\":\"search\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVICE_AUTH_FAILED"));
    }

    @Test
    void rejectsMissingTenantContextAfterInternalAuthentication() throws Exception {
        mockMvc.perform(post("/internal/v1/mcp/tools/calls")
                        .header(McpInternalServiceKeyInterceptor.HEADER_NAME, INTERNAL_KEY)
                        .contentType("application/json")
                        .content("{\"traceId\":\"trace-a\",\"toolName\":\"search\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TENANT_CONTEXT_MISSING"));
    }

    private static final class RecordingInvocationService implements McpInvocationService {

        private final TenantContext tenantContext;

        private RecordingInvocationService(TenantContext tenantContext) {
            this.tenantContext = tenantContext;
        }

        @Override
        public McpCallResponse callTool(com.campus.agent.mcp.api.McpToolCallApiRequest apiRequest) {
            if (!"tenant-a".equals(tenantContext.requireTenantId())) {
                throw new AssertionError("Tenant interceptor did not populate context");
            }
            McpToolCallRequest businessRequest = apiRequest.toBusinessRequest(tenantContext.requireTenantId());
            McpCallResponse response = new McpCallResponse();
            response.setStatus("SUCCEEDED");
            response.setTraceId(businessRequest.getTraceId());
            return response;
        }
    }
}
