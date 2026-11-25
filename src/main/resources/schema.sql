-- MCP Gateway PostgreSQL Schema
-- Tenant ID is mandatory for every tenant-owned record.

CREATE TABLE IF NOT EXISTS mcp_server (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id           VARCHAR(64)       NOT NULL,
    server_code         VARCHAR(128)      NOT NULL,
    server_name         VARCHAR(128)      NOT NULL,
    transport_type      VARCHAR(32)       NOT NULL,
    endpoint            VARCHAR(1024),
    process_config      JSONB,
    credential_ref      VARCHAR(128),
    status              VARCHAR(32)       NOT NULL DEFAULT 'REGISTERED',
    health_status       VARCHAR(32)       NOT NULL DEFAULT 'UNKNOWN',
    last_heartbeat_at   TIMESTAMP,
    last_health_error   VARCHAR(512),
    version             INTEGER           NOT NULL DEFAULT 1,
    deleted             SMALLINT          NOT NULL DEFAULT 0,
    created_by          VARCHAR(64),
    updated_by          VARCHAR(64),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_mcp_server_tenant_code UNIQUE (tenant_id, server_code, deleted)
);

COMMENT ON TABLE mcp_server IS 'MCP服务实例注册信息';
COMMENT ON COLUMN mcp_server.tenant_id IS '租户ID，多租户隔离主键之一';
COMMENT ON COLUMN mcp_server.transport_type IS '传输类型：HTTP_SSE、STDIO';
COMMENT ON COLUMN mcp_server.deleted IS '逻辑删除标记：0未删除，1已删除';

ALTER TABLE mcp_server ADD COLUMN IF NOT EXISTS last_health_error VARCHAR(512);

CREATE TABLE IF NOT EXISTS mcp_tool (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id           VARCHAR(64)       NOT NULL,
    server_id           BIGINT            NOT NULL,
    tool_name           VARCHAR(128)      NOT NULL,
    display_name        VARCHAR(128),
    description         TEXT,
    category            VARCHAR(64)       NOT NULL DEFAULT 'GENERAL',
    scope               VARCHAR(32)       NOT NULL DEFAULT 'WORKFLOW',
    input_schema        JSONB,
    output_schema       JSONB,
    permission_level    VARCHAR(32)       NOT NULL DEFAULT 'TENANT',
    timeout_ms          INTEGER,
    max_output_size     INTEGER,
    status              VARCHAR(32)       NOT NULL DEFAULT 'ENABLED',
    version             INTEGER           NOT NULL DEFAULT 1,
    deleted             SMALLINT          NOT NULL DEFAULT 0,
    created_by          VARCHAR(64),
    updated_by          VARCHAR(64),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_mcp_tool_tenant_name UNIQUE (tenant_id, server_id, tool_name, deleted),
    CONSTRAINT fk_mcp_tool_server FOREIGN KEY (server_id)
        REFERENCES mcp_server(id)
);

COMMENT ON TABLE mcp_tool IS 'MCP工具原语';
COMMENT ON COLUMN mcp_tool.category IS '工具分类，如查询、写入、审批、通知';
COMMENT ON COLUMN mcp_tool.scope IS '工具作用域，如租户级、工作流级、节点级';

CREATE TABLE IF NOT EXISTS mcp_resource (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id             VARCHAR(64)       NOT NULL,
    server_id             BIGINT            NOT NULL,
    resource_uri_template VARCHAR(512)      NOT NULL,
    resource_name         VARCHAR(128)      NOT NULL,
    description           TEXT,
    mime_type             VARCHAR(128),
    permission_level      VARCHAR(32)       NOT NULL DEFAULT 'TENANT',
    read_schema           JSONB,
    status                VARCHAR(32)       NOT NULL DEFAULT 'ENABLED',
    version               INTEGER           NOT NULL DEFAULT 1,
    deleted               SMALLINT          NOT NULL DEFAULT 0,
    created_by            VARCHAR(64),
    updated_by            VARCHAR(64),
    created_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_mcp_resource_tenant_uri UNIQUE (tenant_id, server_id, resource_uri_template, deleted),
    CONSTRAINT fk_mcp_resource_server FOREIGN KEY (server_id)
        REFERENCES mcp_server(id)
);

COMMENT ON TABLE mcp_resource IS 'MCP资源原语';

CREATE TABLE IF NOT EXISTS mcp_prompt (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id          VARCHAR(64)       NOT NULL,
    server_id          BIGINT            NOT NULL,
    prompt_name        VARCHAR(128)      NOT NULL,
    description        TEXT,
    argument_schema    JSONB,
    prompt_template    TEXT,
    permission_level   VARCHAR(32)       NOT NULL DEFAULT 'TENANT',
    status             VARCHAR(32)       NOT NULL DEFAULT 'ENABLED',
    version            INTEGER           NOT NULL DEFAULT 1,
    deleted            SMALLINT          NOT NULL DEFAULT 0,
    created_by         VARCHAR(64),
    updated_by         VARCHAR(64),
    created_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_mcp_prompt_tenant_name UNIQUE (tenant_id, server_id, prompt_name, deleted),
    CONSTRAINT fk_mcp_prompt_server FOREIGN KEY (server_id)
        REFERENCES mcp_server(id)
);

COMMENT ON TABLE mcp_prompt IS 'MCP提示词原语';

CREATE TABLE IF NOT EXISTS mcp_credential (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id          VARCHAR(64)       NOT NULL,
    credential_name    VARCHAR(128)      NOT NULL,
    credential_type    VARCHAR(32)       NOT NULL,
    encrypted_value    BYTEA             NOT NULL,
    encryption_key_id  VARCHAR(128)      NOT NULL,
    nonce              BYTEA             NOT NULL,
    status             VARCHAR(32)       NOT NULL DEFAULT 'ENABLED',
    last_used_at       TIMESTAMP,
    expires_at         TIMESTAMP,
    deleted            SMALLINT          NOT NULL DEFAULT 0,
    created_by         VARCHAR(64),
    updated_by         VARCHAR(64),
    created_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_mcp_credential_tenant_name UNIQUE (tenant_id, credential_name, deleted)
);

COMMENT ON TABLE mcp_credential IS '租户AES-GCM加密凭证';
COMMENT ON COLUMN mcp_credential.encrypted_value IS '密文凭证，不允许明文落库';
COMMENT ON COLUMN mcp_credential.nonce IS 'AES-GCM随机nonce';

CREATE TABLE IF NOT EXISTS mcp_invocation (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id           VARCHAR(64)       NOT NULL,
    workflow_id         VARCHAR(64),
    run_id              VARCHAR(64),
    node_id             VARCHAR(64),
    session_id          VARCHAR(64),
    trace_id            VARCHAR(64)       NOT NULL,
    server_id           BIGINT,
    tool_id             BIGINT,
    primitive_type      VARCHAR(16)       NOT NULL,
    primitive_name      VARCHAR(128)      NOT NULL,
    primitive_version   INTEGER,
    request_summary     JSONB,
    response_summary    JSONB,
    status              VARCHAR(32)       NOT NULL,
    error_code          VARCHAR(64),
    latency_ms          BIGINT,
    started_at          TIMESTAMP       NOT NULL,
    finished_at         TIMESTAMP,
    deleted             SMALLINT          NOT NULL DEFAULT 0,
    created_by          VARCHAR(64),
    updated_by          VARCHAR(64),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mcp_invocation_server FOREIGN KEY (server_id)
        REFERENCES mcp_server(id)
);

COMMENT ON TABLE mcp_invocation IS 'MCP工具、资源、提示词调用审计记录';

CREATE INDEX IF NOT EXISTS idx_mcp_server_tenant_status ON mcp_server (tenant_id, status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_mcp_server_tenant_health ON mcp_server (tenant_id, health_status) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_mcp_tool_tenant_server ON mcp_tool (tenant_id, server_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_mcp_tool_tenant_category ON mcp_tool (tenant_id, category) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_mcp_tool_tenant_scope_status ON mcp_tool (tenant_id, scope, status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_mcp_tool_name ON mcp_tool (tool_name) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_mcp_resource_tenant_server ON mcp_resource (tenant_id, server_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_mcp_resource_status ON mcp_resource (tenant_id, status) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_mcp_prompt_tenant_server ON mcp_prompt (tenant_id, server_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_mcp_prompt_status ON mcp_prompt (tenant_id, status) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_mcp_credential_tenant_status ON mcp_credential (tenant_id, status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_mcp_credential_expiry ON mcp_credential (expires_at) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_mcp_invocation_tenant_trace ON mcp_invocation (tenant_id, trace_id);
ALTER TABLE mcp_invocation ADD COLUMN IF NOT EXISTS tool_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_mcp_invocation_tenant_tool ON mcp_invocation (tenant_id, tool_id, started_at);
CREATE INDEX IF NOT EXISTS idx_mcp_invocation_tenant_run ON mcp_invocation (tenant_id, run_id);
CREATE INDEX IF NOT EXISTS idx_mcp_invocation_tenant_status ON mcp_invocation (tenant_id, status, started_at);
CREATE INDEX IF NOT EXISTS idx_mcp_invocation_tenant_primitive ON mcp_invocation (tenant_id, primitive_type, primitive_name);
