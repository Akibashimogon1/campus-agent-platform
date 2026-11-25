package com.campus.agent.mcp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.agent.mcp.entity.McpTool;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface McpToolMapper extends BaseMapper<McpTool> {

    @Select("SELECT * FROM mcp_tool WHERE tenant_id = #{tenantId} AND deleted = 0 AND id = #{id}")
    McpTool selectByTenantAndId(@Param("tenantId") String tenantId, @Param("id") Long id);

    @Select("SELECT t.* FROM mcp_tool t JOIN mcp_server s ON s.id = t.server_id "
            + "WHERE t.tenant_id = #{tenantId} AND s.tenant_id = #{tenantId} "
            + "AND t.tool_name = #{toolName} AND t.deleted = 0 AND s.deleted = 0")
        List<McpTool> selectByTenantAndName(@Param("tenantId") String tenantId,
                                                                                @Param("toolName") String toolName);

    @Select("SELECT * FROM mcp_tool WHERE tenant_id = #{tenantId} AND deleted = 0 AND server_id = #{serverId} AND tool_name = #{toolName}")
    McpTool selectByTenantServerAndName(@Param("tenantId") String tenantId,
                                        @Param("serverId") Long serverId,
                                        @Param("toolName") String toolName);

    @Select("SELECT * FROM mcp_tool WHERE tenant_id = #{tenantId} AND deleted = 0 AND server_id = #{serverId} AND status = 'ENABLED' ORDER BY tool_name")
    List<McpTool> selectEnabledByTenantAndServer(@Param("tenantId") String tenantId,
                                                 @Param("serverId") Long serverId);

    @Select("SELECT * FROM mcp_tool WHERE tenant_id = #{tenantId} AND deleted = 0 "
            + "AND (CAST(#{category} AS VARCHAR) IS NULL OR category = CAST(#{category} AS VARCHAR)) "
            + "AND (CAST(#{scope} AS VARCHAR) IS NULL OR scope = CAST(#{scope} AS VARCHAR)) "
            + "AND (CAST(#{status} AS VARCHAR) IS NULL OR status = CAST(#{status} AS VARCHAR)) "
            + "AND (CAST(#{keyword} AS VARCHAR) IS NULL OR tool_name LIKE '%' || CAST(#{keyword} AS VARCHAR) || '%' OR description LIKE '%' || CAST(#{keyword} AS VARCHAR) || '%') "
            + "AND EXISTS (SELECT 1 FROM mcp_server s WHERE s.id = mcp_tool.server_id "
            + "AND s.tenant_id = #{tenantId} AND s.deleted = 0) "
            + "ORDER BY updated_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<McpTool> selectByTenantQuery(@Param("tenantId") String tenantId,
                                      @Param("category") String category,
                                      @Param("scope") String scope,
                                      @Param("status") String status,
                                      @Param("keyword") String keyword,
                                      @Param("limit") int limit,
                                      @Param("offset") long offset);

    @Select("SELECT COUNT(*) FROM mcp_tool WHERE tenant_id = #{tenantId} AND deleted = 0 "
            + "AND (CAST(#{category} AS VARCHAR) IS NULL OR category = CAST(#{category} AS VARCHAR)) "
            + "AND (CAST(#{scope} AS VARCHAR) IS NULL OR scope = CAST(#{scope} AS VARCHAR)) "
            + "AND (CAST(#{status} AS VARCHAR) IS NULL OR status = CAST(#{status} AS VARCHAR)) "
            + "AND (CAST(#{keyword} AS VARCHAR) IS NULL OR tool_name LIKE '%' || CAST(#{keyword} AS VARCHAR) || '%' OR description LIKE '%' || CAST(#{keyword} AS VARCHAR) || '%') "
            + "AND EXISTS (SELECT 1 FROM mcp_server s WHERE s.id = mcp_tool.server_id "
            + "AND s.tenant_id = #{tenantId} AND s.deleted = 0)")
    long countByTenantQuery(@Param("tenantId") String tenantId,
                            @Param("category") String category,
                            @Param("scope") String scope,
                            @Param("status") String status,
                            @Param("keyword") String keyword);

    @Update("UPDATE mcp_tool SET status = #{status}, updated_at = CURRENT_TIMESTAMP "
            + "WHERE tenant_id = #{tenantId} AND id = #{id} AND deleted = 0")
    int updateStatusByTenant(@Param("tenantId") String tenantId,
                             @Param("id") Long id,
                             @Param("status") String status);

    @Update("UPDATE mcp_tool SET deleted = 1, updated_at = CURRENT_TIMESTAMP "
            + "WHERE tenant_id = #{tenantId} AND server_id = #{serverId} AND deleted = 0")
    int softDeleteByServerTenant(@Param("tenantId") String tenantId,
                                 @Param("serverId") Long serverId);
}
