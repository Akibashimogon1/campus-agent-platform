package com.campus.agent.mcp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.agent.mcp.entity.McpServer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface McpServerMapper extends BaseMapper<McpServer> {

    @Select("SELECT * FROM mcp_server WHERE tenant_id = #{tenantId} AND deleted = 0 AND id = #{id}")
    McpServer selectByTenantAndId(@Param("tenantId") String tenantId, @Param("id") Long id);

        @Update("UPDATE mcp_server SET server_name = #{serverName}, transport_type = #{transportType}, "
            + "endpoint = #{endpoint}, process_config = #{processConfig,typeHandler=com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler}, "
            + "credential_ref = #{credentialRef}, status = #{status}, updated_at = CURRENT_TIMESTAMP "
            + "WHERE tenant_id = #{tenantId} AND id = #{id} AND deleted = 0")
        int updateConfigurationByTenant(@Param("tenantId") String tenantId,
                        @Param("id") Long id,
                        @Param("serverName") String serverName,
                        @Param("transportType") String transportType,
                        @Param("endpoint") String endpoint,
                        @Param("processConfig") java.util.Map<String, Object> processConfig,
                        @Param("credentialRef") String credentialRef,
                        @Param("status") String status);

        @Update("UPDATE mcp_server SET deleted = 1, updated_at = CURRENT_TIMESTAMP "
            + "WHERE tenant_id = #{tenantId} AND id = #{id} AND deleted = 0")
        int softDeleteByTenant(@Param("tenantId") String tenantId, @Param("id") Long id);

        @Update("UPDATE mcp_server SET health_status = #{healthStatus}, last_heartbeat_at = #{testedAt}, "
            + "last_health_error = #{failureReason}, updated_at = CURRENT_TIMESTAMP "
            + "WHERE tenant_id = #{tenantId} AND id = #{id} AND deleted = 0")
        int updateHealthByTenant(@Param("tenantId") String tenantId,
                     @Param("id") Long id,
                     @Param("healthStatus") String healthStatus,
                     @Param("testedAt") java.time.LocalDateTime testedAt,
                     @Param("failureReason") String failureReason);

    @Select("SELECT * FROM mcp_server WHERE tenant_id = #{tenantId} AND server_code = #{serverCode} AND deleted = 0")
    McpServer selectByTenantAndCode(@Param("tenantId") String tenantId,
                                    @Param("serverCode") String serverCode);

    @Select("SELECT * FROM mcp_server WHERE tenant_id = #{tenantId} AND deleted = 0 ORDER BY updated_at DESC")
    List<McpServer> selectListByTenant(@Param("tenantId") String tenantId);

    @Select("<script>SELECT * FROM mcp_server WHERE tenant_id = #{tenantId} AND deleted = 0 "
            + "<if test='status != null'> AND status = #{status} </if> "
            + "<if test='healthStatus != null'> AND health_status = #{healthStatus} </if> "
            + "ORDER BY updated_at DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<McpServer> selectPageByTenant(@Param("tenantId") String tenantId,
                                       @Param("status") String status,
                                       @Param("healthStatus") String healthStatus,
                                       @Param("limit") int limit,
                                       @Param("offset") long offset);

    @Select("<script>SELECT COUNT(*) FROM mcp_server WHERE tenant_id = #{tenantId} AND deleted = 0 "
            + "<if test='status != null'> AND status = #{status} </if> "
            + "<if test='healthStatus != null'> AND health_status = #{healthStatus} </if> "
            + "</script>")
    long countByTenantQuery(@Param("tenantId") String tenantId,
                            @Param("status") String status,
                            @Param("healthStatus") String healthStatus);

    @Select("SELECT * FROM mcp_server WHERE tenant_id = #{tenantId} AND status = #{status} AND deleted = 0 ORDER BY updated_at DESC")
    List<McpServer> selectListByTenantAndStatus(@Param("tenantId") String tenantId,
                                                @Param("status") String status);
}
