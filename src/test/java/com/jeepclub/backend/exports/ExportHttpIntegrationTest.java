package com.jeepclub.backend.exports;
import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.time.Instant;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class ExportHttpIntegrationTest {
    @Autowired MockMvc mvc;
    @MockitoBean JwtTokenParser parser;
    @MockitoBean UserAuthoritiesProvider authorities;
    @MockitoBean AccessTokenAuthenticationService authentication;
    static Stream<Arguments> routes(){return Stream.of(
Arguments.of("/admin/notices/export","PUBLICATIONS_EXPORT"),
Arguments.of("/admin/notices/history/export","PUBLICATIONS_EXPORT"),
Arguments.of("/admin/services/export","PUBLICATIONS_EXPORT"),
Arguments.of("/admin/services/history/export","PUBLICATIONS_EXPORT"),
Arguments.of("/admin/events/export","PUBLICATIONS_EXPORT"),
Arguments.of("/admin/events/history/export","PUBLICATIONS_EXPORT"),
Arguments.of("/admin/service-publication-requests/export","PUBLICATIONS_EXPORT"),
Arguments.of("/admin/service-publication-change-requests/export","PUBLICATIONS_EXPORT"),
Arguments.of("/admin/membership-applications/blocks/export","MEMBERSHIP_EXPORT"),
Arguments.of("/admin/membership-applications/export","MEMBERSHIP_EXPORT"),
Arguments.of("/authentication/admin/sessions/export","AUTHENTICATION_EXPORT"),
Arguments.of("/authentication/admin/refresh-tokens/export","AUTHENTICATION_EXPORT"),
Arguments.of("/authentication/admin/accounts/export","AUTHENTICATION_EXPORT"),
Arguments.of("/authentication/admin/password-recovery-requests/export","AUTHENTICATION_EXPORT"),
Arguments.of("/authorization/admin/users/export","AUTHORIZATION_EXPORT"),
Arguments.of("/authorization/admin/permissions/export","AUTHORIZATION_EXPORT"),
Arguments.of("/authorization/admin/roles/export","AUTHORIZATION_EXPORT"),
Arguments.of("/identity/admin/users/export","IDENTITY_USER_EXPORT"),
Arguments.of("/admin/dependents/export","DEPENDENTS_DEPENDENT_EXPORT"),
Arguments.of("/admin/dependents/history/export","DEPENDENTS_DEPENDENT_EXPORT"),
Arguments.of("/vehicles/admin/history/export","VEHICLES_VEHICLE_EXPORT"),
Arguments.of("/vehicles/admin/export","VEHICLES_VEHICLE_EXPORT"),
Arguments.of("/admin/tools/history/export","TOOLS_TOOL_EXPORT"),
Arguments.of("/admin/tools/export","TOOLS_TOOL_EXPORT"),
Arguments.of("/admin/medical-profiles/export","HEALTH_MEDICAL_PROFILE_EXPORT"),
Arguments.of("/admin/medical-profiles/history/export","HEALTH_MEDICAL_PROFILE_EXPORT"),
Arguments.of("/billing/admin/definitions/export","BILLING_EXPORT"),
Arguments.of("/billing/admin/cycles/export","BILLING_EXPORT"),
Arguments.of("/billing/admin/member-charges/export","BILLING_EXPORT"),
Arguments.of("/billing/admin/payments/export","BILLING_EXPORT"),
Arguments.of("/billing/admin/refunds/export","BILLING_EXPORT"));}
    void auth(String permission){when(parser.parseAndValidate("export-test")).thenReturn(new JwtAuthenticatedUser(999L,1999L,"Export Test",Instant.now().plusSeconds(3600)));when(authorities.findAuthorityCodesByUserId(999L)).thenReturn(List.of(permission));}
    @ParameterizedTest @MethodSource("routes") void csvAndPdfExecuteRealQueriesAndDownload(String path,String permission)throws Exception {
        auth(permission);
        for(String format:List.of("CSV","PDF")) {
            var response=mvc.perform(get(path).param("format",format).header("Authorization","Bearer export-test"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andReturn().getResponse();
            assertThat(response.getHeader("Content-Disposition")).startsWith("attachment;");
            assertThat(response.getContentType()).startsWith(format.equals("CSV")?"text/csv":"application/pdf");
            if(format.equals("PDF"))try(var pdf=org.apache.pdfbox.Loader.loadPDF(response.getContentAsByteArray())){assertThat(pdf.getNumberOfPages()).isPositive();}
            else assertThat(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).startsWith("\uFEFF").doesNotContain("passwordHash","tokenHash","storageKey");
        }
    }
    @ParameterizedTest @MethodSource("routes") void requiresAuthentication(String path,String permission)throws Exception{mvc.perform(get(path)).andExpect(status().isUnauthorized());}
    @ParameterizedTest @MethodSource("routes") void readPermissionDoesNotAllowExport(String path,String permission)throws Exception{auth(permission.replace("EXPORT","READ"));mvc.perform(get(path).header("Authorization","Bearer export-test")).andExpect(status().isForbidden());}
    @ParameterizedTest @MethodSource("routes") void invalidFormatFailsBeforeDownload(String path,String permission)throws Exception{auth(permission);mvc.perform(get(path).param("format","XLSX").header("Authorization","Bearer export-test")).andExpect(status().isBadRequest());}
    @ParameterizedTest @MethodSource("routes") void unknownIdIs404(String path,String permission)throws Exception{
        if(path.contains("history"))return;
        auth(permission);String key=path.contains("accounts")?"identityId":path.contains("medical-profiles")?"profileId":path.equals("/authorization/admin/users/export")?"userId":"id";
        mvc.perform(get(path).param(key,"987654321").header("Authorization","Bearer export-test")).andExpect(status().isNotFound());
    }
    @Test void billingInvalidPeriodIs400()throws Exception{auth("BILLING_EXPORT");mvc.perform(get("/billing/admin/payments/export").param("month","13").header("Authorization","Bearer export-test")).andExpect(status().isBadRequest());}

    @Autowired jakarta.persistence.EntityManager em;
    private static final Instant FIXTURE_TIME=Instant.parse("2026-01-01T12:00:00Z");
    private Object seed(String className,Object... pairs)throws Exception {
        var type=Class.forName("com.jeepclub.backend."+className);
        var constructor=type.getDeclaredConstructor();constructor.setAccessible(true);Object entity=constructor.newInstance();
        for(int i=0;i<pairs.length;i+=2)org.springframework.test.util.ReflectionTestUtils.setField(entity,(String)pairs[i],pairs[i+1]);
        em.persist(entity);em.flush();return entity;
    }
    private Long id(Object entity){return (Long)org.springframework.test.util.ReflectionTestUtils.getField(entity,"id");}
    private Object user(String name,String cpf)throws Exception{return seed("iam.identity.infra.persistence.entity.UserEntity","name",name,"cpf",cpf,"status",com.jeepclub.backend.iam.identity.api.module.UserStatus.ACTIVE,"createdAt",FIXTURE_TIME);}
    private String csv(String path,String permission,String... params)throws Exception {
        auth(permission);var request=get(path).header("Authorization","Bearer export-test");
        for(int i=0;i<params.length;i+=2)request.param(params[i],params[i+1]);
        return mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void identityFieldsAndIndividualSelectionNeverExposeStorage()throws Exception {
        var u=user("Titular Exportado","52998224725");
        org.springframework.test.util.ReflectionTestUtils.setField(u,"profilePhotoStorageKey","SECRET_STORAGE_SENTINEL");em.flush();
        user("Outro cadastro","11144477735");
        String result=csv("/identity/admin/users/export","IDENTITY_USER_EXPORT","id",id(u).toString());
        assertThat(result).contains("Titular Exportado","52998224725","Possui foto","Sim").doesNotContain("Outro cadastro","SECRET_STORAGE_SENTINEL");
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void toolsExportTraversesMultipleChunksAndCombinesOwnerNameStatus()throws Exception {
        var u=user("Titular Ferramentas","52998224725");Long owner=id(u);
        for(int i=0;i<205;i++)seed("tools.infra.persistence.entity.ToolEntity","name","Furadeira "+i,"userId",owner,"status",com.jeepclub.backend.tools.core.domain.enums.ToolStatus.ACTIVE,"createdAt",java.time.LocalDateTime.of(2026,1,1,12,0),"photoStorageKey","SECRET_STORAGE_SENTINEL");
        seed("tools.infra.persistence.entity.ToolEntity","name","Inativa","userId",owner,"status",com.jeepclub.backend.tools.core.domain.enums.ToolStatus.INACTIVE,"createdAt",java.time.LocalDateTime.of(2026,1,1,12,0));
        String result=csv("/admin/tools/export","TOOLS_TOOL_EXPORT","userId",owner.toString(),"name","FURADEIRA","status","ACTIVE");
        assertThat(result.lines().count()).isEqualTo(206);assertThat(result).contains("Furadeira 204","Titular Ferramentas").doesNotContain("Inativa","SECRET_STORAGE_SENTINEL");
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void definitionsPreserveUnassignedAndExpandAssignments()throws Exception {
        var u=user("Alvo cadastral","52998224725");
        Object first=definition("Definição com públicos"),second=definition("Sem público");
        em.persist(new com.jeepclub.backend.billing.infra.persistence.entity.assignment.AllMembersChargeAssignmentEntity(null,id(first),true,FIXTURE_TIME,null));
        em.persist(new com.jeepclub.backend.billing.infra.persistence.entity.assignment.UserChargeAssignmentEntity(null,id(first),id(u),true,FIXTURE_TIME,null));em.flush();
        String result=csv("/billing/admin/definitions/export","BILLING_EXPORT");
        assertThat(result.lines().count()).isEqualTo(4);
        assertThat(result).contains("Sem público","Alvo cadastral","Sem atribuição");
    }
    private Object definition(String name)throws Exception{return seed("billing.infra.persistence.entity.ChargeDefinitionEntity","name",name,"defaultAmount",new java.math.BigDecimal("100.00"),"recurrenceType",com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType.YEARLY,"required",true,"paymentAcceptancePolicy",com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy.AFTER_DUE_DATE,"status",com.jeepclub.backend.billing.core.domain.enums.definition.ChargeDefinitionStatus.ACTIVE,"createdAt",FIXTURE_TIME);}
    @Test @org.springframework.transaction.annotation.Transactional
    void medicalHouseholdIncludesOwnerWithoutProfileAndOnlyItsDependents()throws Exception {
        var u=user("Titular médico","52998224725");var other=user("Outro titular","11144477735");
        var dependent=seed("dependents.infra.persistence.entity.DependentEntity","name","Dependente correto","cpf","12345678901","birthDate",java.time.LocalDate.of(2010,1,1),"relationshipType",com.jeepclub.backend.dependents.core.domain.enums.RelationshipType.CHILD,"userId",id(u),"status",com.jeepclub.backend.dependents.core.domain.enums.DependentStatus.ACTIVE,"createdAt",FIXTURE_TIME);
        seed("dependents.infra.persistence.entity.DependentEntity","name","Dependente alheio","cpf","12345678902","birthDate",java.time.LocalDate.of(2010,1,1),"relationshipType",com.jeepclub.backend.dependents.core.domain.enums.RelationshipType.CHILD,"userId",id(other),"status",com.jeepclub.backend.dependents.core.domain.enums.DependentStatus.ACTIVE,"createdAt",FIXTURE_TIME);
        seed("health.infra.persistence.entity.MedicalProfileEntity","ownerType",com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType.DEPENDENT,"ownerId",id(dependent),"bloodType",com.jeepclub.backend.health.core.domain.enums.BloodType.UNKNOWN,"allergies","CLINICAL_SENTINEL","createdAt",FIXTURE_TIME,"updatedAt",FIXTURE_TIME);
        String result=csv("/admin/medical-profiles/export","HEALTH_MEDICAL_PROFILE_EXPORT","userId",id(u).toString(),"household","true");
        assertThat(result.lines().count()).isEqualTo(3);assertThat(result).contains("Titular médico","Dependente correto","CLINICAL_SENTINEL","Sim","Não").doesNotContain("Dependente alheio");
        var logs=em.createQuery("select l from SystemLogEntity l",com.jeepclub.backend.platform.logging.SystemLogEntity.class).getResultList();
        assertThat(logs).allSatisfy(log->assertThat(log.getAction()+log.getPath()).doesNotContain("CLINICAL_SENTINEL"));
    }
    @Test void openApiDocumentsFilesAndMatchingPermissions()throws Exception {
        var response=mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
        routes().forEach(args->{Object[] pair=args.get();var operation=json.path("paths").path((String)pair[0]).path("get");
            assertThat(operation.path("responses").path("200").path("content").has("text/csv")).isTrue();
            assertThat(operation.path("responses").path("200").path("content").has("application/pdf")).isTrue();
            assertThat(operation.path("x-required-permissions").toString()).contains((String)pair[1]);
        });
    }

    @Test @org.springframework.transaction.annotation.Transactional
    void billingPaymentsUseCycleDueDateAndStructuredEventContext()throws Exception {
        var u=user("Pagador identificado","52998224725");var d=definition("Anuidade");
        var event=seed("publications.infra.persistence.entity.EventEntity","authorUserId",id(u),"title","Evento financeiro","content","Contexto","publishedAt",FIXTURE_TIME,"status",com.jeepclub.backend.publications.core.domain.enums.PublicationStatus.PUBLISHED,"createdAt",FIXTURE_TIME,"updatedAt",FIXTURE_TIME,"startsAt",FIXTURE_TIME.plusSeconds(86400));
        var image=new com.jeepclub.backend.publications.infra.persistence.entity.PublicationImageEntity();image.setStorageKey("images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg");image.setPosition(0);image.setPrimary(true);
        ((com.jeepclub.backend.publications.infra.persistence.entity.EventEntity)event).getImages().add(image);em.flush();
        var cycle=cycle(id(d),java.time.LocalDate.of(2026,2,15),"codigo-sem-ano");
        var charge=charge(id(u),id(d),id(cycle),java.time.LocalDate.of(2026,2,15));
        var payment=seed("billing.infra.persistence.entity.MemberPaymentEntity","memberChargeId",id(charge),"amount",new java.math.BigDecimal("100.00"),"paymentMethod",com.jeepclub.backend.billing.core.domain.enums.payment.PaymentMethod.PIX,"status",com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus.PENDING_VALIDATION,"receiptStorageKey","FORBIDDEN_RECEIPT_KEY","createdAt",FIXTURE_TIME,"paidAt",FIXTURE_TIME,"submittedAt",FIXTURE_TIME);
        em.persist(new com.jeepclub.backend.billing.infra.persistence.entity.assignment.EventParticipantsChargeAssignmentEntity(null,id(d),id(event),true,FIXTURE_TIME,null));em.flush();
        Long assignment=em.createQuery("select a.id from EventParticipantsChargeAssignmentEntity a where a.eventId=:id",Long.class).setParameter("id",id(event)).getSingleResult();
        seed("billing.infra.persistence.entity.EventChargeContextEntity","eventId",id(event),"chargeDefinitionId",id(d),"assignmentId",assignment,"cycleId",id(cycle));
        String result=csv("/billing/admin/payments/export","BILLING_EXPORT","year","2026","month","2","eventId",id(event).toString(),"recurrence","YEARLY","paymentMethod","PIX");
        assertThat(result.lines().count()).isEqualTo(2);assertThat(result).contains("Pagador identificado","52998224725","Nome histórico","codigo-sem-ano","Evento financeiro","PENDING_VALIDATION").doesNotContain("FORBIDDEN_RECEIPT_KEY");
        assertThat(csv("/billing/admin/payments/export","BILLING_EXPORT","year","2025").lines().count()).isEqualTo(1);
        assertThat(csv("/billing/admin/payments/export","BILLING_EXPORT","id",id(payment).toString())).contains("Pagador identificado");
        assertThat(csv("/billing/admin/member-charges/export","BILLING_EXPORT","chargeCycleId",id(cycle).toString())).contains("Nome histórico","Pagador identificado");
    }
    private Object cycle(Long definition,java.time.LocalDate due,String code)throws Exception {
        return seed("billing.infra.persistence.entity.ChargeCycleEntity","chargeDefinitionId",definition,"chargeDefinitionNameSnapshot","Nome histórico","chargeDefinitionDefaultAmountSnapshot",new java.math.BigDecimal("100.00"),"chargeDefinitionRecurrenceTypeSnapshot",com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType.YEARLY,"chargeDefinitionRequiredSnapshot",true,"chargeDefinitionPaymentAcceptancePolicySnapshot",com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy.AFTER_DUE_DATE,"code",code,"dueDate",due,"status",com.jeepclub.backend.billing.core.domain.enums.cycle.ChargeCycleStatus.GENERATED,"generatedByUserId",999L,"generatedAt",FIXTURE_TIME,"createdAt",FIXTURE_TIME);
    }
    private Object charge(Long user,Long definition,Long cycle,java.time.LocalDate due)throws Exception {
        return seed("billing.infra.persistence.entity.MemberChargeEntity","userId",user,"chargeDefinitionId",definition,"chargeCycleId",cycle,"originalAmount",new java.math.BigDecimal("100.00"),"finalAmount",new java.math.BigDecimal("100.00"),"dueDate",due,"paymentAcceptancePolicy",com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy.AFTER_DUE_DATE,"status",com.jeepclub.backend.billing.core.domain.enums.charge.MemberChargeStatus.PENDING,"createdAt",FIXTURE_TIME);
    }
    @Test void eventExportRoutesEnforcePermissionAnd404()throws Exception {
        for(String report:List.of("manifest","transport","financial","access","health-coverage","post-event")) {
            String path="/admin/events/987654/reports/"+report+"/export";
            mvc.perform(get(path)).andExpect(status().isUnauthorized());auth("PUBLICATIONS_EVENT_READ_ADMIN");
            mvc.perform(get(path).header("Authorization","Bearer export-test")).andExpect(status().isForbidden());auth("PUBLICATIONS_EXPORT");
            mvc.perform(get(path).header("Authorization","Bearer export-test")).andExpect(status().isNotFound());
        }
        String path="/admin/events/987654/health/USER/1/export";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());auth("PUBLICATIONS_EXPORT");
        mvc.perform(get(path).header("Authorization","Bearer export-test")).andExpect(status().isForbidden());auth("PUBLICATIONS_EVENT_HEALTH_EMERGENCY_READ");
        mvc.perform(get(path).header("Authorization","Bearer export-test")).andExpect(status().isNotFound());
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void authenticationSelectsOnlyAdministrativeMetadataAndOwner()throws Exception {
        var u=user("Usuário autenticação","52998224725");var other=user("Outro","11144477735");
        seed("iam.authentication.infra.persistence.entity.AuthenticationAccountEntity","user",u,"passwordHash","PASSWORD_SENTINEL","accessStatus",com.jeepclub.backend.iam.authentication.core.domain.enums.AuthenticationAccessStatus.ENABLED,"authenticationStatus",com.jeepclub.backend.iam.authentication.core.domain.enums.AuthenticationStatus.ENABLED,"credentialStatus",com.jeepclub.backend.iam.authentication.core.domain.enums.CredentialStatus.PERMANENT,"createdAt",FIXTURE_TIME);
        var session=seed("iam.authentication.infra.persistence.entity.SessionEntity","userId",id(u),"createdAt",FIXTURE_TIME,"expiresAt",FIXTURE_TIME.plusSeconds(3600),"status",com.jeepclub.backend.iam.authentication.core.domain.enums.SessionStatus.ACTIVE);
        seed("iam.authentication.infra.persistence.entity.SessionEntity","userId",id(other),"createdAt",FIXTURE_TIME,"expiresAt",FIXTURE_TIME.plusSeconds(3600),"status",com.jeepclub.backend.iam.authentication.core.domain.enums.SessionStatus.ACTIVE);
        var token=seed("iam.authentication.infra.persistence.entity.RefreshTokenEntity","sessionId",id(session),"tokenHash","TOKEN_SENTINEL","createdAt",FIXTURE_TIME,"expiresAt",FIXTURE_TIME.plusSeconds(3600),"status",com.jeepclub.backend.iam.authentication.core.domain.enums.RefreshTokenStatus.ACTIVE);
        var recovery=seed("iam.authentication.infra.persistence.entity.PasswordRecoveryRequestEntity","userId",id(u),"tokenHash","RECOVERY_SENTINEL","createdAt",FIXTURE_TIME,"expiresAt",FIXTURE_TIME.plusSeconds(3600),"status",com.jeepclub.backend.iam.authentication.core.domain.enums.PasswordRecoveryRequestStatus.OPEN,"method",com.jeepclub.backend.iam.authentication.core.domain.enums.PasswordRecoveryRequestMethod.EMAIL_TOKEN);
        assertThat(csv("/authentication/admin/accounts/export","AUTHENTICATION_EXPORT","identityId",id(u).toString())).contains("PERMANENT").doesNotContain("PASSWORD_SENTINEL");
        assertThat(csv("/authentication/admin/sessions/export","AUTHENTICATION_EXPORT","userId",id(u).toString()).lines().count()).isEqualTo(2);
        assertThat(csv("/authentication/admin/refresh-tokens/export","AUTHENTICATION_EXPORT","id",id(token).toString())).contains("ACTIVE").doesNotContain("TOKEN_SENTINEL");
        assertThat(csv("/authentication/admin/password-recovery-requests/export","AUTHENTICATION_EXPORT","id",id(recovery).toString())).contains("EMAIL_TOKEN").doesNotContain("RECOVERY_SENTINEL");
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void authorizationIncludesUnassignedUsersAndOnlyActiveRolePermissions()throws Exception {
        var u=user("Usuário com acesso","52998224725");user("Usuário sem papel","11144477735");
        var role=seed("iam.authorization.infra.persistence.entity.RoleEntity","name","Papel suspenso","kind",com.jeepclub.backend.iam.authorization.core.domain.enums.RoleKind.CUSTOM,"status",com.jeepclub.backend.iam.authorization.core.domain.enums.RoleStatus.INACTIVE,"createdAt",FIXTURE_TIME);
        var permission=em.createQuery("select p from PermissionEntity p where p.code=:code",com.jeepclub.backend.iam.authorization.infra.persistence.entity.PermissionEntity.class).setParameter("code",com.jeepclub.backend.shared.authorization.PermissionCode.IDENTITY_USER_EXPORT).getSingleResult();
        seed("iam.authorization.infra.persistence.entity.UserRoleEntity","userId",id(u),"role",role,"createdAt",FIXTURE_TIME);
        seed("iam.authorization.infra.persistence.entity.RolePermissionEntity","role",role,"permission",permission,"createdAt",FIXTURE_TIME);
        String all=csv("/authorization/admin/users/export","AUTHORIZATION_EXPORT");
        assertThat(all).contains("Usuário sem papel","Usuário com acesso","Papel suspenso","52998224725").doesNotContain("IDENTITY_USER_EXPORT");
        org.springframework.test.util.ReflectionTestUtils.setField(role,"status",com.jeepclub.backend.iam.authorization.core.domain.enums.RoleStatus.ACTIVE);em.flush();
        assertThat(csv("/authorization/admin/users/export","AUTHORIZATION_EXPORT","userId",id(u).toString())).contains("IDENTITY_USER_EXPORT").doesNotContain("Usuário sem papel");
        assertThat(csv("/authorization/admin/roles/export","AUTHORIZATION_EXPORT","id",id(role).toString())).contains("Papel suspenso");
        assertThat(csv("/authorization/admin/permissions/export","AUTHORIZATION_EXPORT","id",id(permission).toString())).contains("IDENTITY_USER_EXPORT");
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void membershipStatusFilterAndIndividualSelection()throws Exception {
        var pending=seed("memberships.infra.persistence.entity.MembershipApplicationEntity","name","Candidato pendente","cpf","52998224725","status",com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus.PENDING,"requestedAt",FIXTURE_TIME);
        seed("memberships.infra.persistence.entity.MembershipApplicationEntity","name","Candidato rejeitado","cpf","11144477735","status",com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus.REJECTED,"requestedAt",FIXTURE_TIME);
        assertThat(csv("/admin/membership-applications/export","MEMBERSHIP_EXPORT","status","PENDING")).contains("Candidato pendente").doesNotContain("Candidato rejeitado","version");
        assertThat(csv("/admin/membership-applications/export","MEMBERSHIP_EXPORT","id",id(pending).toString()).lines().count()).isEqualTo(2);
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void vehiclesExcludeLegacyDeletedAndPreserveFunctionalFields()throws Exception {
        var u=user("Proprietário","52998224725");
        var active=vehicle(id(u),"ABC1D23","12345678901",com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus.ACTIVE);
        vehicle(id(u),"DEF4G56","12345678902",com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus.SOFT_DELETED);
        String result=csv("/vehicles/admin/export","VEHICLES_VEHICLE_EXPORT","ownerId",id(u).toString());
        assertThat(result.lines().count()).isEqualTo(2);assertThat(result).contains("ABC1D23","Renegade","12345678901","Proprietário").doesNotContain("DEF4G56","PHOTO_SENTINEL");
        assertThat(csv("/vehicles/admin/export","VEHICLES_VEHICLE_EXPORT","id",id(active).toString())).contains("ABC1D23");
    }
    private Object vehicle(Long owner,String plate,String renavam,com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus state)throws Exception {
        return seed("vehicles.infra.persistence.entity.VehicleEntity","ownerId",owner,"plate",plate,"renavam",renavam,"brand","Jeep","model","Renegade","manufacturingYear",2020,"modelYear",2021,"seatingCapacity",5,"fuelType",com.jeepclub.backend.vehicles.core.domain.enums.FuelType.FLEX,"engineDisplacement",1.8,"towing",false,"status",state,"photo","PHOTO_SENTINEL");
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void eventReportsAndIndividualEmergencyExecuteRealContracts()throws Exception {
        var u=user("Participante confirmado","52998224725");
        var event=seed("publications.infra.persistence.entity.EventEntity","authorUserId",id(u),"title","Encontro operacional","content","Passeio","publishedAt",FIXTURE_TIME,"status",com.jeepclub.backend.publications.core.domain.enums.PublicationStatus.PUBLISHED,"createdAt",FIXTURE_TIME,"updatedAt",FIXTURE_TIME,"startsAt",Instant.now().plusSeconds(86400));
        var image=new com.jeepclub.backend.publications.infra.persistence.entity.PublicationImageEntity();image.setStorageKey("images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg");image.setPosition(0);image.setPrimary(true);
        ((com.jeepclub.backend.publications.infra.persistence.entity.EventEntity)event).getImages().add(image);em.flush();
        seed("publications.infra.persistence.entity.EventRegistrationEntity","eventId",id(event),"userId",id(u),"status","CONFIRMED","createdAt",FIXTURE_TIME,"confirmedAt",FIXTURE_TIME);
        seed("health.infra.persistence.entity.MedicalProfileEntity","ownerType",com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType.USER,"ownerId",id(u),"bloodType",com.jeepclub.backend.health.core.domain.enums.BloodType.UNKNOWN,"allergies","INDIVIDUAL_CLINICAL_SENTINEL","createdAt",FIXTURE_TIME,"updatedAt",FIXTURE_TIME);
        for(String report:List.of("manifest","transport","financial","access","health-coverage","post-event"))for(String format:List.of("CSV","PDF")){
            auth("PUBLICATIONS_EXPORT");var response=mvc.perform(get("/admin/events/"+id(event)+"/reports/"+report+"/export").param("format",format).header("Authorization","Bearer export-test")).andExpect(status().isOk()).andReturn().getResponse();
            String text=response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            if(format.equals("PDF"))try(var pdf=org.apache.pdfbox.Loader.loadPDF(response.getContentAsByteArray())){text=new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);}
            assertThat(text).doesNotContain("INDIVIDUAL_CLINICAL_SENTINEL");
            if(report.equals("access"))assertThat(text).contains("Participante confirmado").doesNotContain("Cobranças","Alergias","Validação");
            if(report.equals("post-event"))assertThat(text).contains("CONFIRMED","REJECTED");
        }
        auth("PUBLICATIONS_EVENT_HEALTH_EMERGENCY_READ");
        var response=mvc.perform(get("/admin/events/"+id(event)+"/health/USER/"+id(u)+"/export").header("Authorization","Bearer export-test")).andExpect(status().isOk()).andReturn().getResponse();
        try(var pdf=org.apache.pdfbox.Loader.loadPDF(response.getContentAsByteArray())){assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(pdf)).contains("INDIVIDUAL_CLINICAL_SENTINEL","Participante confirmado");}
        assertThat(csv("/admin/events/export","PUBLICATIONS_EXPORT","id",id(event).toString())).contains("Encontro operacional","Sem regra financeira");
    }
}
