package com.campus.agent.mcp.error;

import org.springframework.http.HttpStatus;

public enum McpErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST),
    TENANT_CONTEXT_MISSING(HttpStatus.UNAUTHORIZED),
    INTERNAL_SERVICE_AUTH_FAILED(HttpStatus.UNAUTHORIZED),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    RESOURCE_CONFLICT(HttpStatus.CONFLICT),
    BUSINESS_ERROR(HttpStatus.UNPROCESSABLE_ENTITY),
    DOWNSTREAM_ERROR(HttpStatus.BAD_GATEWAY),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    McpErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}