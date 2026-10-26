package com.jeepclub.backend.memberships.registry;
import com.jeepclub.backend.memberships.infra.config.MemberIssuanceProperties;
import com.jeepclub.backend.memberships.core.domain.model.MembershipNumber;
import com.jeepclub.backend.memberships.infra.security.MemberImportProof;
import com.jeepclub.backend.memberships.core.application.result.MemberImportRow;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
class MemberRegistryPolicyTest {
    @Test void defaultConfigurationIsDisabledAndDoesNotInventInitialNumber() {
        var p=new MemberIssuanceProperties();p.validate();assertThat(p.enabled()).isFalse();assertThat(p.getInitialNumber()).isNull();
    }
    @Test void enabledConfigurationRequiresApprovalButNoLegacyFormat() {
        var p=new MemberIssuanceProperties();p.setEnabled(true);assertThatThrownBy(p::validate).isInstanceOf(IllegalStateException.class);
        p.setApproved(true);p.validate();assertThat(p.enabled()).isTrue();
    }
    @Test void legacyConfigurationIsRejectedEvenWhenDisabled() {
        var p=new MemberIssuanceProperties();p.setInitialNumber(1L);assertThatThrownBy(p::validate).isInstanceOf(IllegalStateException.class).hasMessageContaining("retired");
        p.setInitialNumber(null);p.setWidth(1);assertThatThrownBy(p::validate).isInstanceOf(IllegalStateException.class);
        p.setWidth(null);p.setPrefix("A");assertThatThrownBy(p::validate).isInstanceOf(IllegalStateException.class);
    }
    @Test void composeEnvironmentBindsTheApprovedRange() {
        var env=new org.springframework.core.env.StandardEnvironment();
        env.getPropertySources().addFirst(new org.springframework.core.env.SystemEnvironmentPropertySource("synthetic-registry-env",java.util.Map.of(
            "MEMBERSHIPS_REGISTRY_ISSUANCE_ENABLED","true","MEMBERSHIPS_REGISTRY_ISSUANCE_APPROVED","true",
            "MEMBERSHIPS_REGISTRY_ISSUANCE_INITIAL_NUMBER","100","MEMBERSHIPS_REGISTRY_ISSUANCE_WIDTH","3","MEMBERSHIPS_REGISTRY_ISSUANCE_PREFIX","")));
        var p=org.springframework.boot.context.properties.bind.Binder.get(env).bind("memberships.registry.issuance",
                org.springframework.boot.context.properties.bind.Bindable.of(MemberIssuanceProperties.class)).get();
        assertThat(p.getInitialNumber()).isEqualTo(100L);assertThatThrownBy(p::validate).isInstanceOf(IllegalStateException.class);
    }
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false)
    @org.springframework.boot.context.properties.EnableConfigurationProperties(MemberIssuanceProperties.class)
    static class PolicyConfiguration { }
    @Test void enablingWithoutApprovedConfigurationFailsApplicationStartup() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner().withUserConfiguration(PolicyConfiguration.class)
                .withPropertyValues("memberships.registry.issuance.enabled=true")
                .run(context -> assertThat(context).hasFailed());
        new org.springframework.boot.test.context.runner.ApplicationContextRunner().withUserConfiguration(PolicyConfiguration.class)
                .run(context -> { assertThat(context).hasNotFailed();assertThat(context.getBean(MemberIssuanceProperties.class).enabled()).isFalse(); });
        new org.springframework.boot.test.context.runner.ApplicationContextRunner().withUserConfiguration(PolicyConfiguration.class)
                .withPropertyValues("memberships.registry.issuance.enabled=false","memberships.registry.issuance.initial-number=1")
                .run(context -> assertThat(context).hasFailed());
    }
    @Test void decimalsAreCanonicalAndOpaqueFormatsAreRejected() {
        assertThat(new MembershipNumber("027").value()).isEqualTo("27");
        assertThat(new MembershipNumber("027").claimKey()).isEqualTo(new MembershipNumber("27").claimKey());
        for(String s:List.of("A-027","a-027","0","000","-1","+1","1.0","1e2","１"))
            assertThatThrownBy(() -> new MembershipNumber(s)).isInstanceOf(RuntimeException.class);
        assertThat(new MembershipNumber("9".repeat(64)).value()).hasSize(64);
        assertThatThrownBy(() -> new MembershipNumber("9".repeat(65))).isInstanceOf(RuntimeException.class);
    }
    @Test void invalidControlsAndDestructiveWhitespaceAreRejected() {
        for(String value:new String[]{"", " ","027 "," 027","A\nB","x".repeat(65)}) assertThatThrownBy(() -> new MembershipNumber(value)).isInstanceOf(RuntimeException.class);
    }
    @Test void previewProofExpiresAndCannotBeReusedAcrossProcesses() {
        var now=Instant.parse("2026-10-17T00:00:00Z");var clock=org.mockito.Mockito.mock(Clock.class);org.mockito.Mockito.when(clock.instant()).thenReturn(now);var proof=new MemberImportProof(clock);
        var rows=List.of(new MemberImportRow(1L,"027",true,"synthetic-source"));String token=proof.issue(2L,rows,"Verified");proof.verify(token,2L,rows,"Verified");
        org.mockito.Mockito.when(clock.instant()).thenReturn(now.plusSeconds(900));assertThatThrownBy(() -> proof.verify(token,2L,rows,"Verified")).isInstanceOf(RuntimeException.class);
        var another=new MemberImportProof(Clock.fixed(now,ZoneOffset.UTC));assertThatThrownBy(() -> another.verify(token,2L,rows,"Verified")).isInstanceOf(RuntimeException.class);
    }
    @Test void proofBindsActorSourceReasonRowsAndConfirmation() {
        var proof=new MemberImportProof(Clock.fixed(Instant.parse("2026-10-18T00:00:00Z"),ZoneOffset.UTC));
        var rows=List.of(new MemberImportRow(1L,"027",true,"synthetic/source-v1/line-1"));
        String token=proof.issue(2L,rows,"Synthetic reason");
        assertThatThrownBy(() -> proof.verify(token,3L,rows,"Synthetic reason")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> proof.verify(token,2L,rows,"Changed reason")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> proof.verify(token,2L,List.of(new MemberImportRow(1L,"027",true,"synthetic/source-v2/line-1")),"Synthetic reason")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> proof.verify(token,2L,List.of(new MemberImportRow(1L,"027",false,"synthetic/source-v1/line-1")),"Synthetic reason")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> proof.verify(token,2L,List.of(new MemberImportRow(1L,"037",true,"synthetic/source-v1/line-1")),"Synthetic reason")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> proof.verify(token+"altered",2L,rows,"Synthetic reason")).isInstanceOf(RuntimeException.class);
    }
    @Test void importApplicationIsDisabledByDefaultAndExplicitEnvironmentBinds() {
        var p=new com.jeepclub.backend.memberships.infra.config.MemberImportProperties();assertThat(p.applyEnabled()).isFalse();
        var env=new org.springframework.core.env.StandardEnvironment();
        env.getPropertySources().addFirst(new org.springframework.core.env.SystemEnvironmentPropertySource("synthetic-import-env",java.util.Map.of("MEMBERSHIPS_REGISTRY_IMPORTS_APPLY_ENABLED","true")));
        var bound=org.springframework.boot.context.properties.bind.Binder.get(env).bind("memberships.registry.imports",org.springframework.boot.context.properties.bind.Bindable.of(com.jeepclub.backend.memberships.infra.config.MemberImportProperties.class)).get();
        assertThat(bound.applyEnabled()).isTrue();
    }
}
