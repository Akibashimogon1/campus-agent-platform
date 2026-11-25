package com.campus.agent.mcp.api;

import com.campus.agent.mcp.error.McpErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一 API 响应")
public record McpApiResponse<T>(
        @Schema(description = "稳定的响应码", example = "SUCCESS") String code,
        @Schema(description = "响应说明", example = "success") String message,
        @Schema(description = "响应数据") T data) {

    public static <T> McpApiResponse<T> success(T data) {
        return new McpApiResponse<>("SUCCESS", "success", data);
    }

    public static McpApiResponse<Void> error(McpErrorCode code, String message) {
        return new McpApiResponse<>(code.name(), message, null);
    }
}