package com.campus.agent.mcp.config;

import com.campus.agent.mcp.entity.McpUser;
import com.campus.agent.mcp.mapper.McpUserMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevAdminInitializer implements ApplicationRunner {

    private final McpUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public DevAdminInitializer(McpUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        McpUser existing = userMapper.selectActiveByUsername("admin");
        if (existing != null) {
            return;
        }

        McpUser admin = new McpUser();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRole("ADMIN");
        admin.setTenantId("tenant-a");
        admin.setStatus("ENABLED");
        admin.setDeleted(0);
        userMapper.insert(admin);
    }
}
