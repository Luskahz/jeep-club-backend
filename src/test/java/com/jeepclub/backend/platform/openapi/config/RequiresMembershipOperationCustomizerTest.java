package com.jeepclub.backend.platform.openapi.config;

import com.jeepclub.backend.memberships.api.security.RequiresMembership;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;

class RequiresMembershipOperationCustomizerTest {

    private final RequiresMembershipOperationCustomizer customizer =
            new RequiresMembershipOperationCustomizer();

    @Test
    void shouldDocumentMembershipResponsesWithoutReplacingExistingOnes() throws Exception {
        Operation operation = new Operation().responses(
                new ApiResponses().addApiResponse("200", new ApiResponse().description("OK"))
        );

        customizer.customize(operation, handlerMethod("protectedEndpoint"));

        assertThat(operation.getResponses()).containsKeys("200", "402", "503");
        assertThat(operation.getResponses().get("402").getContent())
                .containsKey("application/problem+json");
        assertThat(operation.getResponses().get("402").getContent()
                .get("application/problem+json").getSchema().get$ref())
                .isEqualTo("#/components/schemas/ApiErrorResponse");
        assertThat(operation.getExtensions()).containsEntry("x-requires-membership", true);
    }

    @Test
    void shouldIgnoreUnannotatedEndpoints() throws Exception {
        Operation operation = new Operation().responses(new ApiResponses());

        customizer.customize(operation, handlerMethod("plainEndpoint"));

        assertThat(operation.getResponses()).isEmpty();
    }

    private static HandlerMethod handlerMethod(String methodName) throws Exception {
        TestController controller = new TestController();
        return new HandlerMethod(controller, TestController.class.getDeclaredMethod(methodName));
    }

    static class TestController {
        @RequiresMembership
        void protectedEndpoint() {}

        void plainEndpoint() {}
    }
}
