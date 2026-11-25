-- Development-only seed data for the MCP management pages.

INSERT INTO mcp_server (
    tenant_id, server_code, server_name, transport_type, endpoint,
    status, health_status, version, deleted, created_at, updated_at
)
SELECT seed.tenant_id, seed.server_code, seed.server_name, seed.transport_type, seed.endpoint,
       seed.status, 'UNKNOWN', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    VALUES
        ('tenant-a', 'orders-dev', '订单 MCP Server', 'HTTP_SSE', 'http://127.0.0.1:19001/mcp', 'ENABLED'),
        ('tenant-a', 'notify-dev', '通知 MCP Server', 'HTTP_SSE', 'http://127.0.0.1:19002/mcp', 'REGISTERED')
) AS seed(tenant_id, server_code, server_name, transport_type, endpoint, status)
WHERE NOT EXISTS (
    SELECT 1 FROM mcp_server s
    WHERE s.tenant_id = seed.tenant_id
      AND s.server_code = seed.server_code
      AND s.deleted = 0
);

INSERT INTO mcp_tool (
    tenant_id, server_id, tool_name, display_name, description, category, scope,
    input_schema, output_schema, permission_level, timeout_ms, max_output_size,
    status, version, deleted, created_at, updated_at
)
SELECT
    'tenant-a', s.id, seed.tool_name, seed.display_name, seed.description, seed.category, seed.scope,
    '{}'::jsonb, '{}'::jsonb, 'TENANT', 5000, 65536,
    seed.status, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    VALUES
        ('search_orders', '订单查询', '按订单号或客户查询订单', 'query', 'TENANT', 'ENABLED', 'orders-dev'),
        ('approve_refund', '退款审批', '提交退款审批请求', 'approve', 'WORKFLOW', 'ENABLED', 'orders-dev'),
        ('send_notification', '发送通知', '向指定渠道发送通知', 'notify', 'NODE', 'DISABLED', 'notify-dev')
) AS seed(tool_name, display_name, description, category, scope, status, server_code)
JOIN mcp_server s ON s.tenant_id = 'tenant-a' AND s.server_code = seed.server_code AND s.deleted = 0
WHERE NOT EXISTS (
    SELECT 1 FROM mcp_tool t
    WHERE t.tenant_id = 'tenant-a'
      AND t.server_id = s.id
      AND t.tool_name = seed.tool_name
      AND t.deleted = 0
);

INSERT INTO mcp_invocation (
    tenant_id, workflow_id, run_id, node_id, session_id, trace_id,
    server_id, tool_id, primitive_type, primitive_name, primitive_version,
    request_summary, response_summary, status, error_code, latency_ms,
    started_at, finished_at, deleted, created_at, updated_at
)
SELECT
    'tenant-a', seed.workflow_id, seed.run_id, seed.node_id, seed.session_id, seed.trace_id,
    t.server_id, t.id, 'TOOL', t.tool_name, t.version,
    seed.request_summary::jsonb, seed.response_summary::jsonb, seed.status, seed.error_code, seed.latency_ms,
    seed.started_at::timestamp, seed.finished_at::timestamp, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    VALUES
        ('trace-demo-001', 'search_orders', 'workflow-orders', 'run-001', 'node-search', 'session-001', 'SUCCEEDED', NULL, 42, '2026-11-24 08:10:00', '2026-11-24 08:10:00', '{"argumentCount":2}' , '{"outputFieldCount":5}'),
        ('trace-demo-002', 'approve_refund', 'workflow-refund', 'run-002', 'node-approval', 'session-002', 'FAILED', 'DOWNSTREAM_ERROR', 135, '2026-11-24 08:12:00', '2026-11-24 08:12:00', '{"argumentCount":3}' , '{"status":"FAILED"}'),
        ('trace-demo-003', 'send_notification', 'workflow-notify', 'run-003', 'node-send', 'session-003', 'SUCCEEDED', NULL, 68, '2026-11-24 08:15:00', '2026-11-24 08:15:00', '{"argumentCount":1}' , '{"outputFieldCount":2}'),
        ('trace-demo-004', 'search_orders', 'workflow-orders', 'run-004', 'node-search', 'session-004', 'RUNNING', NULL, NULL, '2026-11-24 09:00:00', NULL, '{"argumentCount":1}' , '{"status":"RUNNING"}'),
        ('trace-demo-005', 'approve_refund', 'workflow-refund', 'run-005', 'node-approval', 'session-005', 'SUCCEEDED', NULL, 96, '2026-11-24 09:03:00', '2026-11-24 09:03:00', '{"argumentCount":4}' , '{"outputFieldCount":3}'),
        ('trace-demo-006', 'send_notification', 'workflow-notify', 'run-006', 'node-send', 'session-006', 'FAILED', 'DOWNSTREAM_ERROR', 184, '2026-11-24 09:08:00', '2026-11-24 09:08:00', '{"argumentCount":2}' , '{"status":"FAILED"}')
) AS seed(trace_id, tool_name, workflow_id, run_id, node_id, session_id, status, error_code, latency_ms, started_at, finished_at, request_summary, response_summary)
JOIN mcp_tool t ON t.tenant_id = 'tenant-a' AND t.tool_name = seed.tool_name AND t.deleted = 0
WHERE NOT EXISTS (
    SELECT 1 FROM mcp_invocation i
    WHERE i.tenant_id = 'tenant-a' AND i.trace_id = seed.trace_id AND i.deleted = 0
);
