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

    AUTHENTICATION_PASSWORD_RECOVERY_READ(
            PermissionCode.AUTHENTICATION_PASSWORD_RECOVERY_READ,
            ModuleCode.AUTHENTICATION,
            "Permite consultar solicitações de recuperação de senha"
    ),

    AUTHENTICATION_PASSWORD_RECOVERY_CANCEL(
            PermissionCode.AUTHENTICATION_PASSWORD_RECOVERY_CANCEL,
            ModuleCode.AUTHENTICATION,
            "Permite cancelar solicitações de recuperação de senha"
    ),

    AUTHENTICATION_REFRESH_TOKEN_READ(
            PermissionCode.AUTHENTICATION_REFRESH_TOKEN_READ,
            ModuleCode.AUTHENTICATION,
            "Permite consultar tokens de renovação"
    ),

    AUTHENTICATION_REFRESH_TOKEN_REVOKE(
            PermissionCode.AUTHENTICATION_REFRESH_TOKEN_REVOKE,
            ModuleCode.AUTHENTICATION,
            "Permite revogar tokens de renovação"
    ),

    AUTHENTICATION_SESSION_READ(
            PermissionCode.AUTHENTICATION_SESSION_READ,
            ModuleCode.AUTHENTICATION,
            "Permite consultar sessões de usuários"
    ),

    AUTHENTICATION_SESSION_LOGOUT(
            PermissionCode.AUTHENTICATION_SESSION_LOGOUT,
            ModuleCode.AUTHENTICATION,
            "Permite encerrar sessões de usuários"
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

    // BILLING / CHARGE ASSIGNMENTS
    BILLING_CHARGE_ASSIGNMENT_CREATE(
            PermissionCode.BILLING_CHARGE_ASSIGNMENT_CREATE,
            ModuleCode.BILLING,
            "Permite criar atribuições de cobrança"
    ),

    BILLING_CHARGE_ASSIGNMENT_READ(
            PermissionCode.BILLING_CHARGE_ASSIGNMENT_READ,
            ModuleCode.BILLING,
            "Permite consultar atribuições de cobrança"
    ),

    BILLING_CHARGE_ASSIGNMENT_UPDATE(
            PermissionCode.BILLING_CHARGE_ASSIGNMENT_UPDATE,
            ModuleCode.BILLING,
            "Permite atualizar atribuições de cobrança"
    ),

    // BILLING / CHARGE CYCLES
    BILLING_CHARGE_CYCLE_GENERATE(
            PermissionCode.BILLING_CHARGE_CYCLE_GENERATE,
            ModuleCode.BILLING,
            "Permite gerar ciclos de cobrança"
    ),

    BILLING_CHARGE_CYCLE_READ(
            PermissionCode.BILLING_CHARGE_CYCLE_READ,
            ModuleCode.BILLING,
            "Permite consultar ciclos de cobrança"
    ),

    BILLING_CHARGE_CYCLE_CANCEL(
            PermissionCode.BILLING_CHARGE_CYCLE_CANCEL,
            ModuleCode.BILLING,
            "Permite cancelar ciclos de cobrança"
    ),

    BILLING_CHARGE_CYCLE_FINISH(
            PermissionCode.BILLING_CHARGE_CYCLE_FINISH,
            ModuleCode.BILLING,
            "Permite finalizar ciclos de cobrança"
    ),

    BILLING_CHARGE_CYCLE_ARCHIVE(
            PermissionCode.BILLING_CHARGE_CYCLE_ARCHIVE,
            ModuleCode.BILLING,
            "Permite arquivar ciclos de cobrança"
    ),

    // BILLING / CHARGE DEFINITIONS
    BILLING_CHARGE_DEFINITION_CREATE(
            PermissionCode.BILLING_CHARGE_DEFINITION_CREATE,
            ModuleCode.BILLING,
            "Permite criar definições de cobrança"
    ),

    BILLING_CHARGE_DEFINITION_READ(
            PermissionCode.BILLING_CHARGE_DEFINITION_READ,
            ModuleCode.BILLING,
            "Permite consultar definições de cobrança"
    ),

    BILLING_CHARGE_DEFINITION_UPDATE(
            PermissionCode.BILLING_CHARGE_DEFINITION_UPDATE,
            ModuleCode.BILLING,
            "Permite atualizar definições de cobrança"
    ),

    // BILLING / MEMBER CHARGES
    BILLING_MEMBER_CHARGE_READ(
            PermissionCode.BILLING_MEMBER_CHARGE_READ,
            ModuleCode.BILLING,
            "Permite consultar cobranças de membros"
    ),

    BILLING_MEMBER_CHARGE_UPDATE(
            PermissionCode.BILLING_MEMBER_CHARGE_UPDATE,
            ModuleCode.BILLING,
            "Permite atualizar cobranças de membros"
    ),

    BILLING_MEMBER_CHARGE_CANCEL(
            PermissionCode.BILLING_MEMBER_CHARGE_CANCEL,
            ModuleCode.BILLING,
            "Permite cancelar cobranças de membros"
    ),

    // BILLING / PAYMENTS
    BILLING_PAYMENT_READ(
            PermissionCode.BILLING_PAYMENT_READ,
            ModuleCode.BILLING,
            "Permite consultar pagamentos"
    ),

    BILLING_PAYMENT_CONFIRM(
            PermissionCode.BILLING_PAYMENT_CONFIRM,
            ModuleCode.BILLING,
            "Permite confirmar pagamentos"
    ),

    BILLING_PAYMENT_REJECT(
            PermissionCode.BILLING_PAYMENT_REJECT,
            ModuleCode.BILLING,
            "Permite rejeitar pagamentos"
    ),

    // BILLING / REFUNDS
    BILLING_REFUND_READ(
            PermissionCode.BILLING_REFUND_READ,
            ModuleCode.BILLING,
            "Permite consultar reembolsos"
    ),

    BILLING_REFUND_APPROVE(
            PermissionCode.BILLING_REFUND_APPROVE,
            ModuleCode.BILLING,
            "Permite aprovar reembolsos"
    ),

    BILLING_REFUND_REJECT(
            PermissionCode.BILLING_REFUND_REJECT,
            ModuleCode.BILLING,
            "Permite rejeitar reembolsos"
    ),

    BILLING_REFUND_MARK_AS_REFUNDED(
            PermissionCode.BILLING_REFUND_MARK_AS_REFUNDED,
            ModuleCode.BILLING,
            "Permite marcar reembolsos como realizados"
    ),

    BILLING_REFUND_EXPIRE(
            PermissionCode.BILLING_REFUND_EXPIRE,
            ModuleCode.BILLING,
            "Permite expirar reembolsos"
    ),

    BILLING_REFUND_CANCEL(
            PermissionCode.BILLING_REFUND_CANCEL,
            ModuleCode.BILLING,
            "Permite cancelar reembolsos"
    ),

    // DEPENDENTS
    DEPENDENTS_DEPENDENT_READ(
            PermissionCode.DEPENDENTS_DEPENDENT_READ,
            ModuleCode.DEPENDENTS,
            "Permite consultar dependentes"
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

    HEALTH_MEDICAL_PROFILE_DELETE(
            PermissionCode.HEALTH_MEDICAL_PROFILE_DELETE,
            ModuleCode.HEALTH,
            "Permite excluir o perfil médico"
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
    ),

    // VEHICLES
    VEHICLES_VEHICLE_CREATE(
            PermissionCode.VEHICLES_VEHICLE_CREATE,
            ModuleCode.VEHICLES,
            "Permite cadastrar veículos"
    ),

    VEHICLES_VEHICLE_READ(
            PermissionCode.VEHICLES_VEHICLE_READ,
            ModuleCode.VEHICLES,
            "Permite consultar veículos"
    ),

    VEHICLES_VEHICLE_UPDATE(
            PermissionCode.VEHICLES_VEHICLE_UPDATE,
            ModuleCode.VEHICLES,
            "Permite atualizar veículos"
    ),

    VEHICLES_VEHICLE_DELETE(
            PermissionCode.VEHICLES_VEHICLE_DELETE,
            ModuleCode.VEHICLES,
            "Permite remover veículos"
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
