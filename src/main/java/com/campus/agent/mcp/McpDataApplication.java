package com.campus.agent.mcp;

import org.mybatis.spring.annotation.MapperScan;
import com.campus.agent.mcp.cache.McpCacheProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(McpCacheProperties.class)
@MapperScan("com.campus.agent.mcp.mapper")
public class McpDataApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpDataApplication.class, args);
    }
}
