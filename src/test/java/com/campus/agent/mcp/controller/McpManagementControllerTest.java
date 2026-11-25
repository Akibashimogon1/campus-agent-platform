package com.campus.agent.mcp.controller;

import com.campus.agent.mcp.api.McpApiResponse;
import com.campus.agent.mcp.api.McpConnectivityTestResponse;
import com.campus.agent.mcp.api.McpInvocationResponse;
import com.campus.agent.mcp.api.McpPageResponse;
import com.campus.agent.mcp.api.McpServerResponse;
import com.campus.agent.mcp.api.McpToolResponse;
import com.campus.agent.mcp.api.McpToolStatusRequest;
import com.campus.agent.mcp.dto.McpInvocationQueryRequest;
import com.campus.agent.mcp.dto.McpServerQueryRequest;
import com.campus.agent.mcp.dto.McpServerRegisterRequest;
import com.campus.agent.mcp.dto.McpServerUpdateRequest;
import com.campus.agent.mcp.dto.McpToolQueryRequest;
import com.campus.agent.mcp.service.McpManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class McpManagementControllerTest {

    private final RecordingManagementService managementService = new RecordingManagementService();
    private final StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
    private McpManagementController controller;

    @BeforeEach
    void setUp() {
        beanFactory.addBean("mcpManagementService", managementService);
        ObjectProvider<McpManagementService> serviceProvider = beanFactory.getBeanProvider(McpManagementService.class);
        controller = new McpManagementController(serviceProvider);
    }

    @Test
    void registersServerForTenantFromRequestContext() {
        McpServerRegisterRequest request = new McpServerRegisterRequest();
        McpServerResponse server = new McpServerResponse(1L, "server-a", "Server A", "HTTP_SSE",
                "https://server.example", "REGISTERED", "UNKNOWN");
        managementService.serverResponse = server;

        var response = controller.registerServer(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(server);
        assertThat(managementService.registerRequest).isSameAs(request);
    }

    @Test
    void queriesToolsWithTenantFromRequestContext() {
        McpToolQueryRequest request = new McpToolQueryRequest();
        McpPageResponse<McpToolResponse> page = new McpPageResponse<>(List.of(), 1, 20, 0);
        managementService.toolPage = page;

        McpApiResponse<McpPageResponse<McpToolResponse>> response = controller.queryTools(request);

        assertThat(response.data()).isEqualTo(page);
        assertThat(managementService.queryRequest).isSameAs(request);
    }

    @Test
    void runsConnectivityTestForTenantScopedServer() {
        McpConnectivityTestResponse result = new McpConnectivityTestResponse(
                true, "CONNECTED", "Connection succeeded", 12L, LocalDateTime.now());
        managementService.connectivityResponse = result;

        McpApiResponse<McpConnectivityTestResponse> response = controller.testConnectivity(7L);
        assertThat(response.data()).isEqualTo(result);
        assertThat(managementService.serverId).isEqualTo(7L);
    }

    @Test
    void updatesToolStatusForTenantFromRequestContext() {
        McpToolStatusRequest request = new McpToolStatusRequest("ENABLED");
        McpToolResponse tool = new McpToolResponse(3L, 7L, "search", "Search", null,
                "utility", "global", "READ", "ENABLED", 1);
        managementService.toolResponse = tool;

        McpApiResponse<McpToolResponse> response = controller.updateToolStatus(3L, request);

        assertThat(response.data()).isEqualTo(tool);
        assertThat(managementService.toolId).isEqualTo(3L);
        assertThat(managementService.statusRequest).isSameAs(request);
    }

    private static final class RecordingManagementService implements McpManagementService {

        private Long serverId;
        private Long toolId;
        private McpServerRegisterRequest registerRequest;
        private McpToolQueryRequest queryRequest;
        private McpToolStatusRequest statusRequest;
        private McpServerResponse serverResponse;
        private McpPageResponse<McpToolResponse> toolPage;
        private McpConnectivityTestResponse connectivityResponse;
        private McpToolResponse toolResponse;
        private McpInvocationResponse invocationResponse;

        @Override
        public McpServerResponse registerServer(McpServerRegisterRequest request) {
            registerRequest = request;
            return serverResponse;
        }

        @Override
        public McpPageResponse<McpServerResponse> queryServers(McpServerQueryRequest request) {
            return new McpPageResponse<>(List.of(), 1, 20, 0);
        }

        @Override
        public McpServerResponse getServer(Long serverId) {
            this.serverId = serverId;
            return serverResponse;
        }

        @Override
        public McpServerResponse updateServer(Long serverId, McpServerUpdateRequest request) {
            this.serverId = serverId;
            return serverResponse;
        }

        @Override
        public void deleteServer(Long serverId) {
            this.serverId = serverId;
        }

        @Override
        public McpPageResponse<McpToolResponse> queryTools(McpToolQueryRequest request) {
            queryRequest = request;
            return toolPage;
        }

        @Override
        public McpToolResponse getTool(Long toolId) {
            this.toolId = toolId;
            return toolResponse;
        }

        @Override
        public McpConnectivityTestResponse testServerConnectivity(Long serverId) {
            this.serverId = serverId;
            return connectivityResponse;
        }

        @Override
        public McpToolResponse updateToolStatus(Long toolId, McpToolStatusRequest request) {
            this.toolId = toolId;
            statusRequest = request;
            return toolResponse;
        }

        @Override
        public McpPageResponse<McpInvocationResponse> queryInvocations(McpInvocationQueryRequest request) {
            return new McpPageResponse<>(List.of(), 1, 20, 0);
        }
    }

}