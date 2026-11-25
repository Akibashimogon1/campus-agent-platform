package com.campus.agent.mcp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class McpToolCallRequest {

    @NotBlank
    @Size(max = 64)
    private String tenantId;

    @Size(max = 64)
    private String workflowId;

    @Size(max = 64)
    private String runId;

    @Size(max = 64)
    private String nodeId;

    @Size(max = 64)
    private String sessionId;

    @NotBlank
    @Size(max = 64)
    private String traceId;

    @NotBlank
    @Size(max = 128)
    private String toolName;

    @Positive
    private Long serverId;

    @Positive
    private Integer toolVersion;

    private Map<String, Object> arguments;

    @Positive
    private Integer timeoutMs;
}
