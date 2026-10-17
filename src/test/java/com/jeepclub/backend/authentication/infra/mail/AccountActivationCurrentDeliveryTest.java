package com.jeepclub.backend.authentication.infra.mail;

import com.jeepclub.backend.iam.authentication.infra.mail.AccountActivationCurrentDelivery;
import com.jeepclub.backend.iam.authentication.infra.mail.AccountActivationMailSender;
import com.jeepclub.backend.iam.authentication.core.application.service.activation.AccountActivationMailEvent;
import com.jeepclub.backend.iam.authentication.core.domain.model.AccountActivationInvitation;
import com.jeepclub.backend.iam.authentication.core.repository.AccountActivationInvitationRepository;
import com.jeepclub.backend.iam.identity.api.module.UserIdentityLock;
import com.jeepclub.backend.iam.identity.api.module.UserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountActivationCurrentDeliveryTest {
    private static final Instant NOW=Instant.parse("2026-10-14T12:00:00Z");
    @Mock UserIdentityLock users;
    @Mock AccountActivationInvitationRepository invitations;
    @Mock AccountActivationMailSender sender;
    @Mock com.jeepclub.backend.iam.authentication.core.repository.AuthenticationAccountRepository accounts;
    private final java.time.Clock clock=java.time.Clock.fixed(NOW,java.time.ZoneOffset.UTC);
    private final AccountActivationMailEvent event=new AccountActivationMailEvent(42L,"current@example.com","Name","raw-old","old-hash");

    AccountActivationCurrentDelivery delivery() { return new AccountActivationCurrentDelivery(users,invitations,sender,accounts,clock); }
    void identity() {
        when(accounts.findByIdentityIdForUpdate(42L)).thenReturn(Optional.of(com.jeepclub.backend.iam.authentication.core.domain.model.AuthenticationAccount.createPendingFirstAccess(42L,"hash",NOW.minusSeconds(60))));
        when(users.byId(42L)).thenReturn(Optional.of(new UserDetails(42L,"Name",null,"current@example.com","52998224725",null,null,null,true,NOW.minusSeconds(60),null,null)));
    }

    @Test void obsoleteEventCannotSendAfterTokenRotationEvenWhenAddressIsUnchanged() {
        identity();
        when(invitations.findById(42L)).thenReturn(Optional.of(AccountActivationInvitation.create(42L,"new-hash",NOW.plusSeconds(3600))));
        assertThat(delivery().sendIfCurrent(event)).isFalse();
        verifyNoInteractions(sender);
    }

    @Test void expiredInvitationCannotBeDeliveredByDelayedCallback() {
        identity();
        when(invitations.findById(42L)).thenReturn(Optional.of(AccountActivationInvitation.create(42L,"old-hash",NOW.minusSeconds(1))));
        assertThat(delivery().sendIfCurrent(event)).isFalse();
        verifyNoInteractions(sender);
    }
    @Test void currentUnexpiredInvitationToPendingAccountIsDelivered() {
        identity();
        when(invitations.findById(42L)).thenReturn(Optional.of(AccountActivationInvitation.create(42L,"old-hash",NOW.plusSeconds(3600))));
        assertThat(delivery().sendIfCurrent(event)).isTrue();
        verify(sender).send("current@example.com","Name","raw-old");
    }

    @Test void invitationExpiringExactlyNowIsNotDelivered() {
        identity();
        when(invitations.findById(42L)).thenReturn(Optional.of(AccountActivationInvitation.create(42L,"old-hash",NOW)));
        assertThat(delivery().sendIfCurrent(event)).isFalse();
        verifyNoInteractions(sender);
    }

    @Test void accountThatBecamePermanentCannotReceivePendingInvitation() {
        when(users.byId(42L)).thenReturn(Optional.of(new UserDetails(42L,"Name",null,"current@example.com","52998224725",null,null,null,true,NOW.minusSeconds(60),null,null)));
        when(accounts.findByIdentityIdForUpdate(42L)).thenReturn(Optional.of(com.jeepclub.backend.iam.authentication.core.domain.model.AuthenticationAccount.create(42L,"hash",NOW.minusSeconds(60))));
        assertThat(delivery().sendIfCurrent(event)).isFalse();
        verifyNoInteractions(invitations,sender);
    }

    @Test void blockedPendingAccountCannotReceiveInvitation() {
        identity();
        var account=accounts.findByIdentityIdForUpdate(42L).orElseThrow();
        for(int i=0;i<5;i++) account.registerFailedLogin();
        assertThat(delivery().sendIfCurrent(event)).isFalse();
        verifyNoInteractions(invitations,sender);
    }

}
