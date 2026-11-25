package com.campus.agent.mcp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.campus.agent.mcp.dto.McpToolCallRequest;
import com.campus.agent.mcp.entity.McpServer;
import com.campus.agent.mcp.entity.McpTool;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpHttpConnectivityClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<String> methods = new ArrayList<>();
    private final List<String> sessionHeaders = new ArrayList<>();
    private final List<String> protocolHeaders = new ArrayList<>();
    private HttpServer httpServer;
    private boolean rejectInitialize;
    private boolean rejectToolCall;
    private boolean streamInitializeResponse;
    private boolean streamToolCallResponse;

    @BeforeEach
    void startHttpServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/mcp", this::handleRequest);
        httpServer.start();
    }

    @AfterEach
    void stopHttpServer() {
        httpServer.stop(0);
    }

    @Test
    void performsInitializeNotificationAndPingInOrder() {
        McpHttpConnectivityClient client = new McpHttpConnectivityClient(
            objectMapper, Duration.ofSeconds(1), Duration.ofSeconds(1));

        client.initializeAndPing(server());

        assertThat(methods).containsExactly("initialize", "notifications/initialized", "ping");
        assertThat(sessionHeaders).containsExactly(null, "test-session", "test-session");
        assertThat(protocolHeaders).containsOnly("2025-03-26");
    }

    @Test
    void rejectsInitializeJsonRpcError() {
        rejectInitialize = true;
        McpHttpConnectivityClient client = new McpHttpConnectivityClient(
            objectMapper, Duration.ofSeconds(1), Duration.ofSeconds(1));

        assertThatThrownBy(() -> client.initializeAndPing(server()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid JSON-RPC response");
    }

    @Test
    void executesToolsCallWithSessionAndProtocolHeaders() throws Exception {
        McpHttpToolExecutionClient client = new McpHttpToolExecutionClient(
                objectMapper, Duration.ofSeconds(1), Duration.ofSeconds(1));
        McpToolCallRequest request = new McpToolCallRequest();
        request.setTenantId("tenant-a");
        request.setTraceId("trace-a");
        request.setToolName("search");
        request.setArguments(Map.of("q", "orders"));

        var response = client.call(server(), tool(), request);

        assertThat(response.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(methods).containsExactly("initialize", "notifications/initialized", "tools/call");
        assertThat(sessionHeaders).containsExactly(null, "test-session", "test-session");
        assertThat(protocolHeaders).containsOnly("2025-03-26");
    }

    @Test
    void parsesToolsCallFromServerSentEventsAndMapsJsonRpcError() throws Exception {
        streamToolCallResponse = true;
        McpHttpToolExecutionClient client = new McpHttpToolExecutionClient(
                objectMapper, Duration.ofSeconds(1), Duration.ofSeconds(1));
        McpToolCallRequest request = new McpToolCallRequest();
        request.setTenantId("tenant-a");
        request.setTraceId("trace-a");
        request.setToolName("search");

        assertThat(client.call(server(), tool(), request).getStatus()).isEqualTo("SUCCEEDED");

        rejectToolCall = true;
        assertThatThrownBy(() -> client.call(server(), tool(), request))
                .isInstanceOf(com.campus.agent.mcp.error.McpBusinessException.class)
                .hasMessageContaining("downstream unavailable");
    }

    @Test
    void parsesInitializeResponseFromServerSentEvents() {
        streamInitializeResponse = true;
        McpHttpConnectivityClient client = new McpHttpConnectivityClient(
                objectMapper, Duration.ofSeconds(1), Duration.ofSeconds(1));

        client.initializeAndPing(server());

        assertThat(methods).containsExactly("initialize", "notifications/initialized", "ping");
    }

    private McpTool tool() {
        McpTool tool = new McpTool();
        tool.setToolName("search");
        tool.setVersion(1);
        return tool;
    }

    private McpServer server() {
        McpServer server = new McpServer();
        server.setTransportType("HTTP_SSE");
        server.setEndpoint("http://127.0.0.1:" + httpServer.getAddress().getPort() + "/mcp");
        return server;
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        Map<String, Object> request = objectMapper.readValue(
                exchange.getRequestBody(), new TypeReference<>() { });
        String method = (String) request.get("method");
        methods.add(method);
        sessionHeaders.add(exchange.getRequestHeaders().getFirst("Mcp-Session-Id"));
        protocolHeaders.add(exchange.getRequestHeaders().getFirst("MCP-Protocol-Version"));
        if ("notifications/initialized".equals(method)) {
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
            return;
        }

        Map<String, Object> response;
        if (rejectInitialize && "initialize".equals(method)) {
            response = Map.of("jsonrpc", "2.0", "id", request.get("id"),
                    "error", Map.of("code", -32603, "message", "not ready"));
        } else if ("initialize".equals(method)) {
            response = Map.of("jsonrpc", "2.0", "id", request.get("id"), "result",
                    Map.of("protocolVersion", "2025-03-26", "capabilities", Map.of(),
                            "serverInfo", Map.of("name", "test-server", "version", "1")));
        } else if (rejectToolCall && "tools/call".equals(method)) {
            response = Map.of("jsonrpc", "2.0", "id", request.get("id"),
                    "error", Map.of("code", -32603, "message", "downstream unavailable"));
        } else if ("tools/call".equals(method)) {
            response = Map.of("jsonrpc", "2.0", "id", request.get("id"), "result",
                    Map.of("content", List.of(Map.of("type", "text", "text", "ok")), "isError", false));
        } else {
            response = Map.of("jsonrpc", "2.0", "id", request.get("id"), "result", Map.of());
        }

        byte[] responseBody = objectMapper.writeValueAsString(response).getBytes(StandardCharsets.UTF_8);
        if ("initialize".equals(method) && !rejectInitialize) {
            exchange.getResponseHeaders().set("Mcp-Session-Id", "test-session");
        }
        if ((streamInitializeResponse && "initialize".equals(method))
                || (streamToolCallResponse && "tools/call".equals(method))) {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            String event = "event: message\ndata: " + new String(responseBody, StandardCharsets.UTF_8) + "\n\n";
            responseBody = event.getBytes(StandardCharsets.UTF_8);
        } else {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
        }
        exchange.sendResponseHeaders(200, responseBody.length);
        exchange.getResponseBody().write(responseBody);
        exchange.close();
    }
}
