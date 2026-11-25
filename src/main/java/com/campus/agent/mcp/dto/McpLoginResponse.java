package com.campus.agent.mcp.dto;

public record McpLoginResponse(String accessToken, String username, String role, String tenantId) {
}
