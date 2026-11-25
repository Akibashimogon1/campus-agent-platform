package com.campus.agent.mcp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "MCP Server 基础配置更新请求")
public class McpServerUpdateRequest {

    @NotBlank
    @Size(max = 128)
    private String serverName;

    @NotBlank
    @Pattern(regexp = "HTTP_SSE|STDIO")
    private String transportType;

    @Size(max = 1024)
    private String endpoint;

    private Map<String, Object> processConfig;

    @Size(max = 128)
    private String credentialRef;

    @NotBlank
    @Pattern(regexp = "REGISTERED|ENABLED|DISABLED|UNHEALTHY")
    private String status;
}