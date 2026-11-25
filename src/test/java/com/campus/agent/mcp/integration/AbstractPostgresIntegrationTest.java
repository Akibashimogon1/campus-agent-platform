package com.campus.agent.mcp.integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "mcp.execution.mode=mock",
        "mcp.internal.require-service-key=false",
        "spring.sql.init.mode=always"
})
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("campus_agent_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl() + "?stringtype=unspecified");
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.update("DELETE FROM mcp_invocation");
        jdbcTemplate.update("DELETE FROM mcp_tool");
        jdbcTemplate.update("DELETE FROM mcp_resource");
        jdbcTemplate.update("DELETE FROM mcp_prompt");
        jdbcTemplate.update("DELETE FROM mcp_credential");
        jdbcTemplate.update("DELETE FROM mcp_server");
    }
}
