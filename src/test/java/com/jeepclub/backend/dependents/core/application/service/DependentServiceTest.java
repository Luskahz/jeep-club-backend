package com.jeepclub.backend.dependents.core.application.service;

import com.jeepclub.backend.dependents.core.application.exception.DependentCpfAlreadyInUseException;
import com.jeepclub.backend.dependents.core.application.exception.DependentNotFoundException;
import com.jeepclub.backend.dependents.core.application.result.DependentResult;
import com.jeepclub.backend.dependents.core.application.service.dependent.DependentService;
import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DependentServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-30T12:00:00Z");

    @Mock
    private DependentRepository dependentRepository;
    @Mock
    private DependentUserPort dependentUserPort;

    private DependentService service;

    @BeforeEach
    void setUp() {
        service = new DependentService(
                dependentRepository,
                dependentUserPort,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createAcceptsAvailableCpf() {
        allowOwner();
        when(dependentRepository.save(any(Dependent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DependentResult result = create("123.456.789-00");

        assertThat(result.cpf()).isEqualTo("12345678900");
        assertThat(result.status()).isEqualTo(DependentStatus.ACTIVE);
    }

    @Test
    void createBlocksCpfUsedByActiveDependent() {
        assertCreateBlocksReservedCpf();
    }

    @Test
    void createBlocksCpfUsedByDisabledDependent() {
        assertCreateBlocksReservedCpf();
    }

    @Test
    void updateOfActiveDependentWorks() {
        Dependent dependent = DependentsFixture.dependent(10L, 1L);
        when(dependentRepository.findActiveById(10L)).thenReturn(Optional.of(dependent));
        when(dependentRepository.save(dependent)).thenReturn(dependent);

        DependentResult result = service.update(
                10L, "Pedro Ramos", "987.654.321-00",
                LocalDate.of(2010, 5, 20), RelationshipType.CHILD,
                "11977776666", 1L
        );

        assertThat(result.name()).isEqualTo("Pedro Ramos");
        assertThat(result.cpf()).isEqualTo("98765432100");
        assertThat(result.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void updateOfDisabledDependentIsBlocked() {
        when(dependentRepository.findActiveById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                10L, "Pedro Ramos", "98765432100",
                LocalDate.of(2010, 5, 20), RelationshipType.CHILD,
                null, 1L
        )).isInstanceOf(DependentNotFoundException.class);
    }

    @Test
    void disableChangesActiveToDisabled() {
        Dependent dependent = DependentsFixture.dependent(10L, 1L);
        when(dependentRepository.findById(10L)).thenReturn(Optional.of(dependent));
        when(dependentRepository.save(dependent)).thenReturn(dependent);

        assertThat(service.disable(10L, 1L).status())
                .isEqualTo(DependentStatus.DISABLED);
    }

    @Test
    void enableChangesDisabledToActive() {
        Dependent dependent = DependentsFixture.dependent(
                10L, 1L, DependentStatus.DISABLED
        );
        when(dependentRepository.findById(10L)).thenReturn(Optional.of(dependent));
        when(dependentRepository.save(dependent)).thenReturn(dependent);

        assertThat(service.enable(10L, 1L).status())
                .isEqualTo(DependentStatus.ACTIVE);
    }

    @Test
    void deleteOfActiveDependentDelegatesSnapshotData() {
        assertDeleteDelegates(DependentStatus.ACTIVE);
    }

    @Test
    void deleteOfDisabledDependentDelegatesSnapshotData() {
        assertDeleteDelegates(DependentStatus.DISABLED);
    }

    private void assertCreateBlocksReservedCpf() {
        allowOwner();
        when(dependentRepository.existsByCpf("12345678900")).thenReturn(true);

        assertThatThrownBy(() -> create("12345678900"))
                .isInstanceOf(DependentCpfAlreadyInUseException.class);
    }

    private void assertDeleteDelegates(DependentStatus status) {
        Dependent dependent = DependentsFixture.dependent(10L, 1L, status);
        when(dependentRepository.findById(10L)).thenReturn(Optional.of(dependent));

        service.delete(10L, 1L);

        ArgumentCaptor<Dependent> captor = ArgumentCaptor.forClass(Dependent.class);
        verify(dependentRepository).delete(captor.capture(), eq(1L), eq(NOW));
        assertThat(captor.getValue()).isSameAs(dependent);
    }

    private void allowOwner() {
        when(dependentUserPort.existsById(1L)).thenReturn(true);
        when(dependentUserPort.existsActiveById(1L)).thenReturn(true);
    }

    private DependentResult create(String cpf) {
        return service.create(
                "Pedro", cpf, LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD, null, 1L
        );
    }
}
