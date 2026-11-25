package com.campus.agent.mcp.controller;

import com.campus.agent.mcp.api.McpApiResponse;
import com.campus.agent.mcp.dto.McpCurrentUserResponse;
import com.campus.agent.mcp.dto.McpLoginRequest;
import com.campus.agent.mcp.dto.McpLoginResponse;
import com.campus.agent.mcp.service.McpUserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final McpUserService userService;

    public AuthController(McpUserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public McpApiResponse<McpLoginResponse> login(@Valid @RequestBody McpLoginRequest request) {
        return McpApiResponse.success(userService.login(request));
    }

    @GetMapping("/me")
    public McpApiResponse<McpCurrentUserResponse> me(Authentication authentication) {
        return McpApiResponse.success(userService.getCurrentUser(authentication.getName()));
    }
}
