package com.campus.agent.mcp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "MCP Server 注册请求")
public class McpServerRegisterRequest {

    @NotBlank
    @Size(max = 128)
    @Schema(description = "租户内唯一的 Server 编码", example = "orders-mcp")
    private String serverCode;

    @NotBlank
    @Size(max = 128)
    @Schema(description = "Server 展示名称", example = "Orders MCP")
    private String serverName;

    @NotBlank
    @Pattern(regexp = "HTTP_SSE|STDIO")
    @Schema(description = "传输类型", allowableValues = {"HTTP_SSE", "STDIO"}, example = "HTTP_SSE")
    private String transportType;

    @Size(max = 1024)
    @Schema(description = "HTTP_SSE endpoint；STDIO 模式可为空", example = "https://mcp.example.com/sse")
    private String endpoint;

    @Schema(description = "STDIO 进程配置")
    private Map<String, Object> processConfig;

    @Size(max = 128)
    @Schema(description = "凭据引用，不包含明文密钥", example = "orders-readonly")
    private String credentialRef;

    @NotBlank
    @Pattern(regexp = "REGISTERED|ENABLED|DISABLED|UNHEALTHY")
    @Schema(description = "初始状态", allowableValues = {"REGISTERED", "ENABLED", "DISABLED", "UNHEALTHY"},
            example = "REGISTERED")
    private String status;
}
