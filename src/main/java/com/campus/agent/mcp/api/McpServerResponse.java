package com.campus.agent.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "MCP Server 管理视图")
public record McpServerResponse(
        Long id,
        String serverCode,
        String serverName,
        String transportType,
        String endpoint,
        String status,
        String healthStatus) {
}