package com.campus.agent.mcp.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "当前租户的 MCP 工具分页查询参数")
public class McpToolQueryRequest {

    @Size(max = 64)
    @Schema(description = "按工具分类筛选", example = "search")
    private String category;

    @Size(max = 32)
    @Schema(description = "按工具作用域筛选", example = "TENANT")
    private String scope;

    @Size(max = 32)
    @Schema(description = "按工具状态筛选", example = "ENABLED")
    private String status;

    @Size(max = 128)
    @Schema(description = "工具名称或描述关键字", example = "search orders")
    private String keyword;

    @Min(1)
    @Schema(description = "页码，从 1 开始", minimum = "1", example = "1")
    private Integer page = 1;

    @Min(1)
    @Max(100)
    @Schema(description = "每页条数", minimum = "1", maximum = "100", example = "20")
    private Integer size = 20;
}
