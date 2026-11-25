package com.campus.agent.mcp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "当前租户的 MCP Server 分页查询参数")
public class McpServerQueryRequest {

    @Size(max = 32)
    @Schema(description = "按 Server 状态筛选", example = "ENABLED")
    private String status;

    @Size(max = 32)
    @Schema(description = "按健康状态筛选", example = "HEALTHY")
    private String healthStatus;

    @Min(1)
    @Schema(description = "页码，从 1 开始", minimum = "1", example = "1")
    private Integer page = 1;

    @Min(1)
    @Max(100)
    @Schema(description = "每页条数", minimum = "1", maximum = "100", example = "20")
    private Integer size = 20;
}
