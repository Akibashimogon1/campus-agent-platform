package com.campus.agent.mcp.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.agent.mcp.api.McpToolCallApiRequest;
import com.campus.agent.mcp.context.TenantContext;
import com.campus.agent.mcp.dto.McpCallResponse;
import com.campus.agent.mcp.entity.McpInvocation;
import com.campus.agent.mcp.entity.McpServer;
import com.campus.agent.mcp.entity.McpTool;
import com.campus.agent.mcp.error.McpBusinessException;
import com.campus.agent.mcp.error.McpErrorCode;
import com.campus.agent.mcp.mapper.McpInvocationMapper;
import com.campus.agent.mcp.mapper.McpServerMapper;
import com.campus.agent.mcp.mapper.McpToolMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpInvocationServiceImplTest {

    private final TenantContext tenantContext = new TenantContext();
    private final McpTool tool = new McpTool();
    private final McpServer server = new McpServer();
    private McpInvocation capturedInvocation;
    private McpInvocationServiceImpl service;

    @BeforeEach
    void setUp() {
        tenantContext.setTenantId("tenant-a");
        tool.setId(11L);
        tool.setServerId(7L);
        tool.setToolName("search");
        tool.setStatus("ENABLED");
        tool.setVersion(3);
        server.setId(7L);
        server.setVersion(2);
        server.setStatus("ENABLED");

        McpToolMapper toolMapper = mapperStub(McpToolMapper.class, Map.of(
                "selectByTenantAndName", arguments -> {
                    assertThat(arguments[0]).isEqualTo("tenant-a");
                    assertThat(arguments[1]).isEqualTo("search");
                    return List.of(tool);
                }));
        McpServerMapper serverMapper = mapperStub(McpServerMapper.class, Map.of(
                "selectByTenantAndId", arguments -> {
                    assertThat(arguments[0]).isEqualTo("tenant-a");
                    assertThat(arguments[1]).isEqualTo(7L);
                    return server;
                }));
        McpInvocationMapper invocationMapper = mapperStub(McpInvocationMapper.class, Map.of(
                "insert", arguments -> {
                    capturedInvocation = (McpInvocation) arguments[0];
                    return 1;
                }));
        McpToolExecutionClient executionClient = (mcpServer, mcpTool, request) -> {
            McpCallResponse response = new McpCallResponse();
            response.setStatus("SUCCEEDED");
            return response;
        };
        service = new McpInvocationServiceImpl(toolMapper, serverMapper, invocationMapper,
                tenantContext, executionClient);
    }

    @AfterEach
    void clearTenant() {
        tenantContext.clear();
    }

    @Test
    void executesEnabledToolAndPersistsTenantScopedAuditRecord() {
        McpToolCallApiRequest request = new McpToolCallApiRequest();
        request.setTraceId("trace-a");
        request.setToolName("search");
        request.setWorkflowId("workflow-a");

        McpCallResponse response = service.callTool(request);

        assertThat(response.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(response.getTraceId()).isEqualTo("trace-a");
        assertThat(response.getToolVersion()).isEqualTo(3);
        assertThat(capturedInvocation.getTenantId()).isEqualTo("tenant-a");
        assertThat(capturedInvocation.getToolId()).isEqualTo(11L);
        assertThat(capturedInvocation.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(capturedInvocation.getWorkflowId()).isEqualTo("workflow-a");
    }

    @Test
    void recordsDownstreamFailureAndReturnsUnifiedBusinessError() {
        McpToolExecutionClient failingClient = (mcpServer, mcpTool, request) -> {
            throw new IllegalStateException("downstream unavailable");
        };
        service = new McpInvocationServiceImpl(
                mapperStub(McpToolMapper.class, Map.of("selectByTenantAndName", arguments -> List.of(tool))),
                mapperStub(McpServerMapper.class, Map.of("selectByTenantAndId", arguments -> server)),
                mapperStub(McpInvocationMapper.class, Map.of("insert", arguments -> {
                    capturedInvocation = (McpInvocation) arguments[0];
                    return 1;
                })),
                tenantContext,
                failingClient);
        McpToolCallApiRequest request = new McpToolCallApiRequest();
        request.setTraceId("trace-fail");
        request.setToolName("search");

        assertThatThrownBy(() -> service.callTool(request))
                .isInstanceOf(McpBusinessException.class)
                .extracting(exception -> ((McpBusinessException) exception).getErrorCode())
                .isEqualTo(McpErrorCode.DOWNSTREAM_ERROR);
        assertThat(capturedInvocation.getTenantId()).isEqualTo("tenant-a");
        assertThat(capturedInvocation.getStatus()).isEqualTo("FAILED");
        assertThat(capturedInvocation.getErrorCode()).isEqualTo("DOWNSTREAM_ERROR");
    }

    private static <T> T mapperStub(Class<T> mapperType, Map<String, Function<Object[], Object>> actions) {
        InvocationHandler handler = (proxy, method, arguments) -> {
            Function<Object[], Object> action = actions.get(method.getName());
            if (action != null) {
                return action.apply(arguments == null ? new Object[0] : arguments);
            }
            if (method.getDeclaringClass() == Object.class) {
                return method.getName().equals("toString") ? mapperType.getSimpleName() + "Stub" : null;
            }
            if (method.getReturnType() == int.class) {
                return 0;
            }
            if (method.getReturnType() == long.class) {
                return 0L;
            }
            if (method.getReturnType() == boolean.class) {
                return false;
            }
            return null;
        };
        Object mapper = Proxy.newProxyInstance(
                mapperType.getClassLoader(), new Class<?>[]{mapperType, BaseMapper.class}, handler);
        return mapperType.cast(mapper);
    }
}
