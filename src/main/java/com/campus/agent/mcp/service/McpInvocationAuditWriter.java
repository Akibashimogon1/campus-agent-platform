package com.campus.agent.mcp.service;

import com.campus.agent.mcp.entity.McpInvocation;
import com.campus.agent.mcp.error.McpBusinessException;
import com.campus.agent.mcp.error.McpErrorCode;
import com.campus.agent.mcp.mapper.McpInvocationMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class McpInvocationAuditWriter {

    private final McpInvocationMapper invocationMapper;

    public McpInvocationAuditWriter(McpInvocationMapper invocationMapper) {
        this.invocationMapper = invocationMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(McpInvocation invocation) {
        if (invocationMapper.insert(invocation) != 1) {
            throw new McpBusinessException(McpErrorCode.BUSINESS_ERROR,
                    "MCP invocation audit record could not be saved");
        }
    }
}
