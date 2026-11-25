package com.campus.agent.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "MCP 工具管理视图")
public record McpToolResponse(
        Long id,
        Long serverId,
        String toolName,
        String displayName,
        String description,
        String category,
        String scope,
        String permissionLevel,
        String status,
        Integer version) {
}