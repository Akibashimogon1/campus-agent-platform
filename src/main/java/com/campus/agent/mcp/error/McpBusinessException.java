package com.campus.agent.mcp.error;

public class McpBusinessException extends RuntimeException {

    private final McpErrorCode errorCode;

    public McpBusinessException(McpErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public McpErrorCode getErrorCode() {
        return errorCode;
    }
}