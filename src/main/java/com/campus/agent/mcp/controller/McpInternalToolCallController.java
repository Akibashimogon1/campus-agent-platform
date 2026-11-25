package com.campus.agent.mcp.controller;

import com.campus.agent.mcp.api.McpApiResponse;
import com.campus.agent.mcp.api.McpToolCallApiRequest;
import com.campus.agent.mcp.config.McpInternalServiceKeyInterceptor;
import com.campus.agent.mcp.dto.McpCallResponse;
import com.campus.agent.mcp.error.McpBusinessException;
import com.campus.agent.mcp.error.McpErrorCode;
import com.campus.agent.mcp.service.McpInvocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/mcp/tools/calls")
@Tag(name = "MCP 内部调用", description = "供 LangGraph 等可信内部服务调用")
public class McpInternalToolCallController {

    private final ObjectProvider<McpInvocationService> invocationServiceProvider;

        public McpInternalToolCallController(ObjectProvider<McpInvocationService> invocationServiceProvider) {
        this.invocationServiceProvider = invocationServiceProvider;
    }

    @PostMapping
    @Operation(summary = "调用 MCP 工具", description = "tenantId 从可信网关上下文注入，调用方不得在请求体中指定。")
    @SecurityRequirement(name = "InternalServiceKey")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "调用完成",
                    content = @Content(schema = @Schema(implementation = McpCallResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求校验失败"),
            @ApiResponse(responseCode = "401", description = "内部服务密钥无效或 tenant 上下文缺失",
                    content = @Content(schema = @Schema(implementation = McpApiResponse.class))),
            @ApiResponse(responseCode = "503", description = "调用服务尚未接入")
    })
        public McpCallResponse callTool(
            @Parameter(in = ParameterIn.HEADER, name = McpInternalServiceKeyInterceptor.HEADER_NAME,
                    required = true, description = "由服务端安全配置下发的内部服务密钥")
            @RequestHeader(McpInternalServiceKeyInterceptor.HEADER_NAME) String internalServiceKey,
            @Valid @RequestBody McpToolCallApiRequest request) {
        McpInvocationService service = invocationServiceProvider.getIfAvailable();
        if (service == null) {
            throw new McpBusinessException(McpErrorCode.SERVICE_UNAVAILABLE,
                    "MCP invocation service is not configured");
        }
                return service.callTool(request);
    }
}