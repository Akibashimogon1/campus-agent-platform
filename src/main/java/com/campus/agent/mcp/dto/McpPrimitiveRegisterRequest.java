package com.campus.agent.mcp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class McpPrimitiveRegisterRequest {

    @Positive
    private Long serverId;

    @NotBlank
    @Pattern(regexp = "TOOL|RESOURCE|PROMPT")
    private String primitiveType;

    @NotBlank
    @Size(max = 128)
    private String name;

    @Size(max = 128)
    private String displayName;

    private String description;

    @Size(max = 64)
    private String category;

    @Size(max = 32)
    private String scope;

    private Map<String, Object> schema;

    private String mimeType;

    @Size(max = 512)
    private String uriTemplate;

    @Size(max = 32)
    private String permissionLevel;

    @Size(max = 32)
    private String status;

    @Positive
    private Integer timeoutMs;

    @Positive
    private Integer maxOutputSize;
}
