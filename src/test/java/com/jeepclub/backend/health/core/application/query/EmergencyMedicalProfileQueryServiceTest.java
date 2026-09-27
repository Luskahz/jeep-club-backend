package com.jeepclub.backend.health.core.application.query;
import com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileOwner;
import com.jeepclub.backend.health.core.domain.enums.*;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.core.port.*;
import com.jeepclub.backend.health.core.repository.MedicalProfileRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class EmergencyMedicalProfileQueryServiceTest {
    @Test void inactiveOwnerNeverReadsClinicalData() {
        var repository = mock(MedicalProfileRepository.class); var owners = mock(MedicalProfileOwnerStatusChecker.class);
        when(owners.getStatus(MedicalProfileOwnerType.USER,1L)).thenReturn(MedicalProfileOwnerStatus.INACTIVE);
        assertThat(new EmergencyMedicalProfileQueryService(repository,owners).find(MedicalProfileOwner.USER,1L)).isEmpty();
        verifyNoInteractions(repository);
    }
    @Test void activeDependentUsesCorrectBoundaryAndMissingProfileIsEmpty() {
        var repository = mock(MedicalProfileRepository.class); var owners = mock(MedicalProfileOwnerStatusChecker.class);
        when(owners.getStatus(MedicalProfileOwnerType.DEPENDENT,2L)).thenReturn(MedicalProfileOwnerStatus.ACTIVE);
        when(repository.findByOwner(MedicalProfileOwnerType.DEPENDENT,2L)).thenReturn(Optional.empty());
        assertThat(new EmergencyMedicalProfileQueryService(repository,owners).find(MedicalProfileOwner.DEPENDENT,2L)).isEmpty();
        verify(repository).findByOwner(MedicalProfileOwnerType.DEPENDENT,2L);
    }
    @Test void activeProfileReturnsOnlyPublicClinicalResult() {
        var repository = mock(MedicalProfileRepository.class); var owners = mock(MedicalProfileOwnerStatusChecker.class);
        var profile = mock(MedicalProfile.class);
        when(profile.getBloodType()).thenReturn(BloodType.UNKNOWN);
        when(profile.getAllergies()).thenReturn("Example allergy");
        when(owners.getStatus(MedicalProfileOwnerType.USER,1L)).thenReturn(MedicalProfileOwnerStatus.ACTIVE);
        when(repository.findByOwner(MedicalProfileOwnerType.USER,1L)).thenReturn(Optional.of(profile));
        assertThat(new EmergencyMedicalProfileQueryService(repository,owners).find(MedicalProfileOwner.USER,1L).orElseThrow().allergies()).isEqualTo("Example allergy");
    }
}
