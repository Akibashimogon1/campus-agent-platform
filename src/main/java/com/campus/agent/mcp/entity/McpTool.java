package com.campus.agent.mcp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "mcp_tool", autoResultMap = true)
public class McpTool extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("server_id")
    private Long serverId;

    @TableField("tool_name")
    private String toolName;

    @TableField("display_name")
    private String displayName;

    @TableField("description")
    private String description;

    @TableField("category")
    private String category;

    @TableField("scope")
    private String scope;

    @TableField(value = "input_schema", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> inputSchema;

    @TableField(value = "output_schema", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> outputSchema;

    @TableField("permission_level")
    private String permissionLevel;

    @TableField("timeout_ms")
    private Integer timeoutMs;

    @TableField("max_output_size")
    private Integer maxOutputSize;

    @TableField("status")
    private String status;

    @TableField("version")
    private Integer version;
}
