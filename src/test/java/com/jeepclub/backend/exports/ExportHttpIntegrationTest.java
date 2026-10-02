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
        assertThat(download("/admin/medical-profiles/export","HEALTH_MEDICAL_PROFILE_EXPORT","PDF","userId",id(u).toString(),"household","true"))
            .contains("Titular médico","Dependente correto","CLINICAL_SENTINEL").doesNotContain("Dependente alheio");
        assertThat(csv("/admin/medical-profiles/export","HEALTH_MEDICAL_PROFILE_EXPORT","dependentId",id(dependent).toString())).contains("CLINICAL_SENTINEL");
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void dependentSelectionAndHistoryStaySeparateInBothFormats()throws Exception {
        var u=user("Titular dependentes","52998224725");var other=user("Outro titular","11144477735");
        Object disabled=null;
        for(var state:com.jeepclub.backend.dependents.core.domain.enums.DependentStatus.values()) {
            var dep=seed("dependents.infra.persistence.entity.DependentEntity","name","Dependente "+state,"cpf",state.name().equals("ACTIVE")?"12345678901":"12345678902","birthDate",java.time.LocalDate.of(2010,1,1),"relationshipType",com.jeepclub.backend.dependents.core.domain.enums.RelationshipType.CHILD,"userId",id(u),"status",state,"createdAt",FIXTURE_TIME);
            if(state.name().equals("DISABLED"))disabled=dep;
        }
        seed("dependents.infra.persistence.entity.DependentEntity","name","Dependente alheio","cpf","12345678903","birthDate",java.time.LocalDate.of(2010,1,1),"relationshipType",com.jeepclub.backend.dependents.core.domain.enums.RelationshipType.CHILD,"userId",id(other),"status",com.jeepclub.backend.dependents.core.domain.enums.DependentStatus.ACTIVE,"createdAt",FIXTURE_TIME);
        seed("dependents.infra.persistence.entity.DependentHistoryEntity","dependentId",999999L,"name","Dependente removido","cpf","12345678904","birthDate",java.time.LocalDate.of(2010,1,1),"relationshipType",com.jeepclub.backend.dependents.core.domain.enums.RelationshipType.CHILD,"userId",id(u),"status",com.jeepclub.backend.dependents.core.domain.enums.DependentStatus.DISABLED,"createdAt",FIXTURE_TIME,"deletedAt",FIXTURE_TIME,"deletedByUserId",999L);
        for(String format:List.of("CSV","PDF")) {
            assertThat(download("/admin/dependents/export","DEPENDENTS_DEPENDENT_EXPORT",format,"userId",id(u).toString())).contains("Titular dependentes","Dependente ACTIVE","Dependente DISABLED").doesNotContain("Dependente alheio","Dependente removido");
            assertThat(download("/admin/dependents/export","DEPENDENTS_DEPENDENT_EXPORT",format,"id",id(disabled).toString())).contains("Dependente DISABLED").doesNotContain("Dependente ACTIVE");
            assertThat(download("/admin/dependents/history/export","DEPENDENTS_DEPENDENT_EXPORT",format)).contains("Dependente removido","Excluído em").doesNotContain("Dependente ACTIVE");
        }
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void membershipPeriodsAndBlocksPreserveSelectionWithoutTechnicalFields()throws Exception {
        var early=seed("memberships.infra.persistence.entity.MembershipApplicationEntity","name","Anterior","cpf","52998224725","status",com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus.PENDING,"requestedAt",FIXTURE_TIME);
        seed("memberships.infra.persistence.entity.MembershipApplicationEntity","name","Selecionado","cpf","11144477735","status",com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus.APPROVED,"requestedAt",FIXTURE_TIME.plusSeconds(86400));
        var block=seed("memberships.infra.persistence.entity.MembershipApplicantBlockEntity","cpf","52998224725","activeCpf","52998224725","reason","Bloqueio administrativo","blockedAt",FIXTURE_TIME,"blockedByUserId",999L);
        for(String format:List.of("CSV","PDF")) {
            assertThat(download("/admin/membership-applications/export","MEMBERSHIP_EXPORT",format,"from",FIXTURE_TIME.plusSeconds(1).toString(),"to",FIXTURE_TIME.plusSeconds(172800).toString())).contains("Selecionado").doesNotContain("Anterior");
            assertThat(download("/admin/membership-applications/blocks/export","MEMBERSHIP_EXPORT",format,"id",id(block).toString())).contains("Bloqueio administrativo").doesNotContain("activeCpf","version","tokenHash");
        }
        assertThat(csv("/admin/membership-applications/blocks/export","MEMBERSHIP_EXPORT","from",FIXTURE_TIME.plusSeconds(1).toString()).lines().count()).isEqualTo(1);
    }
    @Test void openApiDocumentsFilesAndMatchingPermissions()throws Exception {
        var response=mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
        var contracts=new ArrayList<Arguments>(routes().toList());
        for(String report:List.of("manifest","transport","financial","access","health-coverage","post-event"))
            contracts.add(Arguments.of("/admin/events/{eventId}/reports/"+report+"/export","PUBLICATIONS_EXPORT"));
        contracts.add(Arguments.of("/admin/events/{eventId}/health/{type}/{target}/export","PUBLICATIONS_EVENT_HEALTH_EMERGENCY_READ"));
        assertThat(contracts).hasSize(38);
        contracts.forEach(args->{Object[] pair=args.get();String path=(String)pair[0];var operation=json.path("paths").path(path).path("get");
            var success=operation.path("responses").path("200");
            var media=new HashSet<String>();success.path("content").fieldNames().forEachRemaining(media::add);
            assertThat(media).as(path).containsExactlyInAnyOrderElementsOf(path.contains("/health/{type}")?List.of("application/pdf"):List.of("text/csv","application/pdf"));
            media.forEach(type->{var schema=success.path("content").path(type).path("schema");
                assertThat(schema.path("type").asText()).as(path+" "+type).isEqualTo("string");
                assertThat(schema.path("format").asText()).as(path+" "+type).isEqualTo("binary");});
            assertThat(success.path("headers").path("Content-Disposition").path("schema").path("type").asText()).as(path).isEqualTo("string");
            assertThat(success.path("headers").has("Cache-Control")).as(path).isTrue();
            assertThat(operation.path("x-required-permissions").toString()).as(path).contains((String)pair[1]);
            for(String error:List.of("400","401","403","404","413","500")) {
                var responseError=operation.path("responses").path(error);
                assertThat(responseError.path("description").asText()).as(path+" "+error).isNotBlank();
                assertThat(responseError.path("content").path("application/problem+json").path("schema").isMissingNode()).as(path+" "+error).isFalse();
            }
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
    @Autowired com.jeepclub.backend.publications.core.application.service.AdminEventService eventAdministration;
    @Autowired com.jeepclub.backend.publications.core.repository.EventOperationsRepository eventOperations;
    @Autowired com.jeepclub.backend.publications.core.repository.PublicationRepository publications;
    @Autowired com.jeepclub.backend.vehicles.core.application.service.vehicle.AdminVehicleService vehicleAdministration;
    @Autowired com.jeepclub.backend.dependents.core.application.service.dependent.DependentService dependentAdministration;
    @Autowired com.jeepclub.backend.vehicles.api.module.EventVehicleQuery eventVehicles;
    @Autowired com.jeepclub.backend.dependents.api.module.DependentsQuery eventDependents;
    @Test @org.springframework.transaction.annotation.Transactional
    void postEventKeepsVehicleAndDependentAfterTheirRealHardDeletes()throws Exception {
        var owner=user("Titular da viagem","52998224725");
        var vehicle=vehicle(id(owner),"ABC1D23","12345678901",com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus.ACTIVE);
        ((com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleEntity)vehicle).setNickname("Jeep da viagem");
        var dependent=seed("dependents.infra.persistence.entity.DependentEntity","name","Dependente da viagem","cpf","12345678902",
            "birthDate",java.time.LocalDate.of(2010,1,1),"relationshipType",com.jeepclub.backend.dependents.core.domain.enums.RelationshipType.CHILD,
            "userId",id(owner),"status",com.jeepclub.backend.dependents.core.domain.enums.DependentStatus.ACTIVE,"createdAt",FIXTURE_TIME);
        var event=seed("publications.infra.persistence.entity.EventEntity","authorUserId",id(owner),"title","Viagem auditável",
            "content","Histórico","publishedAt",FIXTURE_TIME,"status",com.jeepclub.backend.publications.core.domain.enums.PublicationStatus.PUBLISHED,
            "createdAt",FIXTURE_TIME,"updatedAt",FIXTURE_TIME,"startsAt",FIXTURE_TIME.plusSeconds(86400));
        var image=new com.jeepclub.backend.publications.infra.persistence.entity.PublicationImageEntity();
        image.setStorageKey("images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg");
        image.setPosition(0);image.setPrimary(true);
        ((com.jeepclub.backend.publications.infra.persistence.entity.EventEntity)event).getImages().add(image);
        em.flush();
        Long eventId=id(event),vehicleId=id(vehicle),dependentId=id(dependent);
        eventOperations.save(new com.jeepclub.backend.publications.core.domain.model.EventRegistration(null,eventId,id(owner),
            com.jeepclub.backend.publications.core.domain.model.EventRegistration.Status.CONFIRMED,
            List.of(new com.jeepclub.backend.publications.core.domain.model.EventRegistration.Allocation(vehicleId,true,List.of(dependentId))),
            FIXTURE_TIME,FIXTURE_TIME,null));
        eventAdministration.finish(eventId);
        eventAdministration.delete(eventId,999L);
        em.flush();em.clear();
        for(String format:List.of("CSV","PDF"))
            assertThat(download("/admin/events/"+eventId+"/reports/post-event/export","PUBLICATIONS_EXPORT",format))
                .contains("ABC1D23","Jeep da viagem","Jeep Renegade","Dependente da viagem","Titular da viagem");

        vehicleAdministration.delete(vehicleId,999L);
        dependentAdministration.delete(dependentId,id(owner));
        em.flush();em.clear();
        assertThat(em.find(com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleEntity.class,vehicleId)).isNull();
        assertThat(em.find(com.jeepclub.backend.dependents.infra.persistence.entity.DependentEntity.class,dependentId)).isNull();
        assertThat(em.createQuery("select count(h) from VehicleHistoryEntity h where h.vehicleId=:id",Long.class).setParameter("id",vehicleId).getSingleResult()).isEqualTo(1);
        assertThat(em.createQuery("select count(h) from DependentHistoryEntity h where h.dependentId=:id",Long.class).setParameter("id",dependentId).getSingleResult()).isEqualTo(1);
        assertThat(eventVehicles.findActive(vehicleId)).isEmpty();
        assertThat(eventVehicles.findActiveBatch(List.of(vehicleId))).isEmpty();
        assertThat(eventVehicles.findDetailsBatch(List.of(vehicleId))).isEmpty();
        assertThat(eventDependents.existsActiveById(dependentId)).isFalse();
        assertThat(eventDependents.isActiveDependentOfUser(dependentId,id(owner))).isFalse();
        for(String format:List.of("CSV","PDF")) {
            String post=download("/admin/events/"+eventId+"/reports/post-event/export","PUBLICATIONS_EXPORT",format);
            assertThat(post).contains("ABC1D23","Jeep da viagem","Jeep Renegade","Dependente da viagem",
                "Titular da viagem",vehicleId.toString())
                .doesNotContain("Cadastro indisponível","Veículo indisponível");
            if(format.equals("CSV"))assertThat(post).contains("\"ABC1D23\";\"2\";\"3\"");
            else assertThat(post).contains("capacidade: 5","vagas: 3");
        }
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void historicalPresentationPrefersCurrentAndHandlesUnknownIds()throws Exception {
        var owner=user("Titular atual","52998224725");
        var currentVehicle=vehicle(id(owner),"ABC1D23","12345678901",com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus.ACTIVE);
        var currentDependent=seed("dependents.infra.persistence.entity.DependentEntity","name","Dependente atual","cpf","12345678902",
            "birthDate",java.time.LocalDate.of(2010,1,1),"relationshipType",com.jeepclub.backend.dependents.core.domain.enums.RelationshipType.CHILD,
            "userId",id(owner),"status",com.jeepclub.backend.dependents.core.domain.enums.DependentStatus.ACTIVE,"createdAt",FIXTURE_TIME);
        seed("vehicles.infra.persistence.entity.VehicleHistoryEntity","vehicleId",id(currentVehicle),"nickname","Antigo",
            "plate","DEF4G56","renavam","12345678909","brand","Outra","model","Modelo antigo",
            "manufacturingYear",2019,"modelYear",2020,"seatingCapacity",2,
            "fuelType",com.jeepclub.backend.vehicles.core.domain.enums.FuelType.FLEX,"engineDisplacement",1.8,
            "status",com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus.ACTIVE,"towing",false,
            "ownerId",id(owner),"deletedByUserId",999L,"createdAt",FIXTURE_TIME,"deletedAt",FIXTURE_TIME);
        seed("dependents.infra.persistence.entity.DependentHistoryEntity","dependentId",id(currentDependent),
            "name","Dependente antigo","cpf","12345678903","birthDate",java.time.LocalDate.of(2010,1,1),
            "relationshipType",com.jeepclub.backend.dependents.core.domain.enums.RelationshipType.CHILD,
            "userId",id(owner),"status",com.jeepclub.backend.dependents.core.domain.enums.DependentStatus.ACTIVE,
            "deletedByUserId",999L,"createdAt",FIXTURE_TIME,"deletedAt",FIXTURE_TIME);
        em.flush();em.clear();
        assertThat(eventVehicles.findPresentationDetailsBatch(List.of(id(currentVehicle),987654321L)))
            .extracting(com.jeepclub.backend.vehicles.api.module.EventVehicleQuery.Details::plate).containsExactly("ABC1D23");
        assertThat(eventDependents.findPresentationDetailsByIds(List.of(id(currentDependent),987654321L)))
            .extracting(com.jeepclub.backend.dependents.api.module.DependentsQuery.Details::name).containsExactly("Dependente atual");
        assertThatThrownBy(()->eventVehicles.findPresentationDetailsBatch(java.util.Collections.nCopies(501,987654321L)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->eventDependents.findPresentationDetailsByIds(java.util.Collections.nCopies(501,987654321L)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test @org.springframework.transaction.annotation.Transactional
    void publicationCatalogsRequestsAndDeletionSnapshotsKeepTheirOwnData()throws Exception {
        var u=user("Autor cadastral","52998224725");
        var notice=seed("publications.infra.persistence.entity.NoticeEntity","authorUserId",id(u),"title","Aviso exportado","content","Conteúdo do aviso","status",com.jeepclub.backend.publications.core.domain.enums.PublicationStatus.ARCHIVED,"createdAt",FIXTURE_TIME,"updatedAt",FIXTURE_TIME,"archivedAt",FIXTURE_TIME);
        var service=seed("publications.infra.persistence.entity.ServicePublicationEntity","authorUserId",id(u),"title","Serviço exportado","content","Conteúdo do serviço","status",com.jeepclub.backend.publications.core.domain.enums.PublicationStatus.ARCHIVED,"createdAt",FIXTURE_TIME,"updatedAt",FIXTURE_TIME,"archivedAt",FIXTURE_TIME,"sourceRequestId",999999L,"amount",new java.math.BigDecimal("42.00"),"contactPhone","12999999999");
        var request=seed("publications.infra.persistence.entity.ServicePublicationRequestEntity","requestedByUserId",id(u),"title","Proposta pendente","content","Conteúdo proposto","amount",new java.math.BigDecimal("55.00"),"contactPhone","12888888888","status",com.jeepclub.backend.publications.core.domain.enums.ServicePublicationRequestStatus.PENDING,"requestedAt",FIXTURE_TIME,"updatedAt",FIXTURE_TIME);
        var change=seed("publications.infra.persistence.entity.ServicePublicationChangeRequestEntity","servicePublicationId",id(service),"requestedByUserId",id(u),"proposedTitle","Alteração rejeitada","proposedContent","Conteúdo rejeitado","proposedAmount",new java.math.BigDecimal("65.00"),"proposedContactPhone","12777777777","status",com.jeepclub.backend.publications.core.domain.enums.ServicePublicationChangeRequestStatus.REJECTED,"rejectionReason","Revisão administrativa","requestedAt",FIXTURE_TIME,"updatedAt",FIXTURE_TIME);
        for(String format:List.of("CSV","PDF")) {
            assertThat(download("/admin/notices/export","PUBLICATIONS_EXPORT",format,"id",id(notice).toString(),"status","ARCHIVED")).contains("Aviso exportado","Autor cadastral").doesNotContain("Serviço exportado");
            assertThat(download("/admin/services/export","PUBLICATIONS_EXPORT",format,"id",id(service).toString())).contains("Serviço exportado","42,00","12999999999").doesNotContain("Proposta pendente");
            assertThat(download("/admin/service-publication-requests/export","PUBLICATIONS_EXPORT",format,"id",id(request).toString(),"status","PENDING")).contains("Proposta pendente","55,00").doesNotContain("Alteração rejeitada");
            assertThat(download("/admin/service-publication-change-requests/export","PUBLICATIONS_EXPORT",format,"id",id(change).toString(),"status","REJECTED")).contains("Alteração rejeitada","65,00","Revisão administrativa").doesNotContain("Proposta pendente");
        }
        assertThat(csv("/admin/service-publication-requests/export","PUBLICATIONS_EXPORT","status","APPROVED").lines().count()).isEqualTo(1);
        publications.delete(id(notice),999L,FIXTURE_TIME.plusSeconds(1));publications.delete(id(service),999L,FIXTURE_TIME.plusSeconds(1));
        for(String format:List.of("CSV","PDF")) {
            assertThat(download("/admin/notices/history/export","PUBLICATIONS_EXPORT",format)).contains("Aviso exportado","Excluído em");
            assertThat(download("/admin/services/history/export","PUBLICATIONS_EXPORT",format)).contains("Serviço exportado","42,00","12999999999");
            assertThat(download("/admin/notices/export","PUBLICATIONS_EXPORT",format)).doesNotContain("Aviso exportado");
            assertThat(download("/admin/services/export","PUBLICATIONS_EXPORT",format)).doesNotContain("Serviço exportado");
        }
    }

    @Test @org.springframework.transaction.annotation.Transactional
    void postEventAndAllBillingProductsSurviveRealHardDelete()throws Exception {
        var u=user("Participante histórico","52998224725");
        var d=definition("Cobrança histórica");
        var event=seed("publications.infra.persistence.entity.EventEntity","authorUserId",id(u),"title","Evento removido auditável","content","Histórico","publishedAt",FIXTURE_TIME,"status",com.jeepclub.backend.publications.core.domain.enums.PublicationStatus.PUBLISHED,"createdAt",FIXTURE_TIME,"updatedAt",FIXTURE_TIME,"startsAt",FIXTURE_TIME.plusSeconds(86400));
        var image=new com.jeepclub.backend.publications.infra.persistence.entity.PublicationImageEntity();image.setStorageKey("images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg");image.setPosition(0);image.setPrimary(true);
        ((com.jeepclub.backend.publications.infra.persistence.entity.EventEntity)event).getImages().add(image);em.flush();
        Long eventId=id(event);
        var v=vehicle(id(u),"ABC1D23","12345678901",com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus.ACTIVE);
        eventOperations.save(new com.jeepclub.backend.publications.core.domain.model.EventRegistration(null,eventId,id(u),com.jeepclub.backend.publications.core.domain.model.EventRegistration.Status.CONFIRMED,List.of(new com.jeepclub.backend.publications.core.domain.model.EventRegistration.Allocation(id(v),true,List.of())),FIXTURE_TIME,FIXTURE_TIME,null));
        seed("publications.infra.persistence.entity.EventGuestRequestEntity","eventId",eventId,"requesterUserId",id(u),"vehicleId",id(v),"approvedVehicleId",id(v),"cpf","12345678909","guestName","Convidado histórico","status","APPROVED","createdAt",FIXTURE_TIME,"reviewedAt",FIXTURE_TIME);
        var cycle=cycle(id(d),java.time.LocalDate.of(2026,2,15),"ciclo-historico");
        var charge=charge(id(u),id(d),id(cycle),java.time.LocalDate.of(2026,2,15));
        var payment=seed("billing.infra.persistence.entity.MemberPaymentEntity","memberChargeId",id(charge),"amount",new java.math.BigDecimal("100.00"),"paymentMethod",com.jeepclub.backend.billing.core.domain.enums.payment.PaymentMethod.PIX,"status",com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus.CONFIRMED,"receiptStorageKey","HISTORICAL_RECEIPT_SECRET","createdAt",FIXTURE_TIME,"paidAt",FIXTURE_TIME,"submittedAt",FIXTURE_TIME,"confirmedAt",FIXTURE_TIME,"confirmedByUserId",999L);
        seed("billing.infra.persistence.entity.MemberRefundEntity","memberChargeId",id(charge),"memberPaymentId",id(payment),"chargeCycleId",id(cycle),"userId",id(u),"amount",new java.math.BigDecimal("100.00"),"reason",com.jeepclub.backend.billing.core.domain.enums.refund.RefundReason.MEMBER_REQUEST,"status",com.jeepclub.backend.billing.core.domain.enums.refund.MemberRefundStatus.REQUESTED,"requestedAt",FIXTURE_TIME,"createdAt",FIXTURE_TIME);
        var assignment=new com.jeepclub.backend.billing.infra.persistence.entity.assignment.EventParticipantsChargeAssignmentEntity(null,id(d),eventId,true,FIXTURE_TIME,null);
        em.persist(assignment);em.flush();
        seed("billing.infra.persistence.entity.EventChargeContextEntity","eventId",eventId,"chargeDefinitionId",id(d),"assignmentId",id(assignment),"cycleId",id(cycle));
        eventOperations.replaceRules(eventId,List.of(new com.jeepclub.backend.publications.core.domain.model.EventChargeRule(eventId,id(d),true,FIXTURE_TIME.plusSeconds(86400),java.time.LocalDate.of(2026,2,15))));
        var pendingUser=user("Inscrição histórica pendente","11144477735");
        eventOperations.save(new com.jeepclub.backend.publications.core.domain.model.EventRegistration(null,eventId,id(pendingUser),com.jeepclub.backend.publications.core.domain.model.EventRegistration.Status.PENDING_PAYMENT,List.of(),FIXTURE_TIME,null,null));

        for(String product:List.of("definitions","cycles","member-charges","payments","refunds"))
            assertThat(csv("/billing/admin/"+product+"/export","BILLING_EXPORT","eventId",eventId.toString()).lines().count()).as(product+" ativo").isEqualTo(2);
        eventAdministration.finish(eventId);
        eventAdministration.delete(eventId,999L);
        em.flush();em.clear();
        assertThat(em.find(com.jeepclub.backend.publications.infra.persistence.entity.EventEntity.class,eventId)).isNull();
        assertThat(em.createQuery("select count(e) from EventHistoryEntity e where e.publicationId=:id",Long.class).setParameter("id",eventId).getSingleResult()).isEqualTo(1);
        assertThat(em.createQuery("select count(e) from EventChargeContextEntity e where e.eventId=:id",Long.class).setParameter("id",eventId).getSingleResult()).isEqualTo(1);
        for(String format:List.of("CSV","PDF")) {
            String post=download("/admin/events/"+eventId+"/reports/post-event/export","PUBLICATIONS_EXPORT",format);
            assertThat(post).contains("Evento removido auditável","FINISHED","Participante histórico","Convidado histórico","ABC1D23","CONFIRMED","Histórico","PENDING_PAYMENT");
            for(String product:List.of("definitions","cycles","member-charges","payments","refunds")) {
                String result=download("/billing/admin/"+product+"/export","BILLING_EXPORT",format,"eventId",eventId.toString());
                assertThat(result).as(product+" histórico "+format).contains("Evento removido auditável").doesNotContain("HISTORICAL_RECEIPT_SECRET");
                if(format.equals("CSV"))assertThat(result.lines().count()).as(product).isEqualTo(2);
                if(List.of("member-charges","payments","refunds").contains(product))assertThat(result).contains("Participante histórico","Nome histórico");
            }
        }
        auth("PUBLICATIONS_EXPORT");
        mvc.perform(get("/admin/events/"+eventId+"/reports/manifest/export").header("Authorization","Bearer export-test")).andExpect(status().isNotFound());
        assertThat(eventOperations.registrations(eventId)).hasSize(2);
        assertThat(eventOperations.registrations(eventId).stream().filter(r->r.userId().equals(id(pendingUser))).findFirst().orElseThrow().status())
            .isEqualTo(com.jeepclub.backend.publications.core.domain.model.EventRegistration.Status.PENDING_PAYMENT);
    }
    private String download(String path,String permission,String format,String...params)throws Exception {
        auth(permission);var request=get(path).param("format",format).header("Authorization","Bearer export-test");
        for(int i=0;i<params.length;i+=2)request.param(params[i],params[i+1]);
        var response=mvc.perform(request).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse();
        assertThat(response.getHeader("Content-Disposition")).startsWith("attachment;");
        assertThat(response.getContentType()).startsWith(format.equals("CSV")?"text/csv":"application/pdf");
        if(format.equals("PDF"))try(var pdf=org.apache.pdfbox.Loader.loadPDF(response.getContentAsByteArray())){return new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);}
        return response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }
    @Test void billingEventWithoutFinancialContextIs404()throws Exception {
        auth("BILLING_EXPORT");
        for(String product:List.of("definitions","cycles","member-charges","payments","refunds"))
            mvc.perform(get("/billing/admin/"+product+"/export").param("eventId","987654321").header("Authorization","Bearer export-test"))
                .andExpect(status().isNotFound()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void latestPaymentUsesCreationTimeThenIdAsInEventFinance()throws Exception {
        var u=user("Pagador","52998224725");var d=definition("Anuidade");
        var cycle=cycle(id(d),java.time.LocalDate.of(2026,2,15),"ciclo");
        var charge=charge(id(u),id(d),id(cycle),java.time.LocalDate.of(2026,2,15));
        var latest=seed("billing.infra.persistence.entity.MemberPaymentEntity","memberChargeId",id(charge),"amount",new java.math.BigDecimal("100.00"),"paymentMethod",com.jeepclub.backend.billing.core.domain.enums.payment.PaymentMethod.PIX,"status",com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus.PENDING_VALIDATION,"receiptStorageKey","LATEST_RECEIPT_SECRET","createdAt",FIXTURE_TIME.plusSeconds(10),"paidAt",FIXTURE_TIME,"submittedAt",FIXTURE_TIME);
        seed("billing.infra.persistence.entity.MemberPaymentEntity","memberChargeId",id(charge),"amount",new java.math.BigDecimal("99.00"),"paymentMethod",com.jeepclub.backend.billing.core.domain.enums.payment.PaymentMethod.CASH,"status",com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus.REJECTED,"receiptStorageKey","LATEST_RECEIPT_SECRET","createdAt",FIXTURE_TIME,"paidAt",FIXTURE_TIME,"submittedAt",FIXTURE_TIME);
        var rows=csv("/billing/admin/member-charges/export","BILLING_EXPORT","id",id(charge).toString()).lines().toList();
        assertThat(rows).hasSize(2);assertThat(rows.get(1)).endsWith("\""+id(latest)+"\";\"100,00\";\"PIX\";\"PENDING_VALIDATION\";\"01/01/2026 09:00:00 -03:00\"");
        var tie=seed("billing.infra.persistence.entity.MemberPaymentEntity","memberChargeId",id(charge),"amount",new java.math.BigDecimal("101.00"),"paymentMethod",com.jeepclub.backend.billing.core.domain.enums.payment.PaymentMethod.PIX,"status",com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus.PENDING_VALIDATION,"receiptStorageKey","LATEST_RECEIPT_SECRET","createdAt",FIXTURE_TIME.plusSeconds(10),"paidAt",FIXTURE_TIME,"submittedAt",FIXTURE_TIME);
        assertThat(csv("/billing/admin/member-charges/export","BILLING_EXPORT","id",id(charge).toString())).contains("\""+id(tie)+"\";\"101,00\";\"PIX\"");
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
        for(int i=0;i<2001;i++) {
            var guest=new com.jeepclub.backend.publications.infra.persistence.entity.EventGuestRequestEntity();
            guest.setEventId(id(event));guest.setRequesterUserId(id(u));guest.setCpf(String.format("%011d",i));
            guest.setGuestName("Convidado "+i);guest.setStatus("PENDING");guest.setCreatedAt(FIXTURE_TIME);em.persist(guest);
        }
        em.flush();auth("PUBLICATIONS_EXPORT");
        for(String format:List.of("CSV","PDF"))mvc.perform(get("/admin/events/"+id(event)+"/reports/manifest/export").param("format",format).header("Authorization","Bearer export-test"))
            .andExpect(status().isPayloadTooLarge()).andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(header().doesNotExist("Content-Disposition"));
    }
}
