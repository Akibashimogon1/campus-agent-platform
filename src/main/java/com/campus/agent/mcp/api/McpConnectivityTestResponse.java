package com.campus.agent.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "MCP Server 连通性测试结果")
public record McpConnectivityTestResponse(
        boolean connected,
        String status,
        String message,
        Long latencyMs,
        LocalDateTime testedAt) {
}