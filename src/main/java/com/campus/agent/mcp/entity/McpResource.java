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
@TableName(value = "mcp_resource", autoResultMap = true)
public class McpResource extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("server_id")
    private Long serverId;

    @TableField("resource_uri_template")
    private String resourceUriTemplate;

    @TableField("resource_name")
    private String resourceName;

    @TableField("description")
    private String description;

    @TableField("mime_type")
    private String mimeType;

    @TableField("permission_level")
    private String permissionLevel;

    @TableField(value = "read_schema", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> readSchema;

    @TableField("status")
    private String status;

    @TableField("version")
    private Integer version;
}
