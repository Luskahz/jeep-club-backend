package com.jeepclub.backend.shared.authorization;

import java.util.Arrays;

public enum PermissionDefinition {
    IDENTITY_USER_EXPORT(PermissionCode.IDENTITY_USER_EXPORT, ModuleCode.IDENTITY, "Permite exportação administrativa de IDENTITY"),
    AUTHENTICATION_EXPORT(PermissionCode.AUTHENTICATION_EXPORT, ModuleCode.AUTHENTICATION, "Permite exportação administrativa de AUTHENTICATION"),
    AUTHORIZATION_EXPORT(PermissionCode.AUTHORIZATION_EXPORT, ModuleCode.AUTHORIZATION, "Permite exportação administrativa de AUTHORIZATION"),
    MEMBERSHIP_EXPORT(PermissionCode.MEMBERSHIP_EXPORT, ModuleCode.MEMBERSHIP, "Permite exportação administrativa de MEMBERSHIP"),
    DEPENDENTS_DEPENDENT_EXPORT(PermissionCode.DEPENDENTS_DEPENDENT_EXPORT, ModuleCode.DEPENDENTS, "Permite exportação administrativa de DEPENDENTS"),
    VEHICLES_VEHICLE_EXPORT(PermissionCode.VEHICLES_VEHICLE_EXPORT, ModuleCode.VEHICLES, "Permite exportação administrativa de VEHICLES"),
    TOOLS_TOOL_EXPORT(PermissionCode.TOOLS_TOOL_EXPORT, ModuleCode.TOOLS, "Permite exportação administrativa de TOOLS"),
    HEALTH_MEDICAL_PROFILE_EXPORT(PermissionCode.HEALTH_MEDICAL_PROFILE_EXPORT, ModuleCode.HEALTH, "Permite exportação administrativa de HEALTH"),
    BILLING_EXPORT(PermissionCode.BILLING_EXPORT, ModuleCode.BILLING, "Permite exportação administrativa de BILLING"),
    PUBLICATIONS_EXPORT(PermissionCode.PUBLICATIONS_EXPORT, ModuleCode.PUBLICATIONS, "Permite exportação administrativa de PUBLICATIONS"),

    PUBLICATIONS_INTERACTION_LIKE(PermissionCode.PUBLICATIONS_INTERACTION_LIKE, ModuleCode.PUBLICATIONS, "Publication: like and unlike"),
    PUBLICATIONS_COMMENT_CREATE(PermissionCode.PUBLICATIONS_COMMENT_CREATE, ModuleCode.PUBLICATIONS, "Publication: create comment"),
    PUBLICATIONS_COMMENT_READ(PermissionCode.PUBLICATIONS_COMMENT_READ, ModuleCode.PUBLICATIONS, "Publication: read comments"),
    PUBLICATIONS_FEED_READ(PermissionCode.PUBLICATIONS_FEED_READ, ModuleCode.PUBLICATIONS, "Publication: read feed"),
    PUBLICATIONS_PUBLICATION_READ(PermissionCode.PUBLICATIONS_PUBLICATION_READ, ModuleCode.PUBLICATIONS, "Publication: read detail"),
    PUBLICATIONS_EVENT_CREATE(PermissionCode.PUBLICATIONS_EVENT_CREATE, ModuleCode.PUBLICATIONS, "Event: create"),
    PUBLICATIONS_EVENT_READ(PermissionCode.PUBLICATIONS_EVENT_READ, ModuleCode.PUBLICATIONS, "Event: read"),
    PUBLICATIONS_EVENT_READ_ADMIN(PermissionCode.PUBLICATIONS_EVENT_READ_ADMIN, ModuleCode.PUBLICATIONS, "Event: read admin"),
    PUBLICATIONS_EVENT_UPDATE(PermissionCode.PUBLICATIONS_EVENT_UPDATE, ModuleCode.PUBLICATIONS, "Event: update"),
    PUBLICATIONS_EVENT_PUBLISH(PermissionCode.PUBLICATIONS_EVENT_PUBLISH, ModuleCode.PUBLICATIONS, "Event: publish"),
    PUBLICATIONS_EVENT_CANCEL(PermissionCode.PUBLICATIONS_EVENT_CANCEL, ModuleCode.PUBLICATIONS, "Event: cancel"),
    PUBLICATIONS_EVENT_FINISH(PermissionCode.PUBLICATIONS_EVENT_FINISH, ModuleCode.PUBLICATIONS, "Event: finish"),
    PUBLICATIONS_EVENT_DELETE(PermissionCode.PUBLICATIONS_EVENT_DELETE, ModuleCode.PUBLICATIONS, "Event: delete"),
    PUBLICATIONS_EVENT_REGISTER(PermissionCode.PUBLICATIONS_EVENT_REGISTER, ModuleCode.PUBLICATIONS, "Event: register"),
    PUBLICATIONS_EVENT_REGISTRATION_READ(PermissionCode.PUBLICATIONS_EVENT_REGISTRATION_READ, ModuleCode.PUBLICATIONS, "Event: registration read"),
    PUBLICATIONS_EVENT_REGISTRATION_CANCEL(PermissionCode.PUBLICATIONS_EVENT_REGISTRATION_CANCEL, ModuleCode.PUBLICATIONS, "Event: registration cancel"),
    PUBLICATIONS_EVENT_GUEST_REQUEST_CREATE(PermissionCode.PUBLICATIONS_EVENT_GUEST_REQUEST_CREATE, ModuleCode.PUBLICATIONS, "Event: guest request create"),
    PUBLICATIONS_EVENT_GUEST_REQUEST_READ(PermissionCode.PUBLICATIONS_EVENT_GUEST_REQUEST_READ, ModuleCode.PUBLICATIONS, "Event: guest request read"),
    PUBLICATIONS_EVENT_GUEST_ADMIN_CREATE(PermissionCode.PUBLICATIONS_EVENT_GUEST_ADMIN_CREATE, ModuleCode.PUBLICATIONS, "Event: guest admin create"),
    PUBLICATIONS_EVENT_GUEST_ADMIN_READ(PermissionCode.PUBLICATIONS_EVENT_GUEST_ADMIN_READ, ModuleCode.PUBLICATIONS, "Event: guest admin read"),
    PUBLICATIONS_EVENT_GUEST_APPROVE(PermissionCode.PUBLICATIONS_EVENT_GUEST_APPROVE, ModuleCode.PUBLICATIONS, "Event: guest approve"),
    PUBLICATIONS_EVENT_GUEST_REJECT(PermissionCode.PUBLICATIONS_EVENT_GUEST_REJECT, ModuleCode.PUBLICATIONS, "Event: guest reject"),
    PUBLICATIONS_EVENT_RIDE_OFFER_READ(PermissionCode.PUBLICATIONS_EVENT_RIDE_OFFER_READ, ModuleCode.PUBLICATIONS, "Event: ride offer read"),
    PUBLICATIONS_EVENT_RIDE_OFFER_RESPOND(PermissionCode.PUBLICATIONS_EVENT_RIDE_OFFER_RESPOND, ModuleCode.PUBLICATIONS, "Event: ride offer respond"),
    PUBLICATIONS_EVENT_RIDE_OFFER_SELECT(PermissionCode.PUBLICATIONS_EVENT_RIDE_OFFER_SELECT, ModuleCode.PUBLICATIONS, "Event: ride offer select"),
    PUBLICATIONS_EVENT_ADMIN_DASHBOARD_READ(PermissionCode.PUBLICATIONS_EVENT_ADMIN_DASHBOARD_READ, ModuleCode.PUBLICATIONS, "Event: admin dashboard read"),
    PUBLICATIONS_EVENT_HEALTH_EMERGENCY_READ(PermissionCode.PUBLICATIONS_EVENT_HEALTH_EMERGENCY_READ, ModuleCode.PUBLICATIONS, "Event: health emergency read"),

    // IDENTITY / USERS
    IDENTITY_USER_READ(
            PermissionCode.IDENTITY_USER_READ,
            ModuleCode.IDENTITY,
            "Permite consultar usuários"
    ),

    IDENTITY_USER_CREATE(
            PermissionCode.IDENTITY_USER_CREATE,
            ModuleCode.IDENTITY,
            "Permite criar usuários"
    ),

    IDENTITY_USER_UPDATE(
            PermissionCode.IDENTITY_USER_UPDATE,
            ModuleCode.IDENTITY,
            "Permite atualizar usuários"
    ),

    IDENTITY_USER_DISABLE(
            PermissionCode.IDENTITY_USER_DISABLE,
            ModuleCode.IDENTITY,
            "Permite desativar usuários"
    ),

    IDENTITY_USER_ENABLE(
            PermissionCode.IDENTITY_USER_ENABLE,
            ModuleCode.IDENTITY,
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

    MEMBERSHIP_BILLING_CONFIGURATION_READ(
            PermissionCode.MEMBERSHIP_BILLING_CONFIGURATION_READ,
            ModuleCode.MEMBERSHIP,
            "Permite consultar a configuração financeira da membritude"
    ),

    MEMBERSHIP_BILLING_CONFIGURATION_UPDATE(
            PermissionCode.MEMBERSHIP_BILLING_CONFIGURATION_UPDATE,
            ModuleCode.MEMBERSHIP,
            "Permite configurar e habilitar a exigência financeira da membritude"
    ),

    // TOOLS
        TOOLS_TOOL_CREATE(
        PermissionCode.TOOLS_TOOL_CREATE,
        ModuleCode.TOOLS,
        "Permite cadastrar ferramentas em nome de um usuário"
        ),

        TOOLS_TOOL_READ(
        PermissionCode.TOOLS_TOOL_READ,
        ModuleCode.TOOLS,
        "Permite consultar ferramentas de qualquer usuário"
        ),

        TOOLS_TOOL_UPDATE(
        PermissionCode.TOOLS_TOOL_UPDATE,
        ModuleCode.TOOLS,
        "Permite atualizar ferramentas de qualquer usuário"
        ),

        TOOLS_TOOL_DELETE(
        PermissionCode.TOOLS_TOOL_DELETE,
        ModuleCode.TOOLS,
        "Permite excluir ferramentas de qualquer usuário"
        ),

        TOOLS_TOOL_ACTIVATE(
        PermissionCode.TOOLS_TOOL_ACTIVATE,
        ModuleCode.TOOLS,
        "Permite ativar ferramentas de qualquer usuário"
        ),

        TOOLS_TOOL_DEACTIVATE(
        PermissionCode.TOOLS_TOOL_DEACTIVATE,
        ModuleCode.TOOLS,
        "Permite desativar ferramentas de qualquer usuário"
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
    ),

    // PUBLICATIONS / NOTICE
    PUBLICATIONS_NOTICE_CREATE(
            PermissionCode.PUBLICATIONS_NOTICE_CREATE,
            ModuleCode.PUBLICATIONS,
            "Permite criar avisos"
    ),
    PUBLICATIONS_NOTICE_READ(
            PermissionCode.PUBLICATIONS_NOTICE_READ,
            ModuleCode.PUBLICATIONS,
            "Permite consultar avisos em qualquer estado editorial"
    ),
    PUBLICATIONS_NOTICE_UPDATE(
            PermissionCode.PUBLICATIONS_NOTICE_UPDATE,
            ModuleCode.PUBLICATIONS,
            "Permite editar avisos"
    ),
    PUBLICATIONS_NOTICE_PUBLISH(
            PermissionCode.PUBLICATIONS_NOTICE_PUBLISH,
            ModuleCode.PUBLICATIONS,
            "Permite publicar avisos"
    ),
    PUBLICATIONS_NOTICE_ARCHIVE(
            PermissionCode.PUBLICATIONS_NOTICE_ARCHIVE,
            ModuleCode.PUBLICATIONS,
            "Permite arquivar avisos"
    ),
    PUBLICATIONS_NOTICE_DELETE(
            PermissionCode.PUBLICATIONS_NOTICE_DELETE,
            ModuleCode.PUBLICATIONS,
            "Permite excluir avisos com snapshot histórico"
    ),
    PUBLICATIONS_SERVICE_REQUEST_CREATE(PermissionCode.PUBLICATIONS_SERVICE_REQUEST_CREATE, ModuleCode.PUBLICATIONS, "Permite solicitar publicação de serviço"),
    PUBLICATIONS_SERVICE_REQUEST_READ(PermissionCode.PUBLICATIONS_SERVICE_REQUEST_READ, ModuleCode.PUBLICATIONS, "Permite consultar solicitações próprias de serviço"),
    PUBLICATIONS_SERVICE_REQUEST_ADMIN_READ(PermissionCode.PUBLICATIONS_SERVICE_REQUEST_ADMIN_READ, ModuleCode.PUBLICATIONS, "Permite consultar solicitações de serviço administrativamente"),
    PUBLICATIONS_SERVICE_REQUEST_APPROVE(PermissionCode.PUBLICATIONS_SERVICE_REQUEST_APPROVE, ModuleCode.PUBLICATIONS, "Permite aprovar solicitação inicial de serviço"),
    PUBLICATIONS_SERVICE_REQUEST_REJECT(PermissionCode.PUBLICATIONS_SERVICE_REQUEST_REJECT, ModuleCode.PUBLICATIONS, "Permite rejeitar solicitação inicial de serviço"),
    PUBLICATIONS_SERVICE_CHANGE_REQUEST_CREATE(PermissionCode.PUBLICATIONS_SERVICE_CHANGE_REQUEST_CREATE, ModuleCode.PUBLICATIONS, "Permite solicitar alteração de serviço próprio"),
    PUBLICATIONS_SERVICE_CHANGE_REQUEST_READ(PermissionCode.PUBLICATIONS_SERVICE_CHANGE_REQUEST_READ, ModuleCode.PUBLICATIONS, "Permite consultar alteração própria de serviço"),
    PUBLICATIONS_SERVICE_CHANGE_REQUEST_ADMIN_READ(PermissionCode.PUBLICATIONS_SERVICE_CHANGE_REQUEST_ADMIN_READ, ModuleCode.PUBLICATIONS, "Permite consultar alterações de serviço administrativamente"),
    PUBLICATIONS_SERVICE_CHANGE_REQUEST_APPROVE(PermissionCode.PUBLICATIONS_SERVICE_CHANGE_REQUEST_APPROVE, ModuleCode.PUBLICATIONS, "Permite aprovar alteração de serviço"),
    PUBLICATIONS_SERVICE_CHANGE_REQUEST_REJECT(PermissionCode.PUBLICATIONS_SERVICE_CHANGE_REQUEST_REJECT, ModuleCode.PUBLICATIONS, "Permite rejeitar alteração de serviço"),
    PUBLICATIONS_SERVICE_READ(PermissionCode.PUBLICATIONS_SERVICE_READ, ModuleCode.PUBLICATIONS, "Permite consultar serviço publicado"),
    PUBLICATIONS_SERVICE_DELETE(PermissionCode.PUBLICATIONS_SERVICE_DELETE, ModuleCode.PUBLICATIONS, "Permite excluir serviço próprio"),
    PUBLICATIONS_SERVICE_ADMIN_READ(PermissionCode.PUBLICATIONS_SERVICE_ADMIN_READ, ModuleCode.PUBLICATIONS, "Permite consultar serviços administrativamente"),
    PUBLICATIONS_SERVICE_ADMIN_DELETE(PermissionCode.PUBLICATIONS_SERVICE_ADMIN_DELETE, ModuleCode.PUBLICATIONS, "Permite excluir qualquer serviço com histórico");

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
