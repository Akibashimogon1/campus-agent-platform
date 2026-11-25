package com.campus.agent.mcp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "MCP 工具调用结果")
public class McpCallResponse {

    @Schema(description = "调用状态", example = "SUCCEEDED")
    private String status;

    @Schema(description = "工具输出")
    private Map<String, Object> output;

    @Schema(description = "Server 版本")
    private Integer serverVersion;

    @Schema(description = "工具版本")
    private Integer toolVersion;

    @Schema(description = "原语类型", example = "TOOL")
    private String primitiveType;

    @Schema(description = "原语名称", example = "search_orders")
    private String primitiveName;

    @Schema(description = "调用耗时毫秒")
    private Long latencyMs;

    @Schema(description = "链路追踪 ID")
    private String traceId;

    @Schema(description = "统一错误码；成功时为空")
    private String errorCode;

    @Schema(description = "失败是否可重试")
    private Boolean retryable;

    @Schema(description = "结果说明")
    private String message;
}
