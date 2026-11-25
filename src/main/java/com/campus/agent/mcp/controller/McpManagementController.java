package com.campus.agent.mcp.controller;

import com.campus.agent.mcp.api.McpApiResponse;
import com.campus.agent.mcp.api.McpConnectivityTestResponse;
import com.campus.agent.mcp.api.McpInvocationResponse;
import com.campus.agent.mcp.api.McpPageResponse;
import com.campus.agent.mcp.api.McpServerResponse;
import com.campus.agent.mcp.api.McpToolResponse;
import com.campus.agent.mcp.api.McpToolStatusRequest;
import com.campus.agent.mcp.dto.McpInvocationQueryRequest;
import com.campus.agent.mcp.dto.McpServerQueryRequest;
import com.campus.agent.mcp.dto.McpServerRegisterRequest;
import com.campus.agent.mcp.dto.McpServerUpdateRequest;
import com.campus.agent.mcp.dto.McpToolQueryRequest;
import com.campus.agent.mcp.error.McpBusinessException;
import com.campus.agent.mcp.error.McpErrorCode;
import com.campus.agent.mcp.service.McpManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mcp")
@Validated
@Tag(name = "MCP 管理", description = "面向管理端的 MCP Server 与工具管理接口")
public class McpManagementController {

    private final ObjectProvider<McpManagementService> managementServiceProvider;

        public McpManagementController(ObjectProvider<McpManagementService> managementServiceProvider) {
        this.managementServiceProvider = managementServiceProvider;
    }

    @PostMapping("/servers")
    @Operation(summary = "注册 MCP Server", description = "tenantId 从可信请求上下文解析，不接受客户端传入。")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "注册成功"),
            @ApiResponse(responseCode = "400", description = "请求校验失败",
                    content = @Content(schema = @Schema(implementation = McpApiResponse.class))),
            @ApiResponse(responseCode = "409", description = "Server 编码冲突"),
            @ApiResponse(responseCode = "503", description = "业务服务尚未接入")
    })
    public ResponseEntity<McpApiResponse<McpServerResponse>> registerServer(
            @Valid @RequestBody McpServerRegisterRequest request) {
        McpServerResponse result = requireManagementService().registerServer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(McpApiResponse.success(result));
    }

    @GetMapping("/servers")
    @Operation(summary = "分页查询 MCP Server", description = "按当前租户分页查询未删除的 Server。")
    public McpApiResponse<McpPageResponse<McpServerResponse>> queryServers(
            @Valid @ParameterObject @ModelAttribute McpServerQueryRequest request) {
        return McpApiResponse.success(requireManagementService().queryServers(request));
    }

    @GetMapping("/servers/{serverId}")
    @Operation(summary = "查询 MCP Server 详情")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "Server 不存在")
    })
    public McpApiResponse<McpServerResponse> getServer(
            @Parameter(in = ParameterIn.PATH, description = "MCP Server ID", example = "42")
            @PathVariable @Positive Long serverId) {
        return McpApiResponse.success(requireManagementService().getServer(serverId));
    }

    @PutMapping("/servers/{serverId}")
    @Operation(summary = "更新 MCP Server 配置")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "请求校验失败"),
            @ApiResponse(responseCode = "404", description = "Server 不存在")
    })
    public McpApiResponse<McpServerResponse> updateServer(
            @PathVariable @Positive Long serverId,
            @Valid @RequestBody McpServerUpdateRequest request) {
        return McpApiResponse.success(requireManagementService().updateServer(serverId, request));
    }

    @DeleteMapping("/servers/{serverId}")
    @Operation(summary = "逻辑删除 MCP Server")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "Server 不存在")
    })
    public McpApiResponse<Void> deleteServer(@PathVariable @Positive Long serverId) {
        requireManagementService().deleteServer(serverId);
        return McpApiResponse.success(null);
    }

    @GetMapping("/tools")
    @Operation(summary = "查询工具列表", description = "按当前租户分页查询工具；租户由可信请求上下文提供。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "查询参数校验失败"),
            @ApiResponse(responseCode = "503", description = "业务服务尚未接入")
    })
    public McpApiResponse<McpPageResponse<McpToolResponse>> queryTools(
            @Valid @ParameterObject @ModelAttribute McpToolQueryRequest request) {
        return McpApiResponse.success(requireManagementService().queryTools(request));
    }

    @GetMapping("/tools/{toolId}")
    @Operation(summary = "查询 MCP 工具详情")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "工具不存在")
    })
    public McpApiResponse<McpToolResponse> getTool(@PathVariable @Positive Long toolId) {
        return McpApiResponse.success(requireManagementService().getTool(toolId));
    }

    @GetMapping("/invocations")
    @Operation(summary = "分页查询 MCP 调用记录")
    public McpApiResponse<McpPageResponse<McpInvocationResponse>> queryInvocations(
            @Valid @ParameterObject @ModelAttribute McpInvocationQueryRequest request) {
        return McpApiResponse.success(requireManagementService().queryInvocations(request));
    }

    @PostMapping("/servers/{serverId}/connectivity-tests")
    @Operation(summary = "测试 Server 连通性", description = "对当前租户的 MCP Server 执行连通性测试。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "测试完成"),
            @ApiResponse(responseCode = "400", description = "Server ID 无效"),
            @ApiResponse(responseCode = "404", description = "Server 不存在"),
            @ApiResponse(responseCode = "503", description = "业务服务尚未接入")
    })
    public McpApiResponse<McpConnectivityTestResponse> testConnectivity(
            @Parameter(in = ParameterIn.PATH, description = "MCP Server ID", example = "42")
            @PathVariable @Positive Long serverId) {
        return McpApiResponse.success(requireManagementService().testServerConnectivity(serverId));
    }

    @PatchMapping("/tools/{toolId}/status")
    @Operation(summary = "工具上线或下线", description = "通过 ENABLED/DISABLED 状态切换工具。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "状态已更新"),
            @ApiResponse(responseCode = "400", description = "请求校验失败"),
            @ApiResponse(responseCode = "404", description = "工具不存在"),
            @ApiResponse(responseCode = "503", description = "业务服务尚未接入")
    })
    public McpApiResponse<McpToolResponse> updateToolStatus(
            @Parameter(in = ParameterIn.PATH, description = "MCP Tool ID", example = "73")
            @PathVariable @Positive Long toolId,
            @Valid @RequestBody McpToolStatusRequest request) {
        return McpApiResponse.success(requireManagementService().updateToolStatus(toolId, request));
    }

    private McpManagementService requireManagementService() {
        McpManagementService service = managementServiceProvider.getIfAvailable();
        if (service == null) {
            throw new McpBusinessException(McpErrorCode.SERVICE_UNAVAILABLE,
                    "MCP management service is not configured");
        }
        return service;
    }
}