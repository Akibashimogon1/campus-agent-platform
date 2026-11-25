package com.campus.agent.mcp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.agent.mcp.entity.McpResource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface McpResourceMapper extends BaseMapper<McpResource> {

    @Select("SELECT * FROM mcp_resource WHERE tenant_id = #{tenantId} AND deleted = 0 AND id = #{id}")
    McpResource selectByTenantAndId(@Param("tenantId") String tenantId, @Param("id") Long id);

    @Select("SELECT * FROM mcp_resource WHERE tenant_id = #{tenantId} AND server_id = #{serverId} AND deleted = 0 AND status = 'ENABLED' ORDER BY resource_name")
    List<McpResource> selectEnabledByTenantAndServer(@Param("tenantId") String tenantId,
                                                     @Param("serverId") Long serverId);

    @Select("SELECT * FROM mcp_resource WHERE tenant_id = #{tenantId} AND resource_uri_template = #{uriTemplate} AND deleted = 0")
    McpResource selectByTenantAndUri(@Param("tenantId") String tenantId,
                                     @Param("uriTemplate") String uriTemplate);
}
