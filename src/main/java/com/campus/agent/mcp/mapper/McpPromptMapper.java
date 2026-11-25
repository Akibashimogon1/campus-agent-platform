package com.campus.agent.mcp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.agent.mcp.entity.McpPrompt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface McpPromptMapper extends BaseMapper<McpPrompt> {

    @Select("SELECT * FROM mcp_prompt WHERE tenant_id = #{tenantId} AND deleted = 0 AND id = #{id}")
    McpPrompt selectByTenantAndId(@Param("tenantId") String tenantId, @Param("id") Long id);

    @Select("SELECT * FROM mcp_prompt WHERE tenant_id = #{tenantId} AND server_id = #{serverId} AND deleted = 0 AND status = 'ENABLED' ORDER BY prompt_name")
    List<McpPrompt> selectEnabledByTenantAndServer(@Param("tenantId") String tenantId,
                                                   @Param("serverId") Long serverId);

    @Select("SELECT * FROM mcp_prompt WHERE tenant_id = #{tenantId} AND prompt_name = #{promptName} AND deleted = 0")
    McpPrompt selectByTenantAndName(@Param("tenantId") String tenantId,
                                    @Param("promptName") String promptName);
}
