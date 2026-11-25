package com.campus.agent.mcp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class McpInvocationAuditRecord {

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

    private Long serverId;

    @NotBlank
    @Size(max = 16)
    private String primitiveType;

    @NotBlank
    @Size(max = 128)
    private String primitiveName;

    private Integer primitiveVersion;

    private Map<String, Object> requestSummary;

    private Map<String, Object> responseSummary;

    @NotBlank
    private String status;

    @Size(max = 64)
    private String errorCode;

    private Long latencyMs;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}
