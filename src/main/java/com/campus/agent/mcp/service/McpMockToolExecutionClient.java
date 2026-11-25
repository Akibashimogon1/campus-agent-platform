package com.campus.agent.mcp.service;

import com.campus.agent.mcp.dto.McpCallResponse;
import com.campus.agent.mcp.dto.McpToolCallRequest;
import com.campus.agent.mcp.entity.McpServer;
import com.campus.agent.mcp.entity.McpTool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "mcp.execution.mode", havingValue = "mock")
public class McpMockToolExecutionClient implements McpToolExecutionClient {

    @Override
    public McpCallResponse call(McpServer server, McpTool tool, McpToolCallRequest request) {
        McpCallResponse response = new McpCallResponse();
        response.setStatus("SUCCEEDED");
        response.setOutput(Map.of(
                "mocked", true,
                "toolName", tool.getToolName(),
                "argumentCount", request.getArguments() == null ? 0 : request.getArguments().size()));
        response.setRetryable(false);
        response.setMessage("模拟调用成功；待对接真实下游服务");
        return response;
    }
}