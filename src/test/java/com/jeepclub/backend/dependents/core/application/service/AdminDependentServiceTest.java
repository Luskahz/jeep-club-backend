package com.jeepclub.backend.dependents.core.application.service;

import com.jeepclub.backend.dependents.core.application.service.dependent.AdminDependentService;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.core.port.DependentMedicalProfilePort;
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
class AdminDependentServiceTest {

    @Mock
    private DependentRepository dependentRepository;
    @Mock
    private DependentMedicalProfilePort medicalProfilePort;

    private AdminDependentService service;

    @BeforeEach
    void setUp() {
        service = new AdminDependentService(dependentRepository, medicalProfilePort);
    }

    @Test
    void listsAndFindsDependentForTheSpecifiedSocio() {
        Dependent dependent = DependentsFixture.dependent(10L, 5L);
        when(dependentRepository.findAllBySocioId(5L)).thenReturn(List.of(dependent));
        when(dependentRepository.findById(10L)).thenReturn(Optional.of(dependent));

        assertThat(service.findAllBySocioId(5L)).hasSize(1);
        assertThat(service.findBySocioIdAndId(5L, 10L).dependent()).isSameAs(dependent);
    }

    @Test
    void preservesBadRequestForMismatchedSocio() {
        when(dependentRepository.findById(10L))
                .thenReturn(Optional.of(DependentsFixture.dependent(10L, 1L)));

        assertThatThrownBy(() -> service.findBySocioIdAndId(5L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O dependente informado não pertence ao sócio especificado.");
    }
}
