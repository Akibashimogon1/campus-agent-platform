package com.campus.agent.mcp.service;

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

public interface McpManagementService {

    McpServerResponse registerServer(McpServerRegisterRequest request);

    McpServerResponse getServer(Long serverId);

    McpPageResponse<McpServerResponse> queryServers(McpServerQueryRequest request);

    McpServerResponse updateServer(Long serverId, McpServerUpdateRequest request);

    void deleteServer(Long serverId);

    McpPageResponse<McpToolResponse> queryTools(McpToolQueryRequest request);

    McpToolResponse getTool(Long toolId);

    McpConnectivityTestResponse testServerConnectivity(Long serverId);

    McpToolResponse updateToolStatus(Long toolId, McpToolStatusRequest request);

    McpPageResponse<McpInvocationResponse> queryInvocations(McpInvocationQueryRequest request);
}