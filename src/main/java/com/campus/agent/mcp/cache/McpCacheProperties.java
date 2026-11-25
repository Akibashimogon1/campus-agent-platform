package com.campus.agent.mcp.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "mcp.cache")
public class McpCacheProperties {

    private CacheSettings server = new CacheSettings(Duration.ofMinutes(5), 1000);
    private CacheSettings tool = new CacheSettings(Duration.ofMinutes(5), 1000);

    public CacheSettings getServer() {
        return server;
    }

    public void setServer(CacheSettings server) {
        this.server = server;
    }

    public CacheSettings getTool() {
        return tool;
    }

    public void setTool(CacheSettings tool) {
        this.tool = tool;
    }

    public static class CacheSettings {
        private Duration ttl;
        private long maxSize;

        public CacheSettings() {
        }

        public CacheSettings(Duration ttl, long maxSize) {
            this.ttl = ttl;
            this.maxSize = maxSize;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }

        public long getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(long maxSize) {
            this.maxSize = maxSize;
        }
    }
}
