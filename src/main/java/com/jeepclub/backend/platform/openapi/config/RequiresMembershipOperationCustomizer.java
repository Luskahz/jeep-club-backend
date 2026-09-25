package com.jeepclub.backend.platform.openapi.config;

import com.jeepclub.backend.memberships.api.security.RequiresMembership;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

@Component
public class RequiresMembershipOperationCustomizer implements OperationCustomizer {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";
    private static final String PROBLEM_MEDIA_TYPE = "application/problem+json";
    private static final String ERROR_SCHEMA = "#/components/schemas/ApiErrorResponse";

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        if (!requiresMembership(handlerMethod)) {
            return operation;
        }

        operation.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
        operation.addExtension("x-requires-membership", true);
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        addResponseIfAbsent(
                operation,
                "402",
                "Pagamento da membritude necessário (MEMBERSHIP_PAYMENT_REQUIRED)."
        );
        addResponseIfAbsent(
                operation,
                "503",
                "Cobrança de membritude indisponível (MEMBERSHIP_CHARGE_UNAVAILABLE)."
        );
        return operation;
    }

    private static boolean requiresMembership(HandlerMethod handlerMethod) {
        return AnnotatedElementUtils.hasAnnotation(
                handlerMethod.getMethod(),
                RequiresMembership.class
        ) || AnnotatedElementUtils.hasAnnotation(
                handlerMethod.getBeanType(),
                RequiresMembership.class
        );
    }

    private static void addResponseIfAbsent(
            Operation operation,
            String status,
            String description
    ) {
        if (operation.getResponses().containsKey(status)) {
            return;
        }

        Schema<?> schema = new Schema<>().$ref(ERROR_SCHEMA);
        Content content = new Content().addMediaType(
                PROBLEM_MEDIA_TYPE,
                new MediaType().schema(schema)
        );
        operation.getResponses().addApiResponse(
                status,
                new ApiResponse().description(description).content(content)
        );
    }
}
