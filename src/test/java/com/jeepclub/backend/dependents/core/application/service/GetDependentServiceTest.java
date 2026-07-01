package com.jeepclub.backend.dependents.core.application.service;

import com.jeepclub.backend.dependents.core.domain.exception.DependentException;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.core.repository.DependentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetDependentServiceTest {

    @Mock
    private DependentRepository dependentRepository;

    private GetDependentService service;

    @BeforeEach
    void setUp() {
        service = new GetDependentService(dependentRepository);
    }

    @Test
    void titularCanGetOwnDependent() {
        Dependent dependent = DependentsFixture.dependent(10L, 1L);
        when(dependentRepository.findById(10L)).thenReturn(Optional.of(dependent));

        assertThat(service.getById(10L, 1L, false)).isSameAs(dependent);
    }

    @Test
    void directorCanGetAnyDependent() {
        Dependent dependent = DependentsFixture.dependent(10L, 1L);
        when(dependentRepository.findById(10L)).thenReturn(Optional.of(dependent));

        assertThat(service.getById(10L, 99L, true)).isSameAs(dependent);
    }

    @Test
    void otherUserCannotGetDependent() {
        when(dependentRepository.findById(10L)).thenReturn(Optional.of(DependentsFixture.dependent(10L, 1L)));

        assertThatThrownBy(() -> service.getById(10L, 2L, false))
                .isInstanceOf(DependentException.class)
                .hasMessage("Você não tem permissão para visualizar este dependente.");
    }

    @Test
    void missingDependentReturnsNotFound() {
        when(dependentRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(10L, 1L, false))
                .isInstanceOf(DependentException.class)
                .hasMessage("Dependente não encontrado com o ID fornecido.");
    }

    @Test
    void titularCanListOwnDependentsAndDirectorCanListAnySocio() {
        List<Dependent> dependents = List.of(DependentsFixture.dependent(10L, 1L));
        when(dependentRepository.findAllBySocioId(1L)).thenReturn(dependents);
        when(dependentRepository.findAllBySocioId(2L)).thenReturn(List.of());

        assertThat(service.getBySocioId(1L, 1L, false)).isEqualTo(dependents);
        assertThat(service.getBySocioId(2L, null, true)).isEmpty();
    }

    @Test
    void otherUserCannotListDependents() {
        assertThatThrownBy(() -> service.getBySocioId(1L, 2L, false))
                .isInstanceOf(DependentException.class)
                .hasMessage("Você não tem permissão para listar dependentes de outro sócio.");
    }
}
