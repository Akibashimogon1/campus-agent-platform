package com.campus.agent.mcp.controller;

import com.campus.agent.mcp.api.McpConnectivityTestResponse;
import com.campus.agent.mcp.api.McpInvocationResponse;
import com.campus.agent.mcp.api.McpPageResponse;
import com.campus.agent.mcp.api.McpServerResponse;
import com.campus.agent.mcp.api.McpToolResponse;
import com.campus.agent.mcp.api.McpToolStatusRequest;
import com.campus.agent.mcp.context.TenantContext;
import com.campus.agent.mcp.context.TenantContextInterceptor;
import com.campus.agent.mcp.dto.McpInvocationQueryRequest;
import com.campus.agent.mcp.dto.McpServerQueryRequest;
import com.campus.agent.mcp.dto.McpServerRegisterRequest;
import com.campus.agent.mcp.dto.McpServerUpdateRequest;
import com.campus.agent.mcp.dto.McpToolQueryRequest;
import com.campus.agent.mcp.error.McpGlobalExceptionHandler;
import com.campus.agent.mcp.service.McpManagementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class McpManagementControllerMockMvcTest {

    private final TenantContext tenantContext = new TenantContext();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RecordingManagementService service = new RecordingManagementService(tenantContext);
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("mcpManagementService", service);
        ObjectProvider<McpManagementService> serviceProvider = beanFactory.getBeanProvider(McpManagementService.class);
        McpManagementController controller = new McpManagementController(serviceProvider);
        mockMvc = standaloneSetup(controller)
                .addInterceptors(new TenantContextInterceptor(tenantContext))
                .setControllerAdvice(new McpGlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clearTenant() {
        tenantContext.clear();
    }

    @Test
    void getsServerDetailWithTenantContext() throws Exception {
        mockMvc.perform(get("/api/v1/mcp/servers/41")
                        .requestAttr(TenantContext.TENANT_ID_ATTRIBUTE, "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(41));
    }

    @Test
    void registersServerThroughValidatedManagementRoute() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/mcp/servers")
                        .requestAttr(TenantContext.TENANT_ID_ATTRIBUTE, "tenant-a")
                        .contentType("application/json")
                        .content("""
                                {"serverCode":"orders","serverName":"Orders","transportType":"HTTP_SSE","endpoint":"https://mcp.example","status":"REGISTERED"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(41));
    }

    @Test
    void updatesServerConfigurationWithValidatedBody() throws Exception {
        mockMvc.perform(put("/api/v1/mcp/servers/41")
                        .requestAttr(TenantContext.TENANT_ID_ATTRIBUTE, "tenant-a")
                        .contentType("application/json")
                        .content("""
                                {"serverName":"Orders","transportType":"HTTP_SSE","endpoint":"https://mcp.example","status":"ENABLED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.serverName").value("Orders"));
    }

    @Test
    void deletesServerLogicallyThroughManagementService() throws Exception {
        mockMvc.perform(delete("/api/v1/mcp/servers/41")
                        .requestAttr(TenantContext.TENANT_ID_ATTRIBUTE, "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void getsToolDetailForTenant() throws Exception {
        mockMvc.perform(get("/api/v1/mcp/tools/52")
                        .requestAttr(TenantContext.TENANT_ID_ATTRIBUTE, "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(52));
    }

    @Test
    void queriesInvocationPageForTenant() throws Exception {
        mockMvc.perform(get("/api/v1/mcp/invocations?page=1&size=20")
                        .requestAttr(TenantContext.TENANT_ID_ATTRIBUTE, "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1));
    }

    @Test
    void queriesToolsThroughManagementRoute() throws Exception {
        mockMvc.perform(get("/api/v1/mcp/tools?page=1&size=20")
                        .requestAttr(TenantContext.TENANT_ID_ATTRIBUTE, "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void runsConnectivityTestThroughManagementRoute() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/mcp/servers/41/connectivity-tests")
                        .requestAttr(TenantContext.TENANT_ID_ATTRIBUTE, "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.connected").value(true));
    }

    @Test
    void changesToolStatusThroughManagementRoute() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/mcp/tools/52/status")
                        .requestAttr(TenantContext.TENANT_ID_ATTRIBUTE, "tenant-a")
                        .contentType("application/json")
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENABLED"));
    }

    @Test
    void invalidPaginationUsesUnifiedValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/mcp/tools?page=0&size=101")
                        .requestAttr(TenantContext.TENANT_ID_ATTRIBUTE, "tenant-a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private static final class RecordingManagementService implements McpManagementService {

        private final TenantContext tenantContext;

        private RecordingManagementService(TenantContext tenantContext) {
            this.tenantContext = tenantContext;
        }

        private void assertTenant() {
            if (!"tenant-a".equals(tenantContext.requireTenantId())) {
                throw new AssertionError("Tenant interceptor did not populate context");
            }
        }

        @Override
        public McpServerResponse registerServer(McpServerRegisterRequest request) {
            assertTenant();
            return server(41L);
        }

        @Override
        public McpPageResponse<McpServerResponse> queryServers(McpServerQueryRequest request) {
            assertTenant();
            return new McpPageResponse<>(List.of(), 1, 20, 0);
        }

        @Override
        public McpServerResponse getServer(Long serverId) {
            assertTenant();
            return server(serverId);
        }

        @Override
        public McpServerResponse updateServer(Long serverId, McpServerUpdateRequest request) {
            assertTenant();
            return server(serverId);
        }

        @Override
        public void deleteServer(Long serverId) {
            assertTenant();
        }

        @Override
        public McpPageResponse<McpToolResponse> queryTools(McpToolQueryRequest request) {
            assertTenant();
            return new McpPageResponse<>(List.of(), 1, 20, 0);
        }

        @Override
        public McpToolResponse getTool(Long toolId) {
            assertTenant();
            return tool(toolId);
        }

        @Override
        public McpConnectivityTestResponse testServerConnectivity(Long serverId) {
            assertTenant();
            return new McpConnectivityTestResponse(true, "CONNECTED", "ok", 1L, LocalDateTime.now());
        }

        @Override
        public McpToolResponse updateToolStatus(Long toolId, McpToolStatusRequest request) {
            assertTenant();
            return tool(toolId);
        }

        @Override
        public McpPageResponse<McpInvocationResponse> queryInvocations(McpInvocationQueryRequest request) {
            assertTenant();
            return new McpPageResponse<>(List.of(), 1, 20, 0);
        }

        private McpServerResponse server(Long id) {
            return new McpServerResponse(id, "orders", "Orders", "HTTP_SSE", "https://mcp.example",
                    "ENABLED", "HEALTHY");
        }

        private McpToolResponse tool(Long id) {
            return new McpToolResponse(id, 41L, "search", "Search", null,
                    "GENERAL", "WORKFLOW", "TENANT", "ENABLED", 1);
        }
    }
}
