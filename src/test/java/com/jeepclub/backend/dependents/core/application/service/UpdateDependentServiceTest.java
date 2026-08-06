package com.jeepclub.backend.dependents.core.application.service;

import com.jeepclub.backend.authentication.core.repository.UserRepository;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.exception.DependentException;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.core.repository.DependentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateDependentServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-30T12:00:00Z");

    @Mock
    private DependentRepository dependentRepository;
    @Mock
    private UserRepository userRepository;

    private UpdateDependentService service;

    @BeforeEach
    void setUp() {
        service = new UpdateDependentService(
                dependentRepository,
                userRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void titularUpdatesOwnDependentWithNormalizedData() {
        Dependent existing = DependentsFixture.dependent(10L, 1L);
        when(dependentRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByCpf("98765432100")).thenReturn(false);
        when(dependentRepository.existsByCpfAndIdNot("98765432100", 10L)).thenReturn(false);
        when(dependentRepository.save(existing)).thenReturn(existing);

        Dependent updated = service.update(
                10L,
                "Pedro Silva Ramos",
                "987.654.321-00",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "(11) 97777-6666",
                true,
                1L,
                false
        );

        assertThat(updated.getName()).isEqualTo("Pedro Silva Ramos");
        assertThat(updated.getCpf()).isEqualTo("98765432100");
        assertThat(updated.getPhoneNumber()).isEqualTo("11977776666");
        assertThat(updated.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void directorUpdatesAnyDependent() {
        Dependent existing = DependentsFixture.dependent(10L, 1L);
        when(dependentRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(dependentRepository.save(existing)).thenReturn(existing);

        Dependent updated = service.update(
                10L,
                "Pedro Silva",
                "12345678900",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "11988887777",
                true,
                99L,
                true
        );

        assertThat(updated.getSocioId()).isEqualTo(1L);
    }

    @Test
    void otherUserCannotUpdateDependent() {
        when(dependentRepository.findById(10L)).thenReturn(Optional.of(DependentsFixture.dependent(10L, 1L)));

        assertThatThrownBy(() -> service.update(
                10L,
                "Pedro Silva",
                "12345678900",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "11988887777",
                true,
                2L,
                false
        ))
                .isInstanceOf(DependentException.class)
                .hasMessage("Você não tem permissão para alterar os dados deste dependente.");
    }

    @Test
    void rejectsDuplicateCpfInUsersAndDependents() {
        when(dependentRepository.findById(10L)).thenReturn(Optional.of(DependentsFixture.dependent(10L, 1L)));
        when(userRepository.existsByCpf("98765432100")).thenReturn(true);

        assertThatThrownBy(() -> service.update(
                10L,
                "Pedro Silva",
                "98765432100",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "11988887777",
                true,
                1L,
                false
        ))
                .isInstanceOf(DependentException.class)
                .hasMessage("Já existe um sócio cadastrado com este CPF.");

        when(userRepository.existsByCpf("11122233344")).thenReturn(false);
        when(dependentRepository.existsByCpfAndIdNot("11122233344", 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(
                10L,
                "Pedro Silva",
                "11122233344",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "11988887777",
                true,
                1L,
                false
        ))
                .isInstanceOf(DependentException.class)
                .hasMessage("Já existe outro dependente cadastrado com este CPF.");
    }

    @Test
    void missingDependentReturnsNotFound() {
        when(dependentRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                10L,
                "Pedro Silva",
                "12345678900",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "11988887777",
                true,
                1L,
                false
        ))
                .isInstanceOf(DependentException.class)
                .hasMessage("Dependente não encontrado com o ID fornecido.");
    }
}
