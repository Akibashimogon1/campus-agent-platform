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
@TableName("mcp_invocation")
public class McpInvocation extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("workflow_id")
    private String workflowId;

    @TableField("run_id")
    private String runId;

    @TableField("node_id")
    private String nodeId;

    @TableField("session_id")
    private String sessionId;

    @TableField("trace_id")
    private String traceId;

    @TableField("server_id")
    private Long serverId;

    @TableField("tool_id")
    private Long toolId;

    @TableField("primitive_type")
    private String primitiveType;

    @TableField("primitive_name")
    private String primitiveName;

    @TableField("primitive_version")
    private Integer primitiveVersion;

    @TableField(value = "request_summary", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> requestSummary;

    @TableField(value = "response_summary", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> responseSummary;

    @TableField("status")
    private String status;

    @TableField("error_code")
    private String errorCode;

    @TableField("latency_ms")
    private Long latencyMs;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;
}
