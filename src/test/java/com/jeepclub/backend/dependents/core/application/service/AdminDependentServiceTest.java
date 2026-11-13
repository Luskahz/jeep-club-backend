package com.jeepclub.backend.dependents.core.application.service;

import com.jeepclub.backend.dependents.core.application.exception.DependentNotFoundException;
import com.jeepclub.backend.dependents.core.application.service.dependent.AdminDependentService;
import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.core.repository.DependentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDependentServiceTest {

    @Mock
    private DependentRepository dependentRepository;
    private AdminDependentService service;

    @BeforeEach
    void setUp() {
        service = new AdminDependentService(dependentRepository);
    }

    @Test
    void listsAndFindsDependentForTheSpecifiedSocio() {
        Dependent dependent = DependentsFixture.dependent(10L, 5L);
        when(dependentRepository.findAllByUserId(5L)).thenReturn(List.of(dependent));
        when(dependentRepository.findById(10L)).thenReturn(Optional.of(dependent));

        assertThat(service.findAllByUserId(5L)).hasSize(1);
        assertThat(service.findByUserIdAndId(5L, 10L).id()).isEqualTo(dependent.getId());
    }

    @Test
    void hidesDependentOfAnotherOwnerAsNotFound() {
        when(dependentRepository.findById(10L))
                .thenReturn(Optional.of(DependentsFixture.dependent(10L, 1L)));

        assertThatThrownBy(() -> service.findByUserIdAndId(5L, 10L))
                .isInstanceOf(DependentNotFoundException.class);
    }

    @Test
    void administrativeReadIncludesDisabledDependentAndRejectsMissingId() {
        Dependent dependent = DependentsFixture.dependent(10L, 5L,
                DependentStatus.INACTIVE);
        when(dependentRepository.findAllByUserId(5L)).thenReturn(List.of(dependent));
        when(dependentRepository.findById(10L)).thenReturn(Optional.of(dependent));
        assertThat(service.findAllByUserId(5L)).extracting(result -> result.status())
                .containsExactly(DependentStatus.INACTIVE);
        assertThat(service.findByUserIdAndId(5L, 10L).status()).isEqualTo(dependent.getStatus());
        assertThatThrownBy(() -> service.findByUserIdAndId(5L, 404L))
                .isInstanceOf(DependentNotFoundException.class);
    }

    @Test
    void searchesAndResolvesOwnerNamesWhenRequested() {
        var userPort = org.mockito.Mockito.mock(com.jeepclub.backend.dependents.core.port.DependentUserPort.class);
        var serviceWithUsers = new AdminDependentService(dependentRepository, userPort);
        var filter = new com.jeepclub.backend.dependents.core.application.query.AdminDependentFilter(
                " Pedro ", null, "529.982.247-25", 5L, null, DependentStatus.ACTIVE
        );
        var fields = java.util.EnumSet.of(
                com.jeepclub.backend.dependents.core.application.query.AdminDependentField.ID,
                com.jeepclub.backend.dependents.core.application.query.AdminDependentField.OWNER_NAME
        );
        var pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        var row = new com.jeepclub.backend.dependents.core.application.result.AdminDependentSearchResult(
                10L, null, null, null, null, null, 5L, null, null, null, null, null, null
        );
        when(dependentRepository.search(filter, fields, pageable))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(row), pageable, 1));
        when(userPort.findNamesByIds(List.of(5L))).thenReturn(java.util.Map.of(5L, "João Silva"));

        var page = serviceWithUsers.search(filter, fields, pageable);
        assertThat(page.getContent()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(10L);
            assertThat(item.ownerName()).isEqualTo("João Silva");
        });

        Dependent dependent = DependentsFixture.dependent(10L, 5L);
        when(dependentRepository.findById(10L)).thenReturn(Optional.of(dependent));
        assertThat(serviceWithUsers.findAdminById(10L).ownerName()).isEqualTo("João Silva");
    }
}
