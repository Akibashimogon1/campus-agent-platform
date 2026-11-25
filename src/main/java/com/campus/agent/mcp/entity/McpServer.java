package com.campus.agent.mcp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mcp_server")
public class McpServer extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("server_code")
    private String serverCode;

    @TableField("server_name")
    private String serverName;

    @TableField("transport_type")
    private String transportType;

    @TableField("endpoint")
    private String endpoint;

    @TableField(value = "process_config", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> processConfig;

    @TableField("credential_ref")
    private String credentialRef;

    @TableField("status")
    private String status;

    @TableField("health_status")
    private String healthStatus;

    @TableField("last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

    @TableField("last_health_error")
    private String lastHealthError;

    @TableField("version")
    private Integer version;
}
