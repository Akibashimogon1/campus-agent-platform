package com.campus.agent.mcp.service;

import com.campus.agent.mcp.dto.McpLoginRequest;
import com.campus.agent.mcp.dto.McpLoginResponse;
import com.campus.agent.mcp.dto.McpCurrentUserResponse;
import com.campus.agent.mcp.entity.McpUser;
import com.campus.agent.mcp.mapper.McpUserMapper;
import com.campus.agent.mcp.security.JwtTokenService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class McpUserServiceImpl implements McpUserService {

    private final McpUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public McpUserServiceImpl(
            McpUserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public McpLoginResponse login(McpLoginRequest request) {
        McpUser user = userMapper.selectActiveByUsername(request.username());
        if (user == null || !"ENABLED".equals(user.getStatus())
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("用户名或密码错误");
        }
        return new McpLoginResponse(
                jwtTokenService.generate(user),
                user.getUsername(),
                user.getRole(),
                user.getTenantId());
    }

    @Override
    public McpCurrentUserResponse getCurrentUser(String username) {
        McpUser user = userMapper.selectActiveByUsername(username);
        if (user == null || !"ENABLED".equals(user.getStatus())) {
            throw new BadCredentialsException("用户不存在或已禁用");
        }
        return new McpCurrentUserResponse(user.getId(), user.getUsername(), user.getRole(), user.getTenantId());
    }
}
