package com.campus.agent.mcp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.campus.agent.mcp.context.TenantContextInterceptor;

@Configuration
public class McpWebConfiguration implements WebMvcConfigurer {

    private final McpInternalServiceKeyInterceptor internalServiceKeyInterceptor;
    private final TenantContextInterceptor tenantContextInterceptor;

    public McpWebConfiguration(
            McpInternalServiceKeyInterceptor internalServiceKeyInterceptor,
            TenantContextInterceptor tenantContextInterceptor) {
        this.internalServiceKeyInterceptor = internalServiceKeyInterceptor;
        this.tenantContextInterceptor = tenantContextInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantContextInterceptor)
            .addPathPatterns("/api/v1/**", "/internal/v1/**");
        registry.addInterceptor(internalServiceKeyInterceptor)
                .addPathPatterns("/internal/v1/mcp/**");
    }
}