package com.campus.agent.mcp.service;

import com.campus.agent.mcp.dto.McpLoginRequest;
import com.campus.agent.mcp.dto.McpLoginResponse;
import com.campus.agent.mcp.dto.McpCurrentUserResponse;

public interface McpUserService {
    McpLoginResponse login(McpLoginRequest request);

    McpCurrentUserResponse getCurrentUser(String username);
}
