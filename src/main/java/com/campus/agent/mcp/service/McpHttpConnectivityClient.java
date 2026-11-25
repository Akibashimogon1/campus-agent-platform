package com.campus.agent.mcp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.campus.agent.mcp.entity.McpServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class McpHttpConnectivityClient implements McpConnectivityClient {

    private static final String PROTOCOL_VERSION = "2025-03-26";

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public McpHttpConnectivityClient(
            ObjectMapper objectMapper,
            @Value("${mcp.connectivity.connect-timeout:PT2S}") Duration connectTimeout,
            @Value("${mcp.connectivity.read-timeout:PT5S}") Duration readTimeout) {
        this.objectMapper = objectMapper;
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public void initializeAndPing(McpServer server) {
        if (!"HTTP_SSE".equals(server.getTransportType())) {
            throw new UnsupportedOperationException("Connectivity tests for STDIO servers are not configured");
        }
        if (server.getEndpoint() == null || server.getEndpoint().isBlank()) {
            throw new IllegalArgumentException("Server endpoint is not configured");
        }

        Map<String, Object> initialize = request(1, "initialize", Map.of(
                "protocolVersion", PROTOCOL_VERSION,
                "capabilities", Map.of(),
                "clientInfo", Map.of("name", "campus-mcp-gateway", "version", "1.0")));
        RpcResponse initializeRpcResponse = post(server.getEndpoint(), initialize, null);
        Map<String, Object> initializeResponse = initializeRpcResponse.body();
        Object result = initializeResponse.get("result");
        if (!(result instanceof Map<?, ?> resultMap) || !resultMap.containsKey("protocolVersion")) {
            throw new IllegalStateException("MCP initialize response is missing result.protocolVersion");
        }

        Map<String, Object> initialized = new HashMap<>();
        initialized.put("jsonrpc", "2.0");
        initialized.put("method", "notifications/initialized");
        notify(server.getEndpoint(), initialized, initializeRpcResponse.sessionId());

        Map<String, Object> pingResponse = post(
            server.getEndpoint(), request(2, "ping", Map.of()), initializeRpcResponse.sessionId()).body();
        if (!pingResponse.containsKey("result")) {
            throw new IllegalStateException("MCP ping response is missing result");
        }
    }

    private Map<String, Object> request(int id, String method, Map<String, Object> params) {
        return Map.of("jsonrpc", "2.0", "id", id, "method", method, "params", params);
    }

    private RpcResponse post(String endpoint, Map<String, Object> request, String sessionId) {
        ResponseEntity<String> response = restClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .header("MCP-Protocol-Version", PROTOCOL_VERSION)
                .headers(headers -> addSessionId(headers, sessionId))
                .body(request)
                .retrieve()
                .toEntity(String.class);
        Map<String, Object> responseBody = parseResponse(response);
        if (responseBody.containsKey("error")) {
            throw new IllegalStateException("MCP server returned an invalid JSON-RPC response");
        }
        return new RpcResponse(responseBody, response.getHeaders().getFirst("Mcp-Session-Id"));
    }

    private void notify(String endpoint, Map<String, Object> notification, String sessionId) {
        restClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .header("MCP-Protocol-Version", PROTOCOL_VERSION)
                .headers(headers -> addSessionId(headers, sessionId))
                .body(notification)
                .retrieve()
                .toBodilessEntity();
    }

    private void addSessionId(org.springframework.http.HttpHeaders headers, String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            headers.set("Mcp-Session-Id", sessionId);
        }
    }

    private Map<String, Object> parseResponse(ResponseEntity<String> response) {
        String body = response.getBody();
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("MCP server returned an empty JSON-RPC response");
        }
        MediaType contentType = response.getHeaders().getContentType();
        try {
            if (contentType != null && contentType.isCompatibleWith(MediaType.TEXT_EVENT_STREAM)) {
                return parseServerSentEvent(body);
            }
            return objectMapper.readValue(body, new TypeReference<>() { });
        } catch (IOException exception) {
            throw new IllegalStateException("MCP server returned malformed JSON-RPC data", exception);
        }
    }

    private Map<String, Object> parseServerSentEvent(String body) throws IOException {
        List<String> dataLines = new ArrayList<>();
        for (String line : body.split("\\R")) {
            if (line.startsWith("data:")) {
                dataLines.add(line.substring("data:".length()).stripLeading());
            } else if (line.isBlank() && !dataLines.isEmpty()) {
                break;
            }
        }
        if (dataLines.isEmpty()) {
            throw new IllegalStateException("MCP server sent an event stream without JSON data");
        }
        return objectMapper.readValue(String.join("\n", dataLines), new TypeReference<>() { });
    }

    private record RpcResponse(Map<String, Object> body, String sessionId) {
    }
}