package com.campus.agent.mcp.config;

import com.campus.agent.mcp.error.McpBusinessException;
import com.campus.agent.mcp.error.McpErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class McpInternalServiceKeyInterceptor implements HandlerInterceptor {

    public static final String HEADER_NAME = "X-Internal-Service-Key";

    private final String configuredServiceKey;
    private final boolean authenticationRequired;

    public McpInternalServiceKeyInterceptor(
            @Value("${mcp.internal.service-key:}") String configuredServiceKey,
            @Value("${mcp.internal.require-service-key:true}") boolean authenticationRequired) {
        this.configuredServiceKey = configuredServiceKey;
        this.authenticationRequired = authenticationRequired;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!authenticationRequired) {
            return true;
        }
        String suppliedServiceKey = request.getHeader(HEADER_NAME);
        if (configuredServiceKey.isBlank() || suppliedServiceKey == null
                || !MessageDigest.isEqual(
                        configuredServiceKey.getBytes(StandardCharsets.UTF_8),
                        suppliedServiceKey.getBytes(StandardCharsets.UTF_8))) {
            throw new McpBusinessException(McpErrorCode.INTERNAL_SERVICE_AUTH_FAILED,
                    "Internal service authentication failed");
        }
        return true;
    }
}