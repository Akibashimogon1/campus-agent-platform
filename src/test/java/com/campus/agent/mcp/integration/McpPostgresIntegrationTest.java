package com.campus.agent.mcp.integration;

import com.campus.agent.mcp.api.McpToolCallApiRequest;
import com.campus.agent.mcp.context.TenantContext;
import com.campus.agent.mcp.dto.McpInvocationQueryRequest;
import com.campus.agent.mcp.dto.McpServerRegisterRequest;
import com.campus.agent.mcp.dto.McpServerUpdateRequest;
import com.campus.agent.mcp.dto.McpToolQueryRequest;
import com.campus.agent.mcp.entity.McpInvocation;
import com.campus.agent.mcp.entity.McpServer;
import com.campus.agent.mcp.entity.McpTool;
import com.campus.agent.mcp.error.McpBusinessException;
import com.campus.agent.mcp.error.McpErrorCode;
import com.campus.agent.mcp.mapper.McpInvocationMapper;
import com.campus.agent.mcp.mapper.McpServerMapper;
import com.campus.agent.mcp.mapper.McpToolMapper;
import com.campus.agent.mcp.service.McpInvocationService;
import com.campus.agent.mcp.service.McpManagementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpPostgresIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private TenantContext tenantContext;
    @Autowired
    private McpManagementService managementService;
    @Autowired
    private McpInvocationService invocationService;
    @Autowired
    private McpServerMapper serverMapper;
    @Autowired
    private McpToolMapper toolMapper;
    @Autowired
    private McpInvocationMapper invocationMapper;

    @AfterEach
    void clearTenant() {
        tenantContext.clear();
    }

    @Test
    void serverRegistrationIsTenantScopedAndDuplicateCodeIsRejected() {
        tenantContext.setTenantId("tenant-a");
        McpServerRegisterRequest request = registerRequest("orders-mcp");
        var registered = managementService.registerServer(request);
        assertThat(registered.serverCode()).isEqualTo("orders-mcp");

        assertThatThrownBy(() -> managementService.registerServer(request))
                .isInstanceOf(McpBusinessException.class)
                .extracting("errorCode")
                .isEqualTo(McpErrorCode.RESOURCE_CONFLICT);

        tenantContext.setTenantId("tenant-b");
        assertThat(managementService.registerServer(request).serverCode()).isEqualTo("orders-mcp");

        tenantContext.setTenantId("tenant-a");
        assertThat(managementService.getServer(registered.id()).serverCode()).isEqualTo("orders-mcp");
    }

    @Test
    void serverCrudUpdateAndLogicalCascadeDeletionAreTenantScoped() {
        tenantContext.setTenantId("tenant-a");
        var server = managementService.registerServer(registerRequest("crud-mcp"));
        McpTool tool = enabledTool("crud-mcp", server.id(), "search");
        toolMapper.insert(tool);

        McpServerUpdateRequest update = new McpServerUpdateRequest();
        update.setServerName("Updated MCP");
        update.setTransportType("HTTP_SSE");
        update.setEndpoint("https://example.test/mcp");
        update.setStatus("ENABLED");
        assertThat(managementService.updateServer(server.id(), update).serverName()).isEqualTo("Updated MCP");

        managementService.deleteServer(server.id());
        assertThat(serverMapper.selectByTenantAndId("tenant-a", server.id())).isNull();
        assertThat(toolMapper.selectByTenantAndId("tenant-a", tool.getId())).isNull();

        tenantContext.setTenantId("tenant-b");
        assertThatThrownBy(() -> managementService.getServer(server.id()))
                .isInstanceOf(McpBusinessException.class)
                .extracting("errorCode")
                .isEqualTo(McpErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void toolQueriesAndPaginationContainOnlyCurrentTenantRecords() {
        tenantContext.setTenantId("tenant-a");
        var serverA = managementService.registerServer(registerRequest("tenant-a-mcp"));
        toolMapper.insert(enabledTool("tenant-a-mcp", serverA.id(), "search-a"));

        tenantContext.setTenantId("tenant-b");
        var serverB = managementService.registerServer(registerRequest("tenant-b-mcp"));
        toolMapper.insert(enabledTool("tenant-b-mcp", serverB.id(), "search-b"));

        tenantContext.setTenantId("tenant-a");
        McpToolQueryRequest query = new McpToolQueryRequest();
        query.setPage(1);
        query.setSize(20);
        var page = managementService.queryTools(query);
        assertThat(page.records()).extracting("toolName").containsExactly("search-a");
        assertThat(page.total()).isEqualTo(1);

        tenantContext.setTenantId("tenant-b");
        var tenantBPage = managementService.queryTools(query);
        assertThat(tenantBPage.records()).extracting("toolName").containsExactly("search-b");
    }

    @Test
    void invocationInsertPersistsSuccessAndFailureRecordsTenantSafely() {
        tenantContext.setTenantId("tenant-a");
        var server = managementService.registerServer(registerRequest("audit-mcp"));
        McpTool tool = enabledTool("audit-mcp", server.id(), "audit-tool");
        toolMapper.insert(tool);

        LocalDateTime now = LocalDateTime.now();
        McpInvocation succeeded = invocation("tenant-a", server.id(), tool.getId(), "SUCCEEDED", "trace-ok", now);
        McpInvocation failed = invocation("tenant-a", server.id(), tool.getId(), "FAILED", "trace-fail", now.minusMinutes(1));
        failed.setErrorCode("DOWNSTREAM_ERROR");
        invocationMapper.insert(succeeded);
        invocationMapper.insert(failed);

        tenantContext.setTenantId("tenant-b");
        assertThat(invocationMapper.selectByTenantAndTrace("tenant-b", "trace-ok")).isEmpty();

        tenantContext.setTenantId("tenant-a");
        McpInvocationQueryRequest query = new McpInvocationQueryRequest();
        query.setPage(1);
        query.setSize(20);
        var page = managementService.queryInvocations(query);
        assertThat(page.records()).hasSize(2);
        assertThat(page.records()).extracting("traceId").containsExactlyInAnyOrder("trace-ok", "trace-fail");
    }

    @Test
    void toolAmbiguityRequiresServerIdAndServerIdRoutesOneTool() {
        tenantContext.setTenantId("tenant-a");
        var serverA = managementService.registerServer(registerRequest("server-a"));
        var serverB = managementService.registerServer(registerRequest("server-b"));
        toolMapper.insert(enabledTool("server-a", serverA.id(), "duplicate"));
        toolMapper.insert(enabledTool("server-b", serverB.id(), "duplicate"));

        McpToolCallApiRequest request = callRequest("duplicate", null);
        assertThatThrownBy(() -> invocationService.callTool(request))
                .isInstanceOf(McpBusinessException.class)
                .extracting("errorCode")
                .isEqualTo(McpErrorCode.RESOURCE_CONFLICT);

        McpToolCallApiRequest routed = callRequest("duplicate", serverA.id());
        routed.setTraceId("trace-routed");
        assertThat(invocationService.callTool(routed).getPrimitiveName()).isEqualTo("duplicate");

        List<McpInvocation> records = invocationMapper.selectByTenantAndTrace("tenant-a", "trace-routed");
        assertThat(records).singleElement().extracting(McpInvocation::getServerId).isEqualTo(serverA.id());
    }

    private McpServerRegisterRequest registerRequest(String code) {
        McpServerRegisterRequest request = new McpServerRegisterRequest();
        request.setServerCode(code);
        request.setServerName(code);
        request.setTransportType("HTTP_SSE");
        request.setEndpoint("https://example.test/mcp");
        request.setStatus("ENABLED");
        return request;
    }

    private McpTool enabledTool(String code, Long serverId, String name) {
        McpTool tool = new McpTool();
        tool.setTenantId(code.startsWith("tenant-b") ? "tenant-b" : tenantContext.requireTenantId());
        tool.setServerId(serverId);
        tool.setToolName(name);
        tool.setDisplayName(name);
        tool.setStatus("ENABLED");
        tool.setVersion(1);
        tool.setCategory("GENERAL");
        tool.setScope("WORKFLOW");
        return tool;
    }

    private McpToolCallApiRequest callRequest(String name, Long serverId) {
        McpToolCallApiRequest request = new McpToolCallApiRequest();
        request.setTraceId("trace-" + name);
        request.setToolName(name);
        request.setServerId(serverId);
        request.setArguments(Map.of("q", "test"));
        return request;
    }

    private McpInvocation invocation(String tenantId, Long serverId, Long toolId,
                                     String status, String traceId, LocalDateTime startedAt) {
        McpInvocation invocation = new McpInvocation();
        invocation.setTenantId(tenantId);
        invocation.setServerId(serverId);
        invocation.setToolId(toolId);
        invocation.setTraceId(traceId);
        invocation.setPrimitiveType("TOOL");
        invocation.setPrimitiveName("audit-tool");
        invocation.setStatus(status);
        invocation.setRequestSummary(Map.of("argumentCount", 0));
        invocation.setResponseSummary(Map.of("status", status));
        invocation.setLatencyMs(1L);
        invocation.setStartedAt(startedAt);
        invocation.setFinishedAt(startedAt.plusNanos(1_000_000));
        return invocation;
    }
}
