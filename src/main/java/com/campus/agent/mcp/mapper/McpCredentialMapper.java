package com.campus.agent.mcp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.agent.mcp.entity.McpCredential;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface McpCredentialMapper extends BaseMapper<McpCredential> {

    @Select("SELECT * FROM mcp_credential WHERE tenant_id = #{tenantId} AND deleted = 0 AND id = #{id}")
    McpCredential selectByTenantAndId(@Param("tenantId") String tenantId, @Param("id") Long id);

    @Select("SELECT * FROM mcp_credential WHERE tenant_id = #{tenantId} AND credential_name = #{credentialName} AND deleted = 0")
    McpCredential selectByTenantAndName(@Param("tenantId") String tenantId,
                                        @Param("credentialName") String credentialName);
}
