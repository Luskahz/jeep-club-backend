package com.jeepclub.backend.dependents.core.application.service;

import com.jeepclub.backend.dependents.core.application.result.DependentResult;
import com.jeepclub.backend.dependents.core.application.service.dependent.DependentService;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.exception.DependentException;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.core.port.DependentUserPort;
import com.jeepclub.backend.dependents.core.repository.DependentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DependentServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-30T12:00:00Z");

    @Mock
    private DependentRepository dependentRepository;
    @Mock
    private DependentUserPort dependentUserPort;
    @Mock
    private DependentMedicalProfilePort medicalProfilePort;

    private DependentService service;

    @BeforeEach
    void setUp() {
        service = new DependentService(
                dependentRepository,
                dependentUserPort,
                medicalProfilePort,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsDependentWithNormalizedDataAndMedicalProfile() {
        DependentMedicalProfileData profile = new DependentMedicalProfileData(
                "O+", "Dipirona", "Asma", "Aerolin", "Observação"
        );
        when(dependentUserPort.existsById(1L)).thenReturn(true);
        when(dependentRepository.save(any(Dependent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DependentResult result = service.create(
                "Pedro Silva", "123.456.789-00", LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD, "(11) 98888-7777", true, profile, 1L
        );

        ArgumentCaptor<Dependent> captor = ArgumentCaptor.forClass(Dependent.class);
        verify(dependentRepository).save(captor.capture());
        verify(medicalProfilePort).upsert(null, profile);
        assertThat(result.dependent().getCpf()).isEqualTo("12345678900");
        assertThat(result.dependent().getPhoneNumber()).isEqualTo("11988887777");
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsMissingSocioBeforeCheckingCpf() {
        when(dependentUserPort.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(
                "Pedro", "12345678900", LocalDate.now(), RelationshipType.CHILD,
                null, true, null, 99L
        )).isInstanceOf(DependentException.class);

        verifyNoInteractions(dependentRepository, medicalProfilePort);
    }

    @Test
    void rejectsCpfAlreadyUsedByUserOrDependent() {
        when(dependentUserPort.existsById(1L)).thenReturn(true);
        when(dependentUserPort.existsByCpf("12345678900")).thenReturn(true);

        assertThatThrownBy(() -> createWithCpf("12345678900"))
                .isInstanceOf(DependentException.class)
                .satisfies(exception -> assertThat(((DependentException) exception).getViolation())
                        .isEqualTo(DependentException.Violation.CONFLICT));

        when(dependentUserPort.existsByCpf("98765432100")).thenReturn(false);
        when(dependentRepository.existsByCpf("98765432100")).thenReturn(true);

        assertThatThrownBy(() -> createWithCpf("98765432100"))
                .isInstanceOf(DependentException.class)
                .satisfies(exception -> assertThat(((DependentException) exception).getViolation())
                        .isEqualTo(DependentException.Violation.CONFLICT));
    }

    @Test
    void findsOnlyOwnedDependentsAndListsCurrentSocio() {
        Dependent dependent = DependentsFixture.dependent(10L, 1L);
        when(dependentRepository.findById(10L)).thenReturn(Optional.of(dependent));
        when(dependentRepository.findAllBySocioId(1L)).thenReturn(List.of(dependent));

        assertThat(service.findById(10L, 1L).dependent()).isSameAs(dependent);
        assertThat(service.findAllByUserId(1L))
                .extracting(result -> result.dependent().getId())
                .containsExactly(10L);

        assertThatThrownBy(() -> service.findById(10L, 2L))
                .isInstanceOf(DependentException.class)
                .satisfies(exception -> assertThat(((DependentException) exception).getViolation())
                        .isEqualTo(DependentException.Violation.ACCESS_DENIED));
    }

    @Test
    void updatesOwnedDependentAndPreservesUniquenessRules() {
        Dependent dependent = DependentsFixture.dependent(10L, 1L);
        when(dependentRepository.findById(10L)).thenReturn(Optional.of(dependent));
        when(dependentRepository.save(dependent)).thenReturn(dependent);

        DependentResult result = service.update(
                10L, "Pedro Silva Ramos", "987.654.321-00", LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD, "(11) 97777-6666", true, null, 1L
        );

        assertThat(result.dependent().getName()).isEqualTo("Pedro Silva Ramos");
        assertThat(result.dependent().getCpf()).isEqualTo("98765432100");
        assertThat(result.dependent().getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsUpdateFromAnotherUserAndDuplicateCpf() {
        when(dependentRepository.findById(10L))
                .thenReturn(Optional.of(DependentsFixture.dependent(10L, 1L)));

        assertThatThrownBy(() -> update(2L, "12345678900"))
                .isInstanceOf(DependentException.class)
                .satisfies(exception -> assertThat(((DependentException) exception).getViolation())
                        .isEqualTo(DependentException.Violation.ACCESS_DENIED));

        when(dependentUserPort.existsByCpf("98765432100")).thenReturn(true);
        assertThatThrownBy(() -> update(1L, "98765432100"))
                .isInstanceOf(DependentException.class)
                .satisfies(exception -> assertThat(((DependentException) exception).getViolation())
                        .isEqualTo(DependentException.Violation.CONFLICT));
    }

    @Test
    void deletesOnlyOwnedDependent() {
        when(dependentRepository.findById(10L))
                .thenReturn(Optional.of(DependentsFixture.dependent(10L, 1L)));

        service.delete(10L, 1L);
        verify(dependentRepository).deleteById(10L);

        assertThatThrownBy(() -> service.delete(10L, 2L))
                .isInstanceOf(DependentException.class)
                .satisfies(exception -> assertThat(((DependentException) exception).getViolation())
                        .isEqualTo(DependentException.Violation.ACCESS_DENIED));
    }

    private DependentResult createWithCpf(String cpf) {
        return service.create(
                "Pedro", cpf, LocalDate.of(2010, 5, 20), RelationshipType.CHILD,
                null, true, null, 1L
        );
    }

    private DependentResult update(Long userId, String cpf) {
        return service.update(
                10L, "Pedro", cpf, LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD, null, true, null, userId
        );
    }
}
