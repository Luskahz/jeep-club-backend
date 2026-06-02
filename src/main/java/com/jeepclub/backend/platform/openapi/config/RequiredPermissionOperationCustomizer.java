package com.jeepclub.backend.platform.openapi.config;

import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.List;

@Component
public class RequiredPermissionOperationCustomizer implements OperationCustomizer {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        RequiredPermission requiredPermission = findRequiredPermission(handlerMethod);

        if (requiredPermission == null) {
            return operation;
        }

        List<String> permissions = Arrays.asList(requiredPermission.value());

        operation.addSecurityItem(
                new SecurityRequirement().addList(SECURITY_SCHEME_NAME)
        );

        operation.addExtension("x-required-permissions", permissions);

        String permissionText = permissions.size() == 1
                ? """
        
        ---
        ### Permission
        
        `%s`
        """
                .formatted(permissions.get(0))
                : """
        
        ---
        ### Permissions
        
        %s
        """
                .formatted(
                        permissions.stream()
                                .map(permission -> "- `" + permission + "`")
                                .reduce((first, second) -> first + "\n" + second)
                                .orElse("")
                );

        String currentDescription = operation.getDescription();

        operation.setDescription(
                currentDescription == null || currentDescription.isBlank()
                        ? permissionText
                        : currentDescription + "\n\n" + permissionText
        );

        return operation;
    }

    private RequiredPermission findRequiredPermission(HandlerMethod handlerMethod) {
        RequiredPermission methodAnnotation = handlerMethod.getMethodAnnotation(RequiredPermission.class);

        if (methodAnnotation != null) {
            return methodAnnotation;
        }

        return handlerMethod.getBeanType().getAnnotation(RequiredPermission.class);
    }
}