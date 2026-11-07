package com.jeepclub.backend.authentication.infra.adapter;

import com.jeepclub.backend.iam.authentication.core.application.service.activation.AccountActivationInvitationService;
import com.jeepclub.backend.iam.authentication.core.application.service.internal.TemporaryPasswordIssuer;
import com.jeepclub.backend.iam.authentication.core.port.ManualTemporaryPasswordGenerator;
import com.jeepclub.backend.iam.authentication.core.port.RandomPasswordGenerator;
import com.jeepclub.backend.iam.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.iam.authentication.infra.integration.identity.AdminUserCredentialProvisioningAdapter;
import com.jeepclub.backend.iam.authentication.infra.integration.memberships.CreateUserWithPendingFirstAccessAdapter;
import com.jeepclub.backend.iam.identity.api.module.UserActivationMethod;
import com.jeepclub.backend.iam.identity.api.module.UserRegistration;
import com.jeepclub.backend.iam.identity.api.module.spi.UserAuthenticationProvisioningPort;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ManualTemporaryPasswordFlowTest {
    @Test void identityUsesManualOnlyForManualDelivery() {
        var strong = mock(RandomPasswordGenerator.class); var manual = mock(ManualTemporaryPasswordGenerator.class);
        var accounts = mock(UserAuthenticationProvisioningPort.class); var invitations = mock(AccountActivationInvitationService.class);
        when(strong.generateSecurePassword()).thenReturn("strong-internal"); when(manual.generate()).thenReturn("abcde012");
        var adapter = new AdminUserCredentialProvisioningAdapter(strong, manual, accounts, invitations);
        var now = Instant.now();
        assertThat(adapter.provision(1L, UserActivationMethod.TEMPORARY_PASSWORD, now)).isEqualTo("abcde012");
        assertThat(adapter.provision(2L, UserActivationMethod.EMAIL_INVITATION, now)).isNull();
        verify(accounts).provisionPendingFirstAccess(1L, "abcde012", now);
        verify(accounts).provisionPendingFirstAccess(2L, "strong-internal", now);
        verify(invitations).issueForNewUser(2L);
    }
    @Test void membershipKeepsStrongInternalCredentialForInvitation() {
        var strong = mock(RandomPasswordGenerator.class); var manual = mock(ManualTemporaryPasswordGenerator.class);
        var registration = mock(UserRegistration.class);
        when(strong.generateSecurePassword()).thenReturn("strong-internal"); when(manual.generate()).thenReturn("abcde012");
        when(registration.createPendingFirstAccess(any(), anyString())).thenReturn(1L);
        var adapter = new CreateUserWithPendingFirstAccessAdapter(registration, strong, manual, Clock.systemUTC());
        adapter.createPendingUserWithTemporaryPassword("Name", "email", "cpf", "phone");
        adapter.createPendingUserForActivationLink("Name", "email", "cpf", "phone");
        verify(registration).createPendingFirstAccess(any(), eq("abcde012"));
        verify(registration).createPendingFirstAccess(any(), eq("strong-internal"));
    }
    @Test void recoveryHashesManualCredential() {
        var generator = mock(ManualTemporaryPasswordGenerator.class); var hasher = mock(PasswordHasher.class);
        when(generator.generate()).thenReturn("abcde012"); when(hasher.hash("abcde012")).thenReturn("hash");
        var issued = new TemporaryPasswordIssuer(generator, hasher).issue();
        assertThat(issued.rawPassword()).isEqualTo("abcde012"); assertThat(issued.passwordHash()).isEqualTo("hash");
    }
}
