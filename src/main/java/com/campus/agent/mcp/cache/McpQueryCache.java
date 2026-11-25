package com.campus.agent.mcp.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class McpQueryCache {

    private final Cache<String, Object> serverCache;
    private final Cache<String, Object> toolCache;

    public McpQueryCache(McpCacheProperties properties) {
        McpCacheProperties.CacheSettings server = properties.getServer();
        McpCacheProperties.CacheSettings tool = properties.getTool();
        this.serverCache = Caffeine.newBuilder()
                .expireAfterWrite(server.getTtl())
                .maximumSize(server.getMaxSize())
                .recordStats()
                .build();
        this.toolCache = Caffeine.newBuilder()
                .expireAfterWrite(tool.getTtl())
                .maximumSize(tool.getMaxSize())
                .recordStats()
                .build();
    }

    public <T> T getServer(String key, Class<T> type) {
        return type.cast(serverCache.getIfPresent(key));
    }

    public void putServer(String key, Object value) {
        serverCache.put(key, value);
    }

    public <T> T getTool(String key, Class<T> type) {
        return type.cast(toolCache.getIfPresent(key));
    }

    public void putTool(String key, Object value) {
        toolCache.put(key, value);
    }

    public void invalidateServerTenant(String tenantId) {
        serverCache.asMap().keySet().removeIf(key -> key.startsWith("server|" + tenantId + "|"));
    }

    public void invalidateToolTenant(String tenantId) {
        toolCache.asMap().keySet().removeIf(key -> key.startsWith("tool|" + tenantId + "|"));
    }

    public String serverKey(String tenantId, String status, String healthStatus, int page, int size) {
        return "server|" + tenantId + "|status=" + normalize(status)
                + "|health=" + normalize(healthStatus)
                + "|page=" + page + "|size=" + size;
    }

    public String toolKey(String tenantId, String category, String scope, String status,
                          String keyword, int page, int size) {
        return "tool|" + tenantId + "|category=" + normalize(category)
                + "|scope=" + normalize(scope)
                + "|status=" + normalize(status)
                + "|keyword=" + normalize(keyword)
                + "|page=" + page + "|size=" + size;
    }

    private String normalize(String value) {
        return value == null ? "-" : value.trim().isEmpty() ? "-" : value.trim().replace("|", "%7C");
    }
}
