package com.campus.agent.mcp.service;

import com.campus.agent.mcp.dto.McpCallResponse;
import com.campus.agent.mcp.dto.McpToolCallRequest;
import com.campus.agent.mcp.entity.McpServer;
import com.campus.agent.mcp.entity.McpTool;
import com.campus.agent.mcp.error.McpBusinessException;
import com.campus.agent.mcp.error.McpErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "mcp.execution.mode", havingValue = "http", matchIfMissing = true)
public class McpHttpToolExecutionClient implements McpToolExecutionClient {

    private static final String PROTOCOL_VERSION = "2025-03-26";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Duration defaultCallTimeout;

    public McpHttpToolExecutionClient(
            ObjectMapper objectMapper,
            @Value("${mcp.execution.connect-timeout:PT2S}") Duration connectTimeout,
            @Value("${mcp.execution.call-timeout:PT10S}") Duration defaultCallTimeout) {
        this.objectMapper = objectMapper;
        this.defaultCallTimeout = defaultCallTimeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
    }

    @Override
    public McpCallResponse call(McpServer server, McpTool tool, McpToolCallRequest request) throws Exception {
        if (!"HTTP_SSE".equals(server.getTransportType())) {
            throw new UnsupportedOperationException("Real MCP tool calls only support HTTP_SSE transport");
        }
        if (server.getEndpoint() == null || server.getEndpoint().isBlank()) {
            throw new IllegalArgumentException("Server endpoint is not configured");
        }

        Duration timeout = request.getTimeoutMs() == null
                ? defaultCallTimeout
                : Duration.ofMillis(request.getTimeoutMs());

        RpcResponse initialize = post(server.getEndpoint(), Map.of(
                "jsonrpc", "2.0",
                "id", 1001,
                "method", "initialize",
                "params", Map.of(
                        "protocolVersion", PROTOCOL_VERSION,
                        "capabilities", Map.of(),
                        "clientInfo", Map.of("name", "campus-mcp-gateway", "version", "1.0"))),
                null,
                timeout);
        requireResult(initialize.body(), "initialize");

        post(server.getEndpoint(), Map.of(
                        "jsonrpc", "2.0",
                        "method", "notifications/initialized"),
                initialize.sessionId(), timeout);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", tool.getToolName());
        params.put("arguments", request.getArguments() == null ? Map.of() : request.getArguments());

        RpcResponse callResponse = post(server.getEndpoint(), Map.of(
                        "jsonrpc", "2.0",
                        "id", 1002,
                        "method", "tools/call",
                        "params", params),
                initialize.sessionId(), timeout);

        if (callResponse.body().containsKey("error")) {
            throw jsonRpcError(callResponse.body().get("error"));
        }

        Object resultValue = callResponse.body().get("result");
        if (!(resultValue instanceof Map<?, ?> result)) {
            throw new McpBusinessException(McpErrorCode.DOWNSTREAM_ERROR,
                    "MCP tools/call response is missing result");
        }

        McpCallResponse response = new McpCallResponse();
        Object isError = result.get("isError");
        if (Boolean.TRUE.equals(isError)) {
            response.setStatus("FAILED");
            response.setRetryable(false);
            response.setErrorCode(McpErrorCode.DOWNSTREAM_ERROR.name());
            response.setMessage(extractText(result));
        } else {
            response.setStatus("SUCCEEDED");
            response.setRetryable(false);
            response.setMessage("MCP tool call succeeded");
        }

        Map<String, Object> output = new LinkedHashMap<>();
        if (result.get("structuredContent") instanceof Map<?, ?> structured) {
            for (Map.Entry<?, ?> entry : structured.entrySet()) {
                output.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        } else if (result.get("content") instanceof List<?> content) {
            output.put("content", content);
        }
        response.setOutput(output);

        if ("FAILED".equals(response.getStatus())) {
            throw new McpBusinessException(McpErrorCode.DOWNSTREAM_ERROR,
                    response.getMessage() == null ? "MCP tool returned isError=true" : response.getMessage());
        }
        return response;
    }

    private RpcResponse post(String endpoint, Map<String, Object> payload, String sessionId,
                             Duration timeout) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .header("MCP-Protocol-Version", PROTOCOL_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));
        if (sessionId != null && !sessionId.isBlank()) {
            builder.header("Mcp-Session-Id", sessionId);
        }

        HttpResponse<String> response;
        try {
            response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (java.net.http.HttpTimeoutException exception) {
            throw new McpBusinessException(McpErrorCode.DOWNSTREAM_ERROR,
                    "MCP downstream call timed out");
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new McpBusinessException(McpErrorCode.DOWNSTREAM_ERROR,
                    "MCP downstream HTTP status " + response.statusCode());
        }

        String body = response.body();
        if (body == null || body.isBlank()) {
            return new RpcResponse(Map.of(), response.headers().firstValue("Mcp-Session-Id").orElse(sessionId));
        }

        String contentType = response.headers().firstValue("Content-Type").orElse("application/json");
        Map<String, Object> rpcBody = contentType.toLowerCase().contains("text/event-stream")
                ? parseServerSentEvent(body)
                : objectMapper.readValue(body, new TypeReference<>() { });
        String returnedSessionId = response.headers().firstValue("Mcp-Session-Id").orElse(sessionId);
        return new RpcResponse(rpcBody, returnedSessionId);
    }

    private Map<String, Object> parseServerSentEvent(String body) throws Exception {
        List<String> dataLines = new ArrayList<>();
        for (String line : body.split("\\R")) {
            if (line.startsWith("data:")) {
                dataLines.add(line.substring(5).stripLeading());
            } else if (line.isBlank() && !dataLines.isEmpty()) {
                break;
            }
        }
        if (dataLines.isEmpty()) {
            throw new IllegalStateException("MCP server sent an event stream without JSON data");
        }
        return objectMapper.readValue(String.join("\n", dataLines), new TypeReference<>() { });
    }

    private void requireResult(Map<String, Object> body, String operation) {
        if (body.containsKey("error")) {
            throw jsonRpcError(body.get("error"));
        }
        if (!body.containsKey("result")) {
            throw new McpBusinessException(McpErrorCode.DOWNSTREAM_ERROR,
                    "MCP " + operation + " response is missing result");
        }
    }

    private McpBusinessException jsonRpcError(Object errorValue) {
        String message = "MCP JSON-RPC error";
        if (errorValue instanceof Map<?, ?> error) {
            Object dataMessage = error.get("message");
            if (dataMessage != null) {
                message = String.valueOf(dataMessage);
            }
        }
        return new McpBusinessException(McpErrorCode.DOWNSTREAM_ERROR, message);
    }

    private String extractText(Map<?, ?> result) {
        if (result.get("content") instanceof List<?> content) {
            for (Object item : content) {
                if (item instanceof Map<?, ?> itemMap && itemMap.get("text") != null) {
                    return String.valueOf(itemMap.get("text"));
                }
            }
        }
        return "MCP tool returned isError=true";
    }

    private record RpcResponse(Map<String, Object> body, String sessionId) {
    }
}
