package com.campus.agent.mcp.service;

import com.campus.agent.mcp.cache.McpQueryCache;
import com.campus.agent.mcp.api.McpConnectivityTestResponse;
import com.campus.agent.mcp.api.McpInvocationResponse;
import com.campus.agent.mcp.api.McpPageResponse;
import com.campus.agent.mcp.api.McpServerResponse;
import com.campus.agent.mcp.api.McpToolResponse;
import com.campus.agent.mcp.api.McpToolStatusRequest;
import com.campus.agent.mcp.context.TenantContext;
import com.campus.agent.mcp.dto.McpInvocationQueryRequest;
import com.campus.agent.mcp.dto.McpServerQueryRequest;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class McpManagementServiceImpl implements McpManagementService {

    private final McpServerMapper serverMapper;
    private final McpToolMapper toolMapper;
    private final McpInvocationMapper invocationMapper;
    private final TenantContext tenantContext;
    private final McpConnectivityClient connectivityClient;
    private final McpQueryCache queryCache;

    public McpManagementServiceImpl(
            McpServerMapper serverMapper,
            McpToolMapper toolMapper,
            McpInvocationMapper invocationMapper,
            TenantContext tenantContext,
            McpConnectivityClient connectivityClient) {
        this(serverMapper, toolMapper, invocationMapper, tenantContext, connectivityClient, null);
    }

    @Autowired
    public McpManagementServiceImpl(
            McpServerMapper serverMapper,
            McpToolMapper toolMapper,
            McpInvocationMapper invocationMapper,
            TenantContext tenantContext,
            McpConnectivityClient connectivityClient,
            McpQueryCache queryCache) {
        this.serverMapper = serverMapper;
        this.toolMapper = toolMapper;
        this.invocationMapper = invocationMapper;
        this.tenantContext = tenantContext;
        this.connectivityClient = connectivityClient;
        this.queryCache = queryCache;
    }

    @Override
    @Transactional
    public McpServerResponse registerServer(McpServerRegisterRequest request) {
        String tenantId = tenantContext.requireTenantId();
        if (serverMapper.selectByTenantAndCode(tenantId, request.getServerCode()) != null) {
            throw new McpBusinessException(McpErrorCode.RESOURCE_CONFLICT,
                    "Server code already exists for this tenant");
        }

        McpServer server = new McpServer();
        server.setTenantId(tenantId);
        server.setServerCode(request.getServerCode());
        server.setServerName(request.getServerName());
        server.setTransportType(request.getTransportType());
        server.setEndpoint(request.getEndpoint());
        server.setProcessConfig(request.getProcessConfig());
        server.setCredentialRef(request.getCredentialRef());
        server.setStatus(request.getStatus());
        server.setHealthStatus("UNKNOWN");
        try {
            if (serverMapper.insert(server) != 1) {
                throw new McpBusinessException(McpErrorCode.BUSINESS_ERROR, "Server registration failed");
            }
        } catch (DuplicateKeyException exception) {
            throw new McpBusinessException(McpErrorCode.RESOURCE_CONFLICT,
                    "Server code already exists for this tenant");
        }
        invalidateServerCache(tenantId);
        return toServerResponse(server);
    }

    @Override
    public McpServerResponse getServer(Long serverId) {
        return toServerResponse(requireServer(tenantContext.requireTenantId(), serverId));
    }

    @Override
    public McpPageResponse<McpServerResponse> queryServers(McpServerQueryRequest request) {
        String tenantId = tenantContext.requireTenantId();
        int page = request.getPage() == null ? 1 : request.getPage();
        int size = request.getSize() == null ? 20 : request.getSize();
        long offset = (long) (page - 1) * size;
        String cacheKey = queryCache == null ? null : queryCache.serverKey(
                tenantId, request.getStatus(), request.getHealthStatus(), page, size);
        if (cacheKey != null) {
            McpPageResponse<McpServerResponse> cached = queryCache.getServer(cacheKey, McpPageResponse.class);
            if (cached != null) {
                return cached;
            }
        }
        long total = serverMapper.countByTenantQuery(tenantId, request.getStatus(), request.getHealthStatus());
        List<McpServerResponse> records = serverMapper.selectPageByTenant(
                        tenantId, request.getStatus(), request.getHealthStatus(), size, offset)
                .stream()
                .map(this::toServerResponse)
                .toList();
        McpPageResponse<McpServerResponse> response = new McpPageResponse<>(records, page, size, total);
        if (cacheKey != null) {
            queryCache.putServer(cacheKey, response);
        }
        return response;
    }

    @Override
    @Transactional
    public McpServerResponse updateServer(Long serverId, McpServerUpdateRequest request) {
        String tenantId = tenantContext.requireTenantId();
        requireServer(tenantId, serverId);
        int updated = serverMapper.updateConfigurationByTenant(
                tenantId,
                serverId,
                request.getServerName(),
                request.getTransportType(),
                request.getEndpoint(),
                request.getProcessConfig(),
                request.getCredentialRef(),
                request.getStatus());
        if (updated != 1) {
            throw new McpBusinessException(McpErrorCode.RESOURCE_NOT_FOUND, "MCP Server not found");
        }
        invalidateServerCache(tenantId);
        return toServerResponse(requireServer(tenantId, serverId));
    }

    @Override
    @Transactional
    public void deleteServer(Long serverId) {
        String tenantId = tenantContext.requireTenantId();
        requireServer(tenantId, serverId);
        toolMapper.softDeleteByServerTenant(tenantId, serverId);
        if (serverMapper.softDeleteByTenant(tenantId, serverId) != 1) {
            throw new McpBusinessException(McpErrorCode.RESOURCE_NOT_FOUND, "MCP Server not found");
        }
        invalidateServerCache(tenantId);
        invalidateToolCache(tenantId);
    }

    @Override
    public McpPageResponse<McpToolResponse> queryTools(McpToolQueryRequest request) {
        String tenantId = tenantContext.requireTenantId();
        int page = request.getPage() == null ? 1 : request.getPage();
        int size = request.getSize() == null ? 20 : request.getSize();
        long offset = (long) (page - 1) * size;
        String cacheKey = queryCache == null ? null : queryCache.toolKey(
                tenantId, request.getCategory(), request.getScope(), request.getStatus(),
                request.getKeyword(), page, size);
        if (cacheKey != null) {
            McpPageResponse<McpToolResponse> cached = queryCache.getTool(cacheKey, McpPageResponse.class);
            if (cached != null) {
                return cached;
            }
        }
        long total = toolMapper.countByTenantQuery(
                tenantId, request.getCategory(), request.getScope(), request.getStatus(), request.getKeyword());
        List<McpToolResponse> records = toolMapper.selectByTenantQuery(
                        tenantId, request.getCategory(), request.getScope(), request.getStatus(),
                        request.getKeyword(), size, offset)
                .stream()
                .map(this::toToolResponse)
                .toList();
        McpPageResponse<McpToolResponse> response = new McpPageResponse<>(records, page, size, total);
        if (cacheKey != null) {
            queryCache.putTool(cacheKey, response);
        }
        return response;
    }

    @Override
    public McpToolResponse getTool(Long toolId) {
        String tenantId = tenantContext.requireTenantId();
        McpTool tool = toolMapper.selectByTenantAndId(tenantId, toolId);
        if (tool == null) {
            throw new McpBusinessException(McpErrorCode.RESOURCE_NOT_FOUND, "MCP Tool not found");
        }
        return toToolResponse(tool);
    }

    @Override
    public McpConnectivityTestResponse testServerConnectivity(Long serverId) {
        String tenantId = tenantContext.requireTenantId();
        McpServer server = requireServer(tenantId, serverId);
        LocalDateTime testedAt = LocalDateTime.now();
        long startedNanos = System.nanoTime();
        boolean connected = false;
        String failureReason = null;
        String message;

        try {
            connectivityClient.initializeAndPing(server);
            connected = true;
            message = "MCP initialize and ping succeeded";
        } catch (Exception exception) {
            failureReason = failureReason(exception);
            message = failureReason;
        }

        String healthStatus = connected ? "HEALTHY" : "UNHEALTHY";
        if (serverMapper.updateHealthByTenant(tenantId, serverId, healthStatus, testedAt, failureReason) != 1) {
            throw new McpBusinessException(McpErrorCode.RESOURCE_NOT_FOUND, "MCP Server not found");
        }
        invalidateServerCache(tenantId);
        return new McpConnectivityTestResponse(
                connected,
                connected ? "CONNECTED" : "UNHEALTHY",
                message,
                (System.nanoTime() - startedNanos) / 1_000_000,
                testedAt);
    }

    @Override
    @Transactional
    public McpToolResponse updateToolStatus(Long toolId, McpToolStatusRequest request) {
        String tenantId = tenantContext.requireTenantId();
        McpTool tool = toolMapper.selectByTenantAndId(tenantId, toolId);
        if (tool == null) {
            throw new McpBusinessException(McpErrorCode.RESOURCE_NOT_FOUND, "MCP Tool not found");
        }
        if (toolMapper.updateStatusByTenant(tenantId, toolId, request.status()) != 1) {
            throw new McpBusinessException(McpErrorCode.RESOURCE_NOT_FOUND, "MCP Tool not found");
        }
        tool.setStatus(request.status());
        invalidateToolCache(tenantId);
        return toToolResponse(tool);
    }

    @Override
    public McpPageResponse<McpInvocationResponse> queryInvocations(McpInvocationQueryRequest request) {
        if (request.getStartedAfter() != null && request.getStartedBefore() != null
                && request.getStartedAfter().isAfter(request.getStartedBefore())) {
            throw new McpBusinessException(McpErrorCode.INVALID_REQUEST,
                    "startedAfter must not be later than startedBefore");
        }
        String tenantId = tenantContext.requireTenantId();
        int page = request.getPage() == null ? 1 : request.getPage();
        int size = request.getSize() == null ? 20 : request.getSize();
        long offset = (long) (page - 1) * size;
        long total = invocationMapper.countByTenantQuery(
                tenantId, request.getToolId(), request.getStatus(),
                request.getStartedAfter(), request.getStartedBefore());
        List<McpInvocationResponse> records = invocationMapper.selectPageByTenant(
                        tenantId, request.getToolId(), request.getStatus(),
                        request.getStartedAfter(), request.getStartedBefore(), size, offset)
                .stream()
                .map(this::toInvocationResponse)
                .toList();
        return new McpPageResponse<>(records, page, size, total);
    }

    private void invalidateServerCache(String tenantId) {
        if (queryCache != null) {
            queryCache.invalidateServerTenant(tenantId);
        }
    }

    private void invalidateToolCache(String tenantId) {
        if (queryCache != null) {
            queryCache.invalidateToolTenant(tenantId);
        }
    }

    private McpServer requireServer(String tenantId, Long serverId) {
        McpServer server = serverMapper.selectByTenantAndId(tenantId, serverId);
        if (server == null) {
            throw new McpBusinessException(McpErrorCode.RESOURCE_NOT_FOUND, "MCP Server not found");
        }
        return server;
    }

    private McpServerResponse toServerResponse(McpServer server) {
        return new McpServerResponse(server.getId(), server.getServerCode(), server.getServerName(),
                server.getTransportType(), server.getEndpoint(), server.getStatus(), server.getHealthStatus());
    }

    private McpToolResponse toToolResponse(McpTool tool) {
        return new McpToolResponse(tool.getId(), tool.getServerId(), tool.getToolName(), tool.getDisplayName(),
                tool.getDescription(), tool.getCategory(), tool.getScope(), tool.getPermissionLevel(),
                tool.getStatus(), tool.getVersion());
    }

    private McpInvocationResponse toInvocationResponse(McpInvocation invocation) {
        return new McpInvocationResponse(invocation.getId(), invocation.getToolId(), invocation.getServerId(),
                invocation.getTraceId(), invocation.getPrimitiveName(), invocation.getStatus(),
                invocation.getErrorCode(), invocation.getStartedAt(), invocation.getFinishedAt(),
                invocation.getLatencyMs());
    }

    private String failureReason(Exception exception) {
        String message = exception.getMessage();
        String reason = exception.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
        return reason.length() <= 512 ? reason : reason.substring(0, 512);
    }
}