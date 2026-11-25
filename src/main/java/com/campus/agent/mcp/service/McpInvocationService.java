package com.campus.agent.mcp.service;

import com.campus.agent.mcp.dto.McpCallResponse;
import com.campus.agent.mcp.api.McpToolCallApiRequest;

public interface McpInvocationService {

    McpCallResponse callTool(McpToolCallApiRequest request);
}