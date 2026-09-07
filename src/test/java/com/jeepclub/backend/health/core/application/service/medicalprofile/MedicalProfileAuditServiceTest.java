package com.jeepclub.backend.health.core.application.service.medicalprofile;

import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditEvent;
import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditOperation;
import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditOutcome;
import com.jeepclub.backend.health.core.application.command.UpsertMedicalProfileCommand;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileAccessDeniedException;
import com.jeepclub.backend.health.core.domain.enums.BloodType;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.core.port.DependentOwnershipChecker;
import com.jeepclub.backend.health.core.port.MedicalProfileAuditTrail;
import com.jeepclub.backend.health.core.port.MedicalProfileOwnerStatus;
import com.jeepclub.backend.health.core.port.MedicalProfileOwnerStatusChecker;
import com.jeepclub.backend.health.core.repository.MedicalProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalProfileAuditServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-07T15:00:00Z");
    private static final String SENSITIVE_VALUE = "ALERGIA-SECRETA-NAO-REGISTRAR";

    @Mock
    private MedicalProfileRepository repository;
    @Mock
    private DependentOwnershipChecker ownershipChecker;
    @Mock
    private MedicalProfileOwnerStatusChecker statusChecker;
    @Mock
    private MedicalProfileAuditTrail auditTrail;

    private MedicalProfileService memberService;
    private AdminMedicalProfileService adminService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        memberService = new MedicalProfileService(
                repository,
                ownershipChecker,
                statusChecker,
                auditTrail,
                clock
        );
        adminService = new AdminMedicalProfileService(
                repository,
                statusChecker,
                auditTrail,
                clock
        );
    }

    @Test
    void authorizedAdministrativeReadRecordsActorOwnerOperationAndTimestamp() {
        MedicalProfile profile = profile(MedicalProfileOwnerType.USER, 7L);
        when(statusChecker.getStatus(MedicalProfileOwnerType.USER, 7L))
                .thenReturn(MedicalProfileOwnerStatus.ACTIVE);
        when(repository.findByOwner(MedicalProfileOwnerType.USER, 7L))
                .thenReturn(Optional.of(profile));

        assertThat(adminService.getByOwner(
                MedicalProfileOwnerType.USER,
                7L,
                99L
        )).isSameAs(profile);

        verify(auditTrail).record(new MedicalProfileAuditEvent(
                99L,
                MedicalProfileOwnerType.USER,
                7L,
                MedicalProfileAuditOperation.READ,
                MedicalProfileAuditOutcome.SUCCEEDED,
                NOW
        ));
    }

    @Test
    void administrativeUpdateRecordsUpdateWithoutClinicalContent() {
        MedicalProfile profile = profile(MedicalProfileOwnerType.USER, 7L);
        when(statusChecker.getStatus(MedicalProfileOwnerType.USER, 7L))
                .thenReturn(MedicalProfileOwnerStatus.ACTIVE);
        when(repository.findByOwnerForUpdate(MedicalProfileOwnerType.USER, 7L))
                .thenReturn(Optional.of(profile));
        when(repository.save(profile)).thenReturn(profile);

        adminService.upsertByOwner(
                MedicalProfileOwnerType.USER,
                7L,
                command(SENSITIVE_VALUE),
                99L
        );

        ArgumentCaptor<MedicalProfileAuditEvent> captor = ArgumentCaptor.forClass(
                MedicalProfileAuditEvent.class
        );
        verify(auditTrail).record(captor.capture());
        assertThat(captor.getValue().operation())
                .isEqualTo(MedicalProfileAuditOperation.UPDATE);
        assertThat(captor.getValue().toString()).doesNotContain(SENSITIVE_VALUE);
    }

    @Test
    void deniedDependentMutationIsObservableWithoutLeakingRequestContent() {
        when(statusChecker.getStatus(MedicalProfileOwnerType.USER, 7L))
                .thenReturn(MedicalProfileOwnerStatus.ACTIVE);
        when(statusChecker.getStatus(MedicalProfileOwnerType.DEPENDENT, 11L))
                .thenReturn(MedicalProfileOwnerStatus.ACTIVE);
        when(ownershipChecker.belongsToUser(11L, 7L)).thenReturn(false);

        assertThatThrownBy(() -> memberService.upsertDependentMedicalProfile(
                7L,
                11L,
                command(SENSITIVE_VALUE)
        )).isInstanceOf(MedicalProfileAccessDeniedException.class)
                .hasMessageNotContaining(SENSITIVE_VALUE);

        verify(auditTrail).record(new MedicalProfileAuditEvent(
                7L,
                MedicalProfileOwnerType.DEPENDENT,
                11L,
                MedicalProfileAuditOperation.UPDATE,
                MedicalProfileAuditOutcome.DENIED,
                NOW
        ));
    }

    @Test
    void auditContractContainsOnlyGovernanceMetadata() {
        Set<String> fields = Stream.of(MedicalProfileAuditEvent.class.getRecordComponents())
                .map(component -> component.getName())
                .collect(Collectors.toSet());

        assertThat(fields).containsExactlyInAnyOrder(
                "actorUserId",
                "ownerType",
                "ownerId",
                "operation",
                "outcome",
                "occurredAt"
        );
    }

    private MedicalProfile profile(MedicalProfileOwnerType ownerType, Long ownerId) {
        return MedicalProfile.reconstitute(
                1L,
                ownerType,
                ownerId,
                BloodType.O_POSITIVE,
                "valor anterior",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                NOW.minusSeconds(60),
                NOW.minusSeconds(60)
        );
    }

    private UpsertMedicalProfileCommand command(String allergies) {
        return new UpsertMedicalProfileCommand(
                BloodType.O_POSITIVE,
                allergies,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
