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
    @Test void enabledConfigurationRequiresBusinessApprovalAndEveryFormatParameter() {
        var p=new MemberIssuanceProperties();p.setEnabled(true);assertThatThrownBy(p::validate).isInstanceOf(IllegalStateException.class);
        p.setApproved(true);p.setInitialNumber(100L);p.setWidth(3);assertThatThrownBy(p::validate).isInstanceOf(IllegalStateException.class);
        p.setPrefix("");p.validate();assertThat(p.format(100)).isEqualTo("100");assertThat(p.format(7)).isEqualTo("007");
    }
    @Test void invalidRangesAndOverflowAreRejected() {
        var p=new MemberIssuanceProperties();p.setEnabled(true);p.setApproved(true);p.setInitialNumber(0L);p.setWidth(3);p.setPrefix("");
        assertThatThrownBy(p::validate).isInstanceOf(IllegalStateException.class);p.setInitialNumber(Long.MAX_VALUE);assertThatThrownBy(p::validate).isInstanceOf(IllegalStateException.class);
    }
    @Test void composeEnvironmentBindsTheApprovedRange() {
        var env=new org.springframework.core.env.StandardEnvironment();
        env.getPropertySources().addFirst(new org.springframework.core.env.SystemEnvironmentPropertySource("synthetic-registry-env",java.util.Map.of(
            "MEMBERSHIPS_REGISTRY_ISSUANCE_ENABLED","true","MEMBERSHIPS_REGISTRY_ISSUANCE_APPROVED","true",
            "MEMBERSHIPS_REGISTRY_ISSUANCE_INITIAL_NUMBER","100","MEMBERSHIPS_REGISTRY_ISSUANCE_WIDTH","3","MEMBERSHIPS_REGISTRY_ISSUANCE_PREFIX","")));
        var p=org.springframework.boot.context.properties.bind.Binder.get(env).bind("memberships.registry.issuance",
                org.springframework.boot.context.properties.bind.Bindable.of(MemberIssuanceProperties.class)).get();
        assertThat(p.getInitialNumber()).isEqualTo(100L);p.validate();assertThat(p.format(100)).isEqualTo("100");
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
    }
    @Test void opaqueFormatsArePreservedAndAmbiguityGuardDoesNotRewriteThem() {
        assertThat(new MembershipNumber("027").value()).isEqualTo("027");
        assertThat(new MembershipNumber("027").claimKey()).isEqualTo(new MembershipNumber("27").claimKey());
        assertThat(new MembershipNumber("A-027").claimKey()).isNotEqualTo(new MembershipNumber("a-027").claimKey());
    }
    @Test void invalidControlsAndDestructiveWhitespaceAreRejected() {
        for(String value:new String[]{"", " ","027 "," 027","A\nB","x".repeat(65)}) assertThatThrownBy(() -> new MembershipNumber(value)).isInstanceOf(RuntimeException.class);
    }
    @Test void previewProofExpiresAndCannotBeReusedAcrossProcesses() {
        var now=Instant.parse("2026-10-17T00:00:00Z");var clock=org.mockito.Mockito.mock(Clock.class);org.mockito.Mockito.when(clock.instant()).thenReturn(now);var proof=new MemberImportProof(clock);
        var rows=List.of(new MemberImportRow(1L,"027",true,"synthetic-source"));String token=proof.issue(2L,rows,"Verified");proof.verify(token,2L,rows,"Verified");
        org.mockito.Mockito.when(clock.instant()).thenReturn(now.plusSeconds(901));assertThatThrownBy(() -> proof.verify(token,2L,rows,"Verified")).isInstanceOf(RuntimeException.class);
        var another=new MemberImportProof(Clock.fixed(now,ZoneOffset.UTC));assertThatThrownBy(() -> another.verify(token,2L,rows,"Verified")).isInstanceOf(RuntimeException.class);
    }
}
