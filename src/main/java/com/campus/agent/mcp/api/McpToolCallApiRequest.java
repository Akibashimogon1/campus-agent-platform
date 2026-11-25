package com.campus.agent.mcp.api;

import com.campus.agent.mcp.dto.McpToolCallRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "LangGraph 内部工具调用请求；tenantId 由服务端从请求上下文注入")
public class McpToolCallApiRequest {

    @Size(max = 64)
    @Schema(description = "工作流 ID", example = "workflow-123")
    private String workflowId;

    @Size(max = 64)
    @Schema(description = "工作流运行 ID", example = "run-456")
    private String runId;

    @Size(max = 64)
    @Schema(description = "工作流节点 ID", example = "node-search")
    private String nodeId;

    @Size(max = 64)
    @Schema(description = "会话 ID", example = "session-789")
    private String sessionId;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "链路追踪 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "trace-a1b2")
    private String traceId;

    @NotBlank
    @Size(max = 128)
    @Schema(description = "要调用的工具名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "search_orders")
    private String toolName;

    @Positive
    @Schema(description = "可选 Server ID；指定后消除同名工具歧义", minimum = "1", example = "7")
    private Long serverId;

    @Positive
    @Schema(description = "期望的工具版本；为空表示使用当前版本", minimum = "1", example = "2")
    private Integer toolVersion;

    @Schema(description = "工具参数")
    private Map<String, Object> arguments;

    @Positive
    @Schema(description = "调用超时毫秒数", minimum = "1", example = "5000")
    private Integer timeoutMs;

    public McpToolCallRequest toBusinessRequest(String tenantId) {
        McpToolCallRequest request = new McpToolCallRequest();
        request.setTenantId(tenantId);
        request.setWorkflowId(workflowId);
        request.setRunId(runId);
        request.setNodeId(nodeId);
        request.setSessionId(sessionId);
        request.setTraceId(traceId);
        request.setToolName(toolName);
        request.setServerId(serverId);
        request.setToolVersion(toolVersion);
        request.setArguments(arguments);
        request.setTimeoutMs(timeoutMs);
        return request;
    }
}