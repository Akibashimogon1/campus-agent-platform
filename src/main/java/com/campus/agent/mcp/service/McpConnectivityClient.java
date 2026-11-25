package com.campus.agent.mcp.service;

import com.campus.agent.mcp.entity.McpServer;

public interface McpConnectivityClient {

    void initializeAndPing(McpServer server) throws Exception;
}