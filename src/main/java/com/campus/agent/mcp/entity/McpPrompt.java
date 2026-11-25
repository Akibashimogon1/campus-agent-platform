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
@TableName(value = "mcp_prompt", autoResultMap = true)
public class McpPrompt extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("server_id")
    private Long serverId;

    @TableField("prompt_name")
    private String promptName;

    @TableField("description")
    private String description;

    @TableField(value = "argument_schema", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> argumentSchema;

    @TableField("prompt_template")
    private String promptTemplate;

    @TableField("permission_level")
    private String permissionLevel;

    @TableField("status")
    private String status;

    @TableField("version")
    private Integer version;
}
