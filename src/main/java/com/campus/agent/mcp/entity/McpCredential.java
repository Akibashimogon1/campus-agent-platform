package com.campus.agent.mcp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mcp_credential")
public class McpCredential extends BaseTenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("credential_name")
    private String credentialName;

    @TableField("credential_type")
    private String credentialType;

    @TableField("encrypted_value")
    private byte[] encryptedValue;

    @TableField("encryption_key_id")
    private String encryptionKeyId;

    @TableField("nonce")
    private byte[] nonce;

    @TableField("status")
    private String status;

    @TableField("last_used_at")
    private LocalDateTime lastUsedAt;

    @TableField("expires_at")
    private LocalDateTime expiresAt;
}
