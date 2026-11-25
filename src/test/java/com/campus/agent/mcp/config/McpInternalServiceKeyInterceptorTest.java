package com.campus.agent.mcp.config;

import com.campus.agent.mcp.error.McpBusinessException;
import com.campus.agent.mcp.error.McpErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpInternalServiceKeyInterceptorTest {

    @Test
    void allowsRequestWithConfiguredKey() throws Exception {
        McpInternalServiceKeyInterceptor interceptor = new McpInternalServiceKeyInterceptor("configured-secret", true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(McpInternalServiceKeyInterceptor.HEADER_NAME, "configured-secret");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    void rejectsIncorrectKey() {
        McpInternalServiceKeyInterceptor interceptor = new McpInternalServiceKeyInterceptor("configured-secret", true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(McpInternalServiceKeyInterceptor.HEADER_NAME, "wrong-secret");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(McpBusinessException.class)
                .extracting(exception -> ((McpBusinessException) exception).getErrorCode())
                .isEqualTo(McpErrorCode.INTERNAL_SERVICE_AUTH_FAILED);
    }

    @Test
    void rejectsMissingKey() {
        McpInternalServiceKeyInterceptor interceptor = new McpInternalServiceKeyInterceptor("configured-secret", true);
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(McpBusinessException.class)
                .extracting(exception -> ((McpBusinessException) exception).getErrorCode())
                .isEqualTo(McpErrorCode.INTERNAL_SERVICE_AUTH_FAILED);
    }

    @Test
    void failsClosedWhenNoSecretIsConfigured() {
        McpInternalServiceKeyInterceptor interceptor = new McpInternalServiceKeyInterceptor("", true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(McpInternalServiceKeyInterceptor.HEADER_NAME, "configured-secret");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(McpBusinessException.class);
    }

    @Test
    void allowsRequestsWhenAuthenticationIsExplicitlyDisabled() throws Exception {
        McpInternalServiceKeyInterceptor interceptor = new McpInternalServiceKeyInterceptor("", false);

        assertThat(interceptor.preHandle(
                new MockHttpServletRequest(), new MockHttpServletResponse(), new Object())).isTrue();
    }
}