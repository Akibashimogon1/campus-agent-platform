package com.campus.agent.mcp.service;

import com.campus.agent.mcp.dto.McpCallResponse;
import com.campus.agent.mcp.dto.McpToolCallRequest;
import com.campus.agent.mcp.entity.McpServer;
import com.campus.agent.mcp.entity.McpTool;

public interface McpToolExecutionClient {

    McpCallResponse call(McpServer server, McpTool tool, McpToolCallRequest request) throws Exception;
}