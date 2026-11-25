package com.campus.agent.mcp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.campus.agent.mcp.mapper")
public class McpDataApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpDataApplication.class, args);
    }
}
