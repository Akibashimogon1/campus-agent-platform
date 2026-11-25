package com.campus.agent.mcp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.agent.mcp.entity.McpUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface McpUserMapper extends BaseMapper<McpUser> {

    @Select("SELECT * FROM mcp_user WHERE username = #{username} AND deleted = 0")
    McpUser selectActiveByUsername(@Param("username") String username);
}
