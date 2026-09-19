package com.jeepclub.backend.health.core.application.service.medicalprofile;

import com.jeepclub.backend.health.core.application.command.UpsertMedicalProfileCommand;
import com.jeepclub.backend.health.core.domain.enums.BloodType;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.core.port.DependentOwnershipChecker;
import com.jeepclub.backend.health.core.port.MedicalProfileOwnerStatus;
import com.jeepclub.backend.health.core.port.MedicalProfileOwnerStatusChecker;
import com.jeepclub.backend.health.core.repository.MedicalProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalProfileNormalizationConsistencyTest {

    private static final Instant NOW = Instant.parse("2026-09-07T18:00:00Z");

    @Mock
    private MedicalProfileRepository repository;
    @Mock
    private DependentOwnershipChecker ownershipChecker;
    @Mock
    private MedicalProfileOwnerStatusChecker statusChecker;

    private MedicalProfileService memberService;
    private AdminMedicalProfileService adminService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        memberService = new MedicalProfileService(
                repository, ownershipChecker, statusChecker, clock
        );
        adminService = new AdminMedicalProfileService(
                repository, statusChecker, (ownerType, ownerIds) -> java.util.Set.of(), clock
        );
        when(statusChecker.getStatus(MedicalProfileOwnerType.USER, 7L))
                .thenReturn(MedicalProfileOwnerStatus.ACTIVE);
        when(repository.save(any(MedicalProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void memberAndAdministrationCreateTheSameNormalizedData() {
        when(repository.findByOwnerForUpdate(MedicalProfileOwnerType.USER, 7L))
                .thenReturn(Optional.empty());
        UpsertMedicalProfileCommand command = command();

        MedicalProfile member = memberService.upsertMyMedicalProfile(7L, command);
        MedicalProfile admin = adminService.upsertByOwner(
                MedicalProfileOwnerType.USER, 7L, command
        );

        assertSameMedicalData(member, admin);
    }

    @Test
    void memberAndAdministrationUpdateTheSameNormalizedData() {
        MedicalProfile memberExisting = existing();
        MedicalProfile adminExisting = existing();
        when(repository.findByOwnerForUpdate(MedicalProfileOwnerType.USER, 7L))
                .thenReturn(Optional.of(memberExisting), Optional.of(adminExisting));
        UpsertMedicalProfileCommand command = command();

        MedicalProfile member = memberService.upsertMyMedicalProfile(7L, command);
        MedicalProfile admin = adminService.upsertByOwner(
                MedicalProfileOwnerType.USER, 7L, command
        );

        assertSameMedicalData(member, admin);
    }

    private void assertSameMedicalData(MedicalProfile member, MedicalProfile admin) {
        assertThat(member.getBloodType()).isEqualTo(admin.getBloodType());
        assertThat(member.getAllergies()).isEqualTo(admin.getAllergies())
                .isEqualTo("Dipirona");
        assertThat(member.getChronicConditions()).isEqualTo(admin.getChronicConditions());
        assertThat(member.getContinuousMedications()).isEqualTo(admin.getContinuousMedications());
        assertThat(member.getHealthInsuranceProvider()).isEqualTo(admin.getHealthInsuranceProvider());
        assertThat(member.getHealthInsurancePlan()).isEqualTo(admin.getHealthInsurancePlan());
        assertThat(member.getHealthInsuranceNumber()).isEqualTo(admin.getHealthInsuranceNumber());
        assertThat(member.getEmergencyContactName()).isEqualTo(admin.getEmergencyContactName());
        assertThat(member.getEmergencyContactPhone()).isEqualTo(admin.getEmergencyContactPhone())
                .isEqualTo("12999999999");
        assertThat(member.getEmergencyContactRelationship())
                .isEqualTo(admin.getEmergencyContactRelationship());
        assertThat(member.getObservations()).isEqualTo(admin.getObservations());
    }

    private UpsertMedicalProfileCommand command() {
        return new UpsertMedicalProfileCommand(
                null,
                "  Dipirona  ",
                "  Asma  ",
                "  Uso contínuo  ",
                "  Operadora  ",
                "  Plano  ",
                "  123  ",
                "  Maria  ",
                "  (12) 99999-9999  ",
                "  Mãe  ",
                "  Observação  "
        );
    }

    private MedicalProfile existing() {
        return MedicalProfile.reconstitute(
                1L,
                MedicalProfileOwnerType.USER,
                7L,
                BloodType.A_POSITIVE,
                null, null, null, null, null,
                null, null, null, null, null,
                NOW.minusSeconds(120),
                NOW.minusSeconds(60)
        );
    }
}
