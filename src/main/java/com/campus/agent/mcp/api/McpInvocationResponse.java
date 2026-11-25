package com.campus.agent.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "MCP 工具调用记录")
public record McpInvocationResponse(
        Long id,
        Long toolId,
        Long serverId,
        String traceId,
        String toolName,
        String status,
        String errorCode,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        Long latencyMs) {
}