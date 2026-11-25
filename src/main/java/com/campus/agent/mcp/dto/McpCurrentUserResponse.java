package com.campus.agent.mcp.dto;

public record McpCurrentUserResponse(Long id, String username, String role, String tenantId) {
}
