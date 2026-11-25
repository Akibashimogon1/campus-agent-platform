package com.campus.agent.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "分页响应")
public record McpPageResponse<T>(
        List<T> records,
        @Schema(minimum = "1", example = "1") int page,
        @Schema(minimum = "1", maximum = "100", example = "20") int size,
        @Schema(minimum = "0", example = "42") long total) {
}