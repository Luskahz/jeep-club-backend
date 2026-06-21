package com.jeepclub.backend.platform.openapi.config;

import com.jeepclub.backend.platform.openapi.group.SwaggerOperationGroup;
import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

@Component
public class SwaggerOperationGroupCustomizer implements OperationCustomizer {

    private static final String GROUP_NAME_EXTENSION = "x-operation-group";
    private static final String GROUP_ORDER_EXTENSION = "x-operation-group-order";

    @Override
    public Operation customize(
            Operation operation,
            HandlerMethod handlerMethod
    ) {
        SwaggerOperationGroup group = handlerMethod.getMethodAnnotation(SwaggerOperationGroup.class);

        if (group == null) {
            return operation;
        }

        operation.addExtension(GROUP_NAME_EXTENSION, group.value());
        operation.addExtension(GROUP_ORDER_EXTENSION, group.order());

        return operation;
    }
}