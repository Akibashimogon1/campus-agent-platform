package com.campus.agent.mcp.controller;

import com.campus.agent.mcp.api.McpToolCallApiRequest;
import com.campus.agent.mcp.dto.McpCallResponse;
import com.campus.agent.mcp.dto.McpToolCallRequest;
import com.campus.agent.mcp.service.McpInvocationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;

class McpInternalToolCallControllerTest {

    @Test
    void injectsTenantContextAndPreservesEstablishedCallContract() {
        RecordingInvocationService invocationService = new RecordingInvocationService();
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("mcpInvocationService", invocationService);
        ObjectProvider<McpInvocationService> serviceProvider = beanFactory.getBeanProvider(McpInvocationService.class);
        McpCallResponse callResponse = new McpCallResponse();
        callResponse.setStatus("SUCCEEDED");
        invocationService.response = callResponse;
        McpInternalToolCallController controller = new McpInternalToolCallController(serviceProvider);
        McpToolCallApiRequest apiRequest = new McpToolCallApiRequest();
        apiRequest.setWorkflowId("workflow-a");
        apiRequest.setTraceId("trace-a");
        apiRequest.setToolName("search");

        McpCallResponse response = controller.callTool("internal-secret", apiRequest);

        assertThat(response).isSameAs(callResponse);
        assertThat(invocationService.request.getWorkflowId()).isEqualTo("workflow-a");
        assertThat(invocationService.request.getTraceId()).isEqualTo("trace-a");
        assertThat(invocationService.request.getToolName()).isEqualTo("search");
    }

    private static final class RecordingInvocationService implements McpInvocationService {

        private McpToolCallApiRequest request;
        private McpCallResponse response;

        @Override
        public McpCallResponse callTool(McpToolCallApiRequest request) {
            this.request = request;
            return response;
        }
    }
}