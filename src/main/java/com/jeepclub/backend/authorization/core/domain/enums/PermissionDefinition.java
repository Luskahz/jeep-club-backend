package com.jeepclub.backend.authorization.core.domain.enums;

import java.util.Arrays;

public enum PermissionDefinition {

    // AUTHENTICATION / USERS
    AUTH_USER_READ(
            PermissionCode.AUTHENTICATION_USER_READ,
            ModuleCode.AUTHENTICATION,
            "Permite consultar usuários"
    ),

    AUTH_USER_CREATE(
            PermissionCode.AUTHENTICATION_USER_CREATE,
            ModuleCode.AUTHENTICATION,
            "Permite criar usuários"
    ),

    AUTH_USER_UPDATE(
            PermissionCode.AUTHENTICATION_USER_UPDATE,
            ModuleCode.AUTHENTICATION,
            "Permite atualizar usuários"
    ),

    AUTH_USER_DISABLE(
            PermissionCode.AUTHENTICATION_USER_DISABLE,
            ModuleCode.AUTHENTICATION,
            "Permite desativar usuários"
    ),

    AUTH_USER_ENABLE(
            PermissionCode.AUTHENTICATION_USER_ENABLE,
            ModuleCode.AUTHENTICATION,
            "Permite reativar usuários"
    ),

    // AUTHORIZATION / ROLES
    AUTHZ_ROLE_READ(
            PermissionCode.AUTHORIZATION_ROLE_READ,
            ModuleCode.AUTHORIZATION,
            "Permite consultar papéis de acesso"
    ),

    AUTHZ_ROLE_CREATE(
            PermissionCode.AUTHORIZATION_ROLE_CREATE,
            ModuleCode.AUTHORIZATION,
            "Permite criar papéis de acesso"
    ),

    AUTHZ_ROLE_UPDATE(
            PermissionCode.AUTHORIZATION_ROLE_UPDATE,
            ModuleCode.AUTHORIZATION,
            "Permite atualizar papéis de acesso"
    ),

    AUTHZ_ROLE_DELETE(
            PermissionCode.AUTHORIZATION_ROLE_DELETE,
            ModuleCode.AUTHORIZATION,
            "Permite remover papéis de acesso"
    ),

    // AUTHORIZATION / PERMISSIONS
    AUTHZ_PERMISSION_READ(
            PermissionCode.AUTHORIZATION_PERMISSION_READ,
            ModuleCode.AUTHORIZATION,
            "Permite consultar permissões"
    ),

    AUTHZ_PERMISSION_ASSIGN(
            PermissionCode.AUTHORIZATION_PERMISSION_ASSIGN,
            ModuleCode.AUTHORIZATION,
            "Permite atribuir permissões a papéis"
    ),

    AUTHZ_PERMISSION_REVOKE(
            PermissionCode.AUTHORIZATION_PERMISSION_REVOKE,
            ModuleCode.AUTHORIZATION,
            "Permite revogar permissões de papéis"
    ),

    // AUTHORIZATION / USER ROLES
    AUTHZ_USER_ROLE_ASSIGN(
            PermissionCode.AUTHORIZATION_USER_ROLE_ASSIGN,
            ModuleCode.AUTHORIZATION,
            "Permite vincular papéis a usuários"
    ),

    AUTHZ_USER_ROLE_REVOKE(
            PermissionCode.AUTHORIZATION_USER_ROLE_REVOKE,
            ModuleCode.AUTHORIZATION,
            "Permite remover papéis de usuários"
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