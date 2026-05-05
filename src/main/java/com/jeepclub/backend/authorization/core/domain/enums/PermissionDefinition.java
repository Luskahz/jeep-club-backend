package com.jeepclub.backend.authorization.core.domain.enums;

import java.util.Arrays;

public enum PermissionDefinition {

    // AUTHENTICATION / USERS
    AUTH_USER_READ(
            PermissionCode.AUTH_USER_READ,
            ModuleCode.AUTHENTICATION,
            "Permite consultar usuários"
    ),

    AUTH_USER_CREATE(
            PermissionCode.AUTH_USER_CREATE,
            ModuleCode.AUTHENTICATION,
            "Permite criar usuários"
    ),

    AUTH_USER_UPDATE(
            PermissionCode.AUTH_USER_UPDATE,
            ModuleCode.AUTHENTICATION,
            "Permite atualizar usuários"
    ),

    AUTH_USER_DISABLE(
            PermissionCode.AUTH_USER_DISABLE,
            ModuleCode.AUTHENTICATION,
            "Permite desativar usuários"
    ),

    AUTH_USER_ENABLE(
            PermissionCode.AUTH_USER_ENABLE,
            ModuleCode.AUTHENTICATION,
            "Permite reativar usuários"
    ),

    // AUTHENTICATION / EMPLOYEES
    AUTH_EMPLOYEE_NAME_READ(
            PermissionCode.AUTH_EMPLOYEE_NAME_READ,
            ModuleCode.AUTHENTICATION,
            "Permite consultar o nome de um colaborador pela matrícula"
    ),

    // AUTHORIZATION / ROLES
    AUTHZ_ROLE_READ(
            PermissionCode.AUTHZ_ROLE_READ,
            ModuleCode.AUTHORIZATION,
            "Permite consultar papéis de acesso"
    ),

    AUTHZ_ROLE_CREATE(
            PermissionCode.AUTHZ_ROLE_CREATE,
            ModuleCode.AUTHORIZATION,
            "Permite criar papéis de acesso"
    ),

    AUTHZ_ROLE_UPDATE(
            PermissionCode.AUTHZ_ROLE_UPDATE,
            ModuleCode.AUTHORIZATION,
            "Permite atualizar papéis de acesso"
    ),

    AUTHZ_ROLE_DELETE(
            PermissionCode.AUTHZ_ROLE_DELETE,
            ModuleCode.AUTHORIZATION,
            "Permite remover papéis de acesso"
    ),

    // AUTHORIZATION / PERMISSIONS
    AUTHZ_PERMISSION_READ(
            PermissionCode.AUTHZ_PERMISSION_READ,
            ModuleCode.AUTHORIZATION,
            "Permite consultar permissões"
    ),

    AUTHZ_PERMISSION_ASSIGN(
            PermissionCode.AUTHZ_PERMISSION_ASSIGN,
            ModuleCode.AUTHORIZATION,
            "Permite atribuir permissões a papéis"
    ),

    AUTHZ_PERMISSION_REVOKE(
            PermissionCode.AUTHZ_PERMISSION_REVOKE,
            ModuleCode.AUTHORIZATION,
            "Permite revogar permissões de papéis"
    ),

    // AUTHORIZATION / USER ROLES
    AUTHZ_USER_ROLE_ASSIGN(
            PermissionCode.AUTHZ_USER_ROLE_ASSIGN,
            ModuleCode.AUTHORIZATION,
            "Permite vincular papéis a usuários"
    ),

    AUTHZ_USER_ROLE_REVOKE(
            PermissionCode.AUTHZ_USER_ROLE_REVOKE,
            ModuleCode.AUTHORIZATION,
            "Permite remover papéis de usuários"
    ),

    // GATEWAY ADMIN / SERVICES
    GATEWAY_SERVICE_RUNTIME_READ(
            PermissionCode.GATEWAY_SERVICE_RUNTIME_READ,
            ModuleCode.GATEWAY_ADMIN,
            "Permite consultar o runtime dos serviços"
    ),

    GATEWAY_SERVICE_START(
            PermissionCode.GATEWAY_SERVICE_START,
            ModuleCode.GATEWAY_ADMIN,
            "Permite iniciar um serviço"
    ),

    GATEWAY_SERVICE_STOP(
            PermissionCode.GATEWAY_SERVICE_STOP,
            ModuleCode.GATEWAY_ADMIN,
            "Permite parar um serviço"
    ),

    GATEWAY_SERVICE_RESTART(
            PermissionCode.GATEWAY_SERVICE_RESTART,
            ModuleCode.GATEWAY_ADMIN,
            "Permite reiniciar um serviço"
    ),

    GATEWAY_SERVICE_START_ALL(
            PermissionCode.GATEWAY_SERVICE_START_ALL,
            ModuleCode.GATEWAY_ADMIN,
            "Permite iniciar todos os serviços"
    ),

    GATEWAY_SERVICE_STOP_ALL(
            PermissionCode.GATEWAY_SERVICE_STOP_ALL,
            ModuleCode.GATEWAY_ADMIN,
            "Permite parar todos os serviços"
    ),

    GATEWAY_SERVICE_REFRESH(
            PermissionCode.GATEWAY_SERVICE_REFRESH,
            ModuleCode.GATEWAY_ADMIN,
            "Permite atualizar informações dos serviços"
    );

    private final PermissionCode code;
    private final ModuleCode module;
    private final String description;

    PermissionDefinition(
            PermissionCode code,
            ModuleCode module,
            String description
    ) {
        this.code = code;
        this.module = module;
        this.description = description;
    }

    public PermissionCode getCode() {
        return code;
    }

    public ModuleCode getModule() {
        return module;
    }

    public String getDescription() {
        return description;
    }

    public static PermissionDefinition from(PermissionCode code) {
        return Arrays.stream(values())
                .filter(definition -> definition.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "PermissionCode sem definição: " + code
                ));
    }
}