package com.jeepclub.backend.dependents.core.application.service;

import com.jeepclub.backend.authentication.core.repository.UserRepository;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.exception.DependentException;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateDependentServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-30T12:00:00Z");

    @Mock
    private DependentRepository dependentRepository;
    @Mock
    private UserRepository userRepository;

    private CreateDependentService service;

    @BeforeEach
    void setUp() {
        service = new CreateDependentService(
                dependentRepository,
                userRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsDependentWhenSocioExistsAndCpfIsUnique() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsByCpf("12345678900")).thenReturn(false);
        when(dependentRepository.existsByCpf("12345678900")).thenReturn(false);
        when(dependentRepository.save(org.mockito.ArgumentMatchers.any(Dependent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Dependent created = service.create(
                "Pedro Silva",
                "123.456.789-00",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "(11) 98888-7777",
                true,
                1L
        );

        ArgumentCaptor<Dependent> captor = ArgumentCaptor.forClass(Dependent.class);
        verify(dependentRepository).save(captor.capture());

        assertThat(created.getCpf()).isEqualTo("12345678900");
        assertThat(created.getPhoneNumber()).isEqualTo("11988887777");
        assertThat(created.getSocioId()).isEqualTo(1L);
        assertThat(created.getConsentAcceptedAt()).isEqualTo(NOW);
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsMissingSocioBeforeCheckingCpf() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(
                "Pedro Silva",
                "12345678900",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "11988887777",
                true,
                99L
        ))
                .isInstanceOf(DependentException.class)
                .hasMessage("Sócio titular não encontrado com o ID fornecido.");

        verifyNoInteractions(dependentRepository);
    }

    @Test
    void rejectsCpfAlreadyUsedBySocio() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsByCpf("12345678900")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                "Pedro Silva",
                "12345678900",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "11988887777",
                true,
                1L
        ))
                .isInstanceOf(DependentException.class)
                .hasMessage("Já existe um sócio cadastrado com este CPF.");
    }

    @Test
    void rejectsCpfAlreadyUsedByDependent() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsByCpf("12345678900")).thenReturn(false);
        when(dependentRepository.existsByCpf("12345678900")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                "Pedro Silva",
                "12345678900",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "11988887777",
                true,
                1L
        ))
                .isInstanceOf(DependentException.class)
                .hasMessage("Já existe um dependente cadastrado com este CPF.");
    }
}
