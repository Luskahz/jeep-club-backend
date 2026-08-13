package com.jeepclub.backend.shared.authorization;

import java.util.Arrays;

public enum PermissionDefinition {

    // AUTHENTICATION / USERS
    AUTHENTICATION_USER_READ(
            PermissionCode.AUTHENTICATION_USER_READ,
            ModuleCode.AUTHENTICATION,
            "Permite consultar usuários"
    ),

    AUTHENTICATION_USER_CREATE(
            PermissionCode.AUTHENTICATION_USER_CREATE,
            ModuleCode.AUTHENTICATION,
            "Permite criar usuários"
    ),

    AUTHENTICATION_USER_UPDATE(
            PermissionCode.AUTHENTICATION_USER_UPDATE,
            ModuleCode.AUTHENTICATION,
            "Permite atualizar usuários"
    ),

    AUTHENTICATION_USER_DISABLE(
            PermissionCode.AUTHENTICATION_USER_DISABLE,
            ModuleCode.AUTHENTICATION,
            "Permite desativar usuários"
    ),

    AUTHENTICATION_USER_ENABLE(
            PermissionCode.AUTHENTICATION_USER_ENABLE,
            ModuleCode.AUTHENTICATION,
            "Permite reativar usuários"
    ),

    // AUTHORIZATION / ROLES
    AUTHORIZATION_ROLE_READ(
            PermissionCode.AUTHORIZATION_ROLE_READ,
            ModuleCode.AUTHORIZATION,
            "Permite consultar papéis de acesso"
    ),

    AUTHORIZATION_ROLE_CREATE(
            PermissionCode.AUTHORIZATION_ROLE_CREATE,
            ModuleCode.AUTHORIZATION,
            "Permite criar papéis de acesso"
    ),

    AUTHORIZATION_ROLE_UPDATE(
            PermissionCode.AUTHORIZATION_ROLE_UPDATE,
            ModuleCode.AUTHORIZATION,
            "Permite atualizar papéis de acesso"
    ),

    AUTHORIZATION_ROLE_DELETE(
            PermissionCode.AUTHORIZATION_ROLE_DELETE,
            ModuleCode.AUTHORIZATION,
            "Permite remover papéis de acesso"
    ),

    AUTHORIZATION_ROLE_DISABLE(
            PermissionCode.AUTHORIZATION_ROLE_DISABLE,
            ModuleCode.AUTHORIZATION,
            "Permite desativar papéis de acesso"
    ),

    AUTHORIZATION_ROLE_ENABLE(
            PermissionCode.AUTHORIZATION_ROLE_ENABLE,
            ModuleCode.AUTHORIZATION,
            "Permite reativar papéis de acesso"
    ),

    // AUTHENTICATION / PASSWORD RECOVERY

    AUTHENTICATION_USER_PASSWORD_RESET_LINK_GENERATE(
            PermissionCode.AUTHENTICATION_USER_PASSWORD_RESET_LINK_GENERATE,
            ModuleCode.AUTHENTICATION,
            "Permite gerar links administrativos de redefinição de senha para usuários"
    ),

    AUTHENTICATION_USER_TEMPORARY_PASSWORD_GENERATE(
            PermissionCode.AUTHENTICATION_USER_TEMPORARY_PASSWORD_GENERATE,
            ModuleCode.AUTHENTICATION,
            "Permite gerar senhas provisórias para usuários"
    ),

    // AUTHORIZATION / PERMISSIONS
    AUTHORIZATION_PERMISSION_READ(
            PermissionCode.AUTHORIZATION_PERMISSION_READ,
            ModuleCode.AUTHORIZATION,
            "Permite consultar permissões"
    ),

    AUTHORIZATION_PERMISSION_ASSIGN(
            PermissionCode.AUTHORIZATION_PERMISSION_ASSIGN,
            ModuleCode.AUTHORIZATION,
            "Permite atribuir permissões a papéis"
    ),

    AUTHORIZATION_PERMISSION_REVOKE(
            PermissionCode.AUTHORIZATION_PERMISSION_REVOKE,
            ModuleCode.AUTHORIZATION,
            "Permite revogar permissões de papéis"
    ),

    // AUTHORIZATION / USER ROLES
    AUTHORIZATION_USER_ROLE_READ(
            PermissionCode.AUTHORIZATION_USER_ROLE_READ,
            ModuleCode.AUTHORIZATION,
            "Permite consultar papéis vinculados a usuários"
    ),

    AUTHORIZATION_USER_ROLE_ASSIGN(
            PermissionCode.AUTHORIZATION_USER_ROLE_ASSIGN,
            ModuleCode.AUTHORIZATION,
            "Permite vincular papéis a usuários"
    ),

    AUTHORIZATION_USER_ROLE_REVOKE(
            PermissionCode.AUTHORIZATION_USER_ROLE_REVOKE,
            ModuleCode.AUTHORIZATION,
            "Permite remover papéis de usuários"
    ),

    HEALTH_MEDICAL_PROFILE_UPDATE(
            PermissionCode.HEALTH_MEDICAL_PROFILE_UPDATE,
            ModuleCode.HEALTH,
            "Permite atualizar o perfil médico"
    ),

    HEALTH_MEDICAL_PROFILE_READ(
            PermissionCode.HEALTH_MEDICAL_PROFILE_READ,
            ModuleCode.HEALTH,
            "Permite consultar o perfil médico"
    ),

    // MEMBERSHIP / MEMBERSHIP REQUEST
    MEMBERSHIP_MEMBERSHIP_REQUEST_READ(
            PermissionCode.MEMBERSHIP_MEMBERSHIP_REQUEST_READ,
            ModuleCode.MEMBERSHIP,
            "Permite consultar solicitações de adesão"
    ),

    MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE(
            PermissionCode.MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE,
            ModuleCode.MEMBERSHIP,
            "Permite aprovar solicitações de adesão"
    ),

    MEMBERSHIP_MEMBERSHIP_REQUEST_REJECT(
            PermissionCode.MEMBERSHIP_MEMBERSHIP_REQUEST_REJECT,
            ModuleCode.MEMBERSHIP,
            "Permite rejeitar solicitações de adesão"
    ),

    MEMBERSHIP_MEMBERSHIP_APPLICANT_BLOCK(
            PermissionCode.MEMBERSHIP_MEMBERSHIP_APPLICANT_BLOCK,
            ModuleCode.MEMBERSHIP,
            "Permite rejeitar solicitações e bloquear solicitantes"
    ),

    MEMBERSHIP_MEMBERSHIP_APPLICANT_UNBLOCK(
            PermissionCode.MEMBERSHIP_MEMBERSHIP_APPLICANT_UNBLOCK,
            ModuleCode.MEMBERSHIP,
            "Permite desbloquear solicitantes"
    ),

    MEMBERSHIP_MEMBERSHIP_REQUEST_INVITE_RESEND(
            PermissionCode.MEMBERSHIP_MEMBERSHIP_REQUEST_INVITE_RESEND,
            ModuleCode.MEMBERSHIP,
            "Permite reenviar o convite de ativação para um solicitante aprovado"
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
