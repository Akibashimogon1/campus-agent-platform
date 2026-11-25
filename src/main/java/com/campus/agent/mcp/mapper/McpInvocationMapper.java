package com.campus.agent.mcp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.agent.mcp.entity.McpInvocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface McpInvocationMapper extends BaseMapper<McpInvocation> {

    @Select("SELECT * FROM mcp_invocation WHERE tenant_id = #{tenantId} AND deleted = 0 AND id = #{id}")
    McpInvocation selectByTenantAndId(@Param("tenantId") String tenantId, @Param("id") Long id);

    @Select("SELECT * FROM mcp_invocation WHERE tenant_id = #{tenantId} AND run_id = #{runId} AND deleted = 0 ORDER BY started_at ASC")
    List<McpInvocation> selectByTenantAndRun(@Param("tenantId") String tenantId,
                                             @Param("runId") String runId);

    @Select("SELECT * FROM mcp_invocation WHERE tenant_id = #{tenantId} AND trace_id = #{traceId} AND deleted = 0 ORDER BY started_at ASC")
    List<McpInvocation> selectByTenantAndTrace(@Param("tenantId") String tenantId,
                                               @Param("traceId") String traceId);

    @Select("SELECT * FROM mcp_invocation WHERE tenant_id = #{tenantId} AND deleted = 0 "
            + "AND (CAST(#{primitiveType} AS VARCHAR) IS NULL OR primitive_type = CAST(#{primitiveType} AS VARCHAR)) "
            + "AND (CAST(#{status} AS VARCHAR) IS NULL OR status = CAST(#{status} AS VARCHAR)) "
            + "ORDER BY started_at DESC")
    List<McpInvocation> selectByTenantQuery(@Param("tenantId") String tenantId,
                                            @Param("primitiveType") String primitiveType,
                                            @Param("status") String status);

    @Select("<script>SELECT * FROM mcp_invocation WHERE tenant_id = #{tenantId} AND deleted = 0 "
            + "<if test='toolId != null'> AND tool_id = #{toolId} </if>"
            + "<if test='status != null'> AND status = #{status} </if>"
            + "<if test='startedAfter != null'> AND started_at &gt;= #{startedAfter} </if>"
            + "<if test='startedBefore != null'> AND started_at &lt;= #{startedBefore} </if>"
            + "ORDER BY started_at DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<McpInvocation> selectPageByTenant(@Param("tenantId") String tenantId,
                                           @Param("toolId") Long toolId,
                                           @Param("status") String status,
                                           @Param("startedAfter") LocalDateTime startedAfter,
                                           @Param("startedBefore") LocalDateTime startedBefore,
                                           @Param("limit") int limit,
                                           @Param("offset") long offset);

    @Select("<script>SELECT COUNT(*) FROM mcp_invocation WHERE tenant_id = #{tenantId} AND deleted = 0 "
            + "<if test='toolId != null'> AND tool_id = #{toolId} </if>"
            + "<if test='status != null'> AND status = #{status} </if>"
            + "<if test='startedAfter != null'> AND started_at &gt;= #{startedAfter} </if>"
            + "<if test='startedBefore != null'> AND started_at &lt;= #{startedBefore} </if>"
            + "</script>")
    long countByTenantQuery(@Param("tenantId") String tenantId,
                            @Param("toolId") Long toolId,
                            @Param("status") String status,
                            @Param("startedAfter") LocalDateTime startedAfter,
                            @Param("startedBefore") LocalDateTime startedBefore);
}
