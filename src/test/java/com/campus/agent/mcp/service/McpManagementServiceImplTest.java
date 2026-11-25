package com.campus.agent.mcp.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.agent.mcp.api.McpConnectivityTestResponse;
import com.campus.agent.mcp.api.McpInvocationResponse;
import com.campus.agent.mcp.api.McpPageResponse;
import com.campus.agent.mcp.api.McpServerResponse;
import com.campus.agent.mcp.api.McpToolResponse;
import com.campus.agent.mcp.cache.McpCacheProperties;
import com.campus.agent.mcp.cache.McpQueryCache;
import com.campus.agent.mcp.dto.McpServerQueryRequest;
import com.campus.agent.mcp.context.TenantContext;
import com.campus.agent.mcp.dto.McpInvocationQueryRequest;
import com.campus.agent.mcp.dto.McpServerRegisterRequest;
import com.campus.agent.mcp.dto.McpServerUpdateRequest;
import com.campus.agent.mcp.dto.McpToolQueryRequest;
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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpManagementServiceImplTest {

    private final TenantContext tenantContext = new TenantContext();
    private McpServerMapper serverMapper;
    private McpToolMapper toolMapper;
    private McpInvocationMapper invocationMapper;
    private McpManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        tenantContext.setTenantId("tenant-a");
        serverMapper = mapperStub(McpServerMapper.class, Map.of());
        toolMapper = mapperStub(McpToolMapper.class, Map.of());
        invocationMapper = mapperStub(McpInvocationMapper.class, Map.of());
        service = new McpManagementServiceImpl(serverMapper, toolMapper, invocationMapper,
                tenantContext, server -> { });
    }

    @AfterEach
    void clearTenant() {
        tenantContext.clear();
    }

    @Test
    void registrationUsesTenantAndReturnsGeneratedId() {
        Map<String, Function<Object[], Object>> actions = new HashMap<>();
        actions.put("selectByTenantAndCode", arguments -> {
            assertThat(arguments[0]).isEqualTo("tenant-a");
            return null;
        });
        actions.put("insert", arguments -> {
            McpServer server = (McpServer) arguments[0];
            assertThat(server.getTenantId()).isEqualTo("tenant-a");
            server.setId(42L);
            return 1;
        });
        serverMapper = mapperStub(McpServerMapper.class, actions);
        service = new McpManagementServiceImpl(serverMapper, toolMapper, invocationMapper,
                tenantContext, server -> { });
        McpServerRegisterRequest request = new McpServerRegisterRequest();
        request.setServerCode("orders");
        request.setServerName("Orders");
        request.setTransportType("HTTP_SSE");
        request.setEndpoint("https://mcp.example");
        request.setStatus("REGISTERED");

        var response = service.registerServer(request);

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.serverCode()).isEqualTo("orders");
    }

    @Test
    void duplicateServerCodeProducesConflictWithoutInsert() {
        McpServer existing = new McpServer();
        existing.setId(8L);
        serverMapper = mapperStub(McpServerMapper.class, Map.of(
                "selectByTenantAndCode", arguments -> existing,
                "insert", arguments -> { throw new AssertionError("duplicate must not insert"); }));
        service = new McpManagementServiceImpl(serverMapper, toolMapper, invocationMapper,
                tenantContext, server -> { });
        McpServerRegisterRequest request = new McpServerRegisterRequest();
        request.setServerCode("orders");

        assertThatThrownBy(() -> service.registerServer(request))
                .isInstanceOf(McpBusinessException.class)
                .extracting(exception -> ((McpBusinessException) exception).getErrorCode())
                .isEqualTo(McpErrorCode.RESOURCE_CONFLICT);
    }

    @Test
    void toolPageUsesTenantAndSqlPagination() {
        McpTool tool = new McpTool();
        tool.setId(7L);
        tool.setServerId(2L);
        tool.setToolName("search");
        tool.setStatus("ENABLED");
        tool.setVersion(1);
        Map<String, Function<Object[], Object>> actions = new HashMap<>();
        actions.put("countByTenantQuery", arguments -> {
            assertThat(arguments[0]).isEqualTo("tenant-a");
            return 21L;
        });
        actions.put("selectByTenantQuery", arguments -> {
            assertThat(arguments[0]).isEqualTo("tenant-a");
            assertThat(arguments[5]).isEqualTo(10);
            assertThat(arguments[6]).isEqualTo(20L);
            return List.of(tool);
        });
        toolMapper = mapperStub(McpToolMapper.class, actions);
        service = new McpManagementServiceImpl(serverMapper, toolMapper, invocationMapper,
                tenantContext, server -> { });
        McpToolQueryRequest request = new McpToolQueryRequest();
        request.setPage(3);
        request.setSize(10);

        McpPageResponse<McpToolResponse> response = service.queryTools(request);

        assertThat(response.page()).isEqualTo(3);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.total()).isEqualTo(21);
        assertThat(response.records()).extracting(McpToolResponse::toolName).containsExactly("search");
    }

    @Test
    void serverPageLoadsOnceThenHitsCache() {
        AtomicInteger countCalls = new AtomicInteger();
        AtomicInteger selectCalls = new AtomicInteger();
        McpServer server = sampleServer(9L);
        Map<String, Function<Object[], Object>> actions = new HashMap<>();
        actions.put("countByTenantQuery", arguments -> {
            countCalls.incrementAndGet();
            return 1L;
        });
        actions.put("selectPageByTenant", arguments -> {
            selectCalls.incrementAndGet();
            return List.of(server);
        });
        serverMapper = mapperStub(McpServerMapper.class, actions);
        McpQueryCache queryCache = new McpQueryCache(testCacheProperties());
        service = new McpManagementServiceImpl(serverMapper, toolMapper, invocationMapper,
                tenantContext, mcpServer -> { }, queryCache);
        McpServerQueryRequest request = new McpServerQueryRequest();

        McpPageResponse<McpServerResponse> first = service.queryServers(request);
        McpPageResponse<McpServerResponse> second = service.queryServers(request);

        assertThat(first.records()).extracting(McpServerResponse::id).containsExactly(9L);
        assertThat(second.records()).extracting(McpServerResponse::id).containsExactly(9L);
        assertThat(countCalls).hasValue(1);
        assertThat(selectCalls).hasValue(1);
    }

    @Test
    void serverUpdateInvalidatesTenantServerCache() {
        AtomicInteger countCalls = new AtomicInteger();
        McpServer server = sampleServer(9L);
        Map<String, Function<Object[], Object>> actions = new HashMap<>();
        actions.put("countByTenantQuery", arguments -> {
            countCalls.incrementAndGet();
            return 1L;
        });
        actions.put("selectPageByTenant", arguments -> List.of(server));
        actions.put("selectByTenantAndId", arguments -> server);
        actions.put("updateConfigurationByTenant", arguments -> 1);
        serverMapper = mapperStub(McpServerMapper.class, actions);
        McpQueryCache queryCache = new McpQueryCache(testCacheProperties());
        service = new McpManagementServiceImpl(serverMapper, toolMapper, invocationMapper,
                tenantContext, mcpServer -> { }, queryCache);
        McpServerQueryRequest request = new McpServerQueryRequest();

        service.queryServers(request);
        McpServerUpdateRequest update = new McpServerUpdateRequest();
        update.setServerName("Updated orders");
        update.setTransportType("HTTP_SSE");
        update.setStatus("ENABLED");
        service.updateServer(9L, update);
        service.queryServers(request);

        assertThat(countCalls).hasValue(2);
    }

    @Test
    void toolPageLoadsOnceThenHitsCache() {
        AtomicInteger countCalls = new AtomicInteger();
        AtomicInteger selectCalls = new AtomicInteger();
        McpTool tool = sampleTool(12L);
        Map<String, Function<Object[], Object>> actions = new HashMap<>();
        actions.put("countByTenantQuery", arguments -> {
            countCalls.incrementAndGet();
            return 1L;
        });
        actions.put("selectByTenantQuery", arguments -> {
            selectCalls.incrementAndGet();
            return List.of(tool);
        });
        toolMapper = mapperStub(McpToolMapper.class, actions);
        McpQueryCache queryCache = new McpQueryCache(testCacheProperties());
        service = new McpManagementServiceImpl(serverMapper, toolMapper, invocationMapper,
                tenantContext, mcpServer -> { }, queryCache);
        McpToolQueryRequest request = new McpToolQueryRequest();

        McpPageResponse<McpToolResponse> first = service.queryTools(request);
        McpPageResponse<McpToolResponse> second = service.queryTools(request);

        assertThat(first.records()).extracting(McpToolResponse::toolName).containsExactly("search");
        assertThat(second.records()).extracting(McpToolResponse::toolName).containsExactly("search");
        assertThat(countCalls).hasValue(1);
        assertThat(selectCalls).hasValue(1);
    }

    @Test
    void serverDetailsAreSelectedWithinCurrentTenant() {
        McpServer server = sampleServer(9L);
        serverMapper = mapperStub(McpServerMapper.class, Map.of("selectByTenantAndId", arguments -> {
            assertThat(arguments[0]).isEqualTo("tenant-a");
            assertThat(arguments[1]).isEqualTo(9L);
            return server;
        }));
        service = new McpManagementServiceImpl(serverMapper, toolMapper, invocationMapper,
                tenantContext, mcpServer -> { });

        assertThat(service.getServer(9L).id()).isEqualTo(9L);
    }

    @Test
    void serverUpdateAndDeleteUseTenantBoundMapperMethods() {
        McpServer server = sampleServer(9L);
        Map<String, Function<Object[], Object>> actions = new HashMap<>();
        actions.put("selectByTenantAndId", arguments -> server);
        actions.put("updateConfigurationByTenant", arguments -> {
            assertThat(arguments[0]).isEqualTo("tenant-a");
            assertThat(arguments[1]).isEqualTo(9L);
            assertThat(arguments[2]).isEqualTo("Updated orders");
            return 1;
        });
        actions.put("softDeleteByTenant", arguments -> {
            assertThat(arguments[0]).isEqualTo("tenant-a");
            assertThat(arguments[1]).isEqualTo(9L);
            return 1;
        });
        AtomicInteger deletedToolCount = new AtomicInteger();
        toolMapper = mapperStub(McpToolMapper.class, Map.of("softDeleteByServerTenant", arguments -> {
            assertThat(arguments[0]).isEqualTo("tenant-a");
            assertThat(arguments[1]).isEqualTo(9L);
            deletedToolCount.set(2);
            return 2;
        }));
        serverMapper = mapperStub(McpServerMapper.class, actions);
        service = new McpManagementServiceImpl(serverMapper, toolMapper, invocationMapper,
                tenantContext, mcpServer -> { });
        McpServerUpdateRequest update = new McpServerUpdateRequest();
        update.setServerName("Updated orders");
        update.setTransportType("HTTP_SSE");
        update.setStatus("ENABLED");

        assertThat(service.updateServer(9L, update).id()).isEqualTo(9L);
        service.deleteServer(9L);
        assertThat(deletedToolCount).hasValue(2);
    }

    @Test
    void toolDetailsAndStatusChangesAreTenantBound() {
        McpTool tool = sampleTool(12L);
        Map<String, Function<Object[], Object>> actions = new HashMap<>();
        actions.put("selectByTenantAndId", arguments -> {
            assertThat(arguments[0]).isEqualTo("tenant-a");
            return tool;
        });
        actions.put("updateStatusByTenant", arguments -> {
            assertThat(arguments[0]).isEqualTo("tenant-a");
            assertThat(arguments[1]).isEqualTo(12L);
            assertThat(arguments[2]).isEqualTo("DISABLED");
            return 1;
        });
        toolMapper = mapperStub(McpToolMapper.class, actions);
        service = new McpManagementServiceImpl(serverMapper, toolMapper, invocationMapper,
                tenantContext, mcpServer -> { });

        assertThat(service.getTool(12L).toolName()).isEqualTo("search");
        assertThat(service.updateToolStatus(12L, new com.campus.agent.mcp.api.McpToolStatusRequest("DISABLED"))
                .status()).isEqualTo("DISABLED");
    }

    @Test
    void connectivityResultPersistsHealthForCurrentTenant() {
        McpServer server = sampleServer(9L);
        Map<String, Function<Object[], Object>> actions = new HashMap<>();
        actions.put("selectByTenantAndId", arguments -> server);
        actions.put("updateHealthByTenant", arguments -> {
            assertThat(arguments[0]).isEqualTo("tenant-a");
            assertThat(arguments[1]).isEqualTo(9L);
            assertThat(arguments[2]).isEqualTo("HEALTHY");
            assertThat(arguments[4]).isNull();
            return 1;
        });
        serverMapper = mapperStub(McpServerMapper.class, actions);
        service = new McpManagementServiceImpl(serverMapper, toolMapper, invocationMapper,
                tenantContext, mcpServer -> { });

        McpConnectivityTestResponse response = service.testServerConnectivity(9L);

        assertThat(response.connected()).isTrue();
        assertThat(response.status()).isEqualTo("CONNECTED");
    }

    @Test
    void connectivityFailurePersistsUnhealthyStatusAndReason() {
        McpServer server = sampleServer(9L);
        Map<String, Function<Object[], Object>> actions = new HashMap<>();
        actions.put("selectByTenantAndId", arguments -> server);
        actions.put("updateHealthByTenant", arguments -> {
            assertThat(arguments[0]).isEqualTo("tenant-a");
            assertThat(arguments[2]).isEqualTo("UNHEALTHY");
            assertThat(arguments[4]).asString().contains("SocketTimeoutException");
            return 1;
        });
        serverMapper = mapperStub(McpServerMapper.class, actions);
        service = new McpManagementServiceImpl(serverMapper, toolMapper, invocationMapper,
                tenantContext, mcpServer -> { throw new java.net.SocketTimeoutException("timed out"); });

        McpConnectivityTestResponse response = service.testServerConnectivity(9L);

        assertThat(response.connected()).isFalse();
        assertThat(response.status()).isEqualTo("UNHEALTHY");
        assertThat(response.message()).contains("SocketTimeoutException");
    }

    @Test
    void invocationPageFiltersAndPaginatesWithinTenant() {
        McpInvocation invocation = new McpInvocation();
        invocation.setId(5L);
        invocation.setToolId(12L);
        invocation.setTraceId("trace-a");
        invocation.setPrimitiveName("search");
        invocation.setStatus("SUCCEEDED");
        invocation.setStartedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        Map<String, Function<Object[], Object>> actions = new HashMap<>();
        actions.put("countByTenantQuery", arguments -> {
            assertThat(arguments[0]).isEqualTo("tenant-a");
            assertThat(arguments[1]).isEqualTo(12L);
            return 1L;
        });
        actions.put("selectPageByTenant", arguments -> {
            assertThat(arguments[0]).isEqualTo("tenant-a");
            assertThat(arguments[5]).isEqualTo(5);
            assertThat(arguments[6]).isEqualTo(5L);
            return List.of(invocation);
        });
        invocationMapper = mapperStub(McpInvocationMapper.class, actions);
        service = new McpManagementServiceImpl(serverMapper, toolMapper, invocationMapper,
                tenantContext, mcpServer -> { });
        McpInvocationQueryRequest request = new McpInvocationQueryRequest();
        request.setToolId(12L);
        request.setPage(2);
        request.setSize(5);

        McpPageResponse<McpInvocationResponse> response = service.queryInvocations(request);

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.records()).extracting(McpInvocationResponse::traceId).containsExactly("trace-a");
    }

    @Test
    void rejectsInvocationTimeRangeInReverseOrder() {
        McpInvocationQueryRequest request = new McpInvocationQueryRequest();
        request.setStartedAfter(LocalDateTime.of(2026, 2, 1, 0, 0));
        request.setStartedBefore(LocalDateTime.of(2026, 1, 1, 0, 0));

        assertThatThrownBy(() -> service.queryInvocations(request))
                .isInstanceOf(McpBusinessException.class)
                .extracting(exception -> ((McpBusinessException) exception).getErrorCode())
                .isEqualTo(McpErrorCode.INVALID_REQUEST);
    }

    private McpCacheProperties testCacheProperties() {
        McpCacheProperties properties = new McpCacheProperties();
        properties.setServer(new McpCacheProperties.CacheSettings(java.time.Duration.ofMinutes(5), 100));
        properties.setTool(new McpCacheProperties.CacheSettings(java.time.Duration.ofMinutes(5), 100));
        return properties;
    }

    private McpServer sampleServer(Long id) {
        McpServer server = new McpServer();
        server.setId(id);
        server.setServerCode("orders");
        server.setServerName("Orders");
        server.setTransportType("HTTP_SSE");
        server.setStatus("ENABLED");
        return server;
    }

    private McpTool sampleTool(Long id) {
        McpTool tool = new McpTool();
        tool.setId(id);
        tool.setServerId(9L);
        tool.setToolName("search");
        tool.setStatus("ENABLED");
        tool.setVersion(1);
        return tool;
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
