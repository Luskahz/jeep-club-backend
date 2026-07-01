package com.jeepclub.backend.dependents.core.application.service;

import com.jeepclub.backend.dependents.core.domain.exception.DependentException;
import com.jeepclub.backend.dependents.core.repository.DependentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteDependentServiceTest {

    @Mock
    private DependentRepository dependentRepository;

    private DeleteDependentService service;

    @BeforeEach
    void setUp() {
        service = new DeleteDependentService(dependentRepository);
    }

    @Test
    void titularDeletesOwnDependent() {
        when(dependentRepository.findById(10L)).thenReturn(Optional.of(DependentsFixture.dependent(10L, 1L)));

        service.delete(10L, 1L, false);

        verify(dependentRepository).deleteById(10L);
    }

    @Test
    void directorDeletesAnyDependent() {
        when(dependentRepository.findById(10L)).thenReturn(Optional.of(DependentsFixture.dependent(10L, 1L)));

        service.delete(10L, 99L, true);

        verify(dependentRepository).deleteById(10L);
    }

    @Test
    void otherUserCannotDeleteDependent() {
        when(dependentRepository.findById(10L)).thenReturn(Optional.of(DependentsFixture.dependent(10L, 1L)));

        assertThatThrownBy(() -> service.delete(10L, 2L, false))
                .isInstanceOf(DependentException.class)
                .hasMessage("Você não tem permissão para remover este dependente.");

        verifyNoMoreInteractions(dependentRepository);
    }

    @Test
    void missingDependentReturnsNotFound() {
        when(dependentRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(10L, 1L, false))
                .isInstanceOf(DependentException.class)
                .hasMessage("Dependente não encontrado com o ID fornecido.");
    }
}
