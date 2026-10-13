package com.jeepclub.backend.iam.authentication.infra.integration.identity;

import com.jeepclub.backend.iam.authentication.core.application.service.activation.AccountActivationInvitationService;
import com.jeepclub.backend.iam.authentication.core.port.RandomPasswordGenerator;
import com.jeepclub.backend.iam.identity.api.module.UserActivationMethod;
import com.jeepclub.backend.iam.identity.api.module.spi.AdminUserCredentialProvisioningPort;
import com.jeepclub.backend.iam.identity.api.module.spi.UserAuthenticationProvisioningPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class AdminUserCredentialProvisioningAdapter implements AdminUserCredentialProvisioningPort {
    private final RandomPasswordGenerator passwords;
    private final UserAuthenticationProvisioningPort accounts;
    private final AccountActivationInvitationService invitations;

    @Override
    public String provision(Long userId, UserActivationMethod method, Instant now) {
        String temporaryPassword = passwords.generateSecurePassword();
        accounts.provisionPendingFirstAccess(userId, temporaryPassword, now);
        if (method == UserActivationMethod.EMAIL_INVITATION) {
            invitations.issueForNewUser(userId);
            return null;
        }
        return temporaryPassword;
    }
}
