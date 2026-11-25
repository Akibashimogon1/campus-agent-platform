package com.campus.agent.mcp.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "工具上下线请求")
public record McpToolStatusRequest(
        @NotBlank
        @Pattern(regexp = "ENABLED|DISABLED")
        @Schema(description = "目标状态", allowableValues = {"ENABLED", "DISABLED"}, example = "ENABLED")
        String status) {
}