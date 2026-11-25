package com.campus.agent.mcp.dto;

import jakarta.validation.constraints.NotBlank;

public record McpLoginRequest(
        @NotBlank String username,
        @NotBlank String password) {
}
