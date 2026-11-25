package com.campus.agent.mcp.controller;

import com.campus.agent.mcp.dto.McpCurrentUserResponse;
import com.campus.agent.mcp.dto.McpLoginRequest;
import com.campus.agent.mcp.dto.McpLoginResponse;
import com.campus.agent.mcp.error.McpGlobalExceptionHandler;
import com.campus.agent.mcp.service.McpUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Proxy;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class AuthControllerMockMvcTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        McpUserService service = (McpUserService) Proxy.newProxyInstance(
                McpUserService.class.getClassLoader(),
                new Class<?>[]{McpUserService.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "login" -> {
                        McpLoginRequest request = (McpLoginRequest) args[0];
                        if (!"admin123".equals(request.password())) {
                            throw new BadCredentialsException("用户名或密码错误");
                        }
                        yield new McpLoginResponse("test-token", "admin", "ADMIN", "tenant-a");
                    }
                    case "getCurrentUser" -> new McpCurrentUserResponse(1L, "admin", "ADMIN", "tenant-a");
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        AuthController controller = new AuthController(service);
        mockMvc = standaloneSetup(controller)
                .setControllerAdvice(new McpGlobalExceptionHandler())
                .build();
    }

    @Test
    void loginSuccessReturnsToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").value("test-token"));
    }

    @Test
    void wrongPasswordReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }
}
