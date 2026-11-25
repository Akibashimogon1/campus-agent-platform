package com.campus.agent.mcp.service;

import com.campus.agent.mcp.api.McpToolCallApiRequest;
import com.campus.agent.mcp.context.TenantContext;
import com.campus.agent.mcp.dto.McpCallResponse;
import com.campus.agent.mcp.dto.McpToolCallRequest;
import com.campus.agent.mcp.entity.McpInvocation;
import com.campus.agent.mcp.entity.McpServer;
import com.campus.agent.mcp.entity.McpTool;
import com.campus.agent.mcp.error.McpBusinessException;
import com.campus.agent.mcp.error.McpErrorCode;
import com.campus.agent.mcp.mapper.McpInvocationMapper;
import com.campus.agent.mcp.mapper.McpServerMapper;
import com.campus.agent.mcp.mapper.McpToolMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class McpInvocationServiceImpl implements McpInvocationService {

    private final McpToolMapper toolMapper;
    private final McpServerMapper serverMapper;
    private final McpInvocationMapper invocationMapper;
    private final TenantContext tenantContext;
    private final McpToolExecutionClient executionClient;
    private final McpInvocationAuditWriter auditWriter;

    public McpInvocationServiceImpl(
            McpToolMapper toolMapper,
            McpServerMapper serverMapper,
            McpInvocationMapper invocationMapper,
            TenantContext tenantContext,
            McpToolExecutionClient executionClient) {
        this(toolMapper, serverMapper, invocationMapper, tenantContext, executionClient, null);
    }

    @Autowired
    public McpInvocationServiceImpl(
            McpToolMapper toolMapper,
            McpServerMapper serverMapper,
            McpInvocationMapper invocationMapper,
            TenantContext tenantContext,
            McpToolExecutionClient executionClient,
            McpInvocationAuditWriter auditWriter) {
        this.toolMapper = toolMapper;
        this.serverMapper = serverMapper;
        this.invocationMapper = invocationMapper;
        this.tenantContext = tenantContext;
        this.executionClient = executionClient;
        this.auditWriter = auditWriter;
    }

    @Override
    public McpCallResponse callTool(McpToolCallApiRequest apiRequest) {
        String tenantId = tenantContext.requireTenantId();
        McpTool tool;
        if (apiRequest.getServerId() != null) {
            tool = toolMapper.selectByTenantServerAndName(
                    tenantId, apiRequest.getServerId(), apiRequest.getToolName());
            if (tool == null) {
                throw new McpBusinessException(McpErrorCode.RESOURCE_NOT_FOUND,
                        "Enabled MCP Tool not found on the specified server");
            }
        } else {
            List<McpTool> toolCandidates = toolMapper.selectByTenantAndName(tenantId, apiRequest.getToolName());
            if (toolCandidates == null || toolCandidates.isEmpty()) {
                throw new McpBusinessException(McpErrorCode.RESOURCE_NOT_FOUND,
                        "Enabled MCP Tool not found for this tenant");
            }
            if (toolCandidates.size() > 1) {
                throw new McpBusinessException(McpErrorCode.RESOURCE_CONFLICT,
                        "Tool name is ambiguous for this tenant; specify serverId");
            }
            tool = toolCandidates.get(0);
        }
        if (!"ENABLED".equals(tool.getStatus())) {
            throw new McpBusinessException(McpErrorCode.RESOURCE_NOT_FOUND,
                "Enabled MCP Tool not found for this tenant");
        }
        McpServer server = serverMapper.selectByTenantAndId(tenantId, tool.getServerId());
        if (server == null || !"ENABLED".equals(server.getStatus())) {
            throw new McpBusinessException(McpErrorCode.SERVICE_UNAVAILABLE,
                    "MCP Server is unavailable");
        }

        McpToolCallRequest request = apiRequest.toBusinessRequest(tenantId);
        LocalDateTime startedAt = LocalDateTime.now();
        long startedNanos = System.nanoTime();
        McpInvocation invocation = newInvocation(tenantId, server, tool, request, startedAt);
        McpCallResponse response;
        try {
            if (apiRequest.getToolVersion() != null && !apiRequest.getToolVersion().equals(tool.getVersion())) {
                throw new McpBusinessException(McpErrorCode.RESOURCE_NOT_FOUND,
                        "Requested MCP Tool version is not available");
            }
            response = executionClient.call(server, tool, request);
            if (response == null || !"SUCCEEDED".equals(response.getStatus())) {
                throw new IllegalStateException("Downstream MCP tool call returned an unsuccessful response");
            }
        } catch (Exception exception) {
            McpErrorCode errorCode = exception instanceof McpBusinessException businessException
                    ? businessException.getErrorCode()
                    : McpErrorCode.DOWNSTREAM_ERROR;
            failInvocation(invocation, errorCode.name(), startedNanos);
            saveAudit(invocation);
            if (exception instanceof McpBusinessException businessException) {
                throw businessException;
            }
            throw new McpBusinessException(McpErrorCode.DOWNSTREAM_ERROR,
                    "Downstream MCP tool call failed");
        }

        completeInvocation(invocation, response, startedNanos);
        response.setTraceId(request.getTraceId());
        response.setPrimitiveType("TOOL");
        response.setPrimitiveName(tool.getToolName());
        response.setToolVersion(tool.getVersion());
        response.setServerVersion(server.getVersion());
        response.setLatencyMs(invocation.getLatencyMs());
        saveAudit(invocation);
        return response;
    }

    private void saveAudit(McpInvocation invocation) {
        if (auditWriter != null) {
            auditWriter.save(invocation);
            return;
        }
        if (invocationMapper.insert(invocation) != 1) {
            throw new McpBusinessException(McpErrorCode.BUSINESS_ERROR,
                    "MCP invocation audit record could not be saved");
        }
    }

    private McpInvocation newInvocation(
            String tenantId, McpServer server, McpTool tool, McpToolCallRequest request, LocalDateTime startedAt) {
        McpInvocation invocation = new McpInvocation();
        invocation.setTenantId(tenantId);
        invocation.setServerId(server.getId());
        invocation.setToolId(tool.getId());
        invocation.setWorkflowId(request.getWorkflowId());
        invocation.setRunId(request.getRunId());
        invocation.setNodeId(request.getNodeId());
        invocation.setSessionId(request.getSessionId());
        invocation.setTraceId(request.getTraceId());
        invocation.setPrimitiveType("TOOL");
        invocation.setPrimitiveName(tool.getToolName());
        invocation.setPrimitiveVersion(tool.getVersion());
        invocation.setStatus("RUNNING");
        invocation.setStartedAt(startedAt);
        Map<String, Object> summary = new HashMap<>();
        summary.put("toolName", tool.getToolName());
        summary.put("argumentCount", request.getArguments() == null ? 0 : request.getArguments().size());
        invocation.setRequestSummary(summary);
        return invocation;
    }

    private void completeInvocation(McpInvocation invocation, McpCallResponse response, long startedNanos) {
        invocation.setStatus("SUCCEEDED");
        invocation.setFinishedAt(LocalDateTime.now());
        invocation.setLatencyMs((System.nanoTime() - startedNanos) / 1_000_000);
        Map<String, Object> summary = new HashMap<>();
        summary.put("status", response.getStatus());
        summary.put("outputFieldCount", response.getOutput() == null ? 0 : response.getOutput().size());
        invocation.setResponseSummary(summary);
    }

    private void failInvocation(McpInvocation invocation, String errorCode, long startedNanos) {
        invocation.setStatus("FAILED");
        invocation.setErrorCode(errorCode);
        invocation.setFinishedAt(LocalDateTime.now());
        invocation.setLatencyMs((System.nanoTime() - startedNanos) / 1_000_000);
        invocation.setResponseSummary(Map.of("status", "FAILED", "errorCode", errorCode));
    }
}