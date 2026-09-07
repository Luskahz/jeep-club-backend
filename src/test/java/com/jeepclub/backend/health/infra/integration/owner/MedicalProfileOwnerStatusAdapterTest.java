package com.jeepclub.backend.health.infra.integration.owner;

import com.jeepclub.backend.dependents.api.module.DependentsQuery;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.port.MedicalProfileOwnerStatus;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalProfileOwnerStatusAdapterTest {

    @Mock
    private UserQuery userQuery;
    @Mock
    private DependentsQuery dependentsQuery;

    private MedicalProfileOwnerStatusAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MedicalProfileOwnerStatusAdapter(userQuery, dependentsQuery);
    }

    @Test
    void classifiesUsersAsNotFoundInactiveOrActive() {
        when(userQuery.existsById(1L)).thenReturn(false);
        when(userQuery.existsById(2L)).thenReturn(true);
        when(userQuery.isAdministrativelyActive(2L)).thenReturn(false);
        when(userQuery.existsById(3L)).thenReturn(true);
        when(userQuery.isAdministrativelyActive(3L)).thenReturn(true);

        assertThat(adapter.getStatus(MedicalProfileOwnerType.USER, 1L))
                .isEqualTo(MedicalProfileOwnerStatus.NOT_FOUND);
        assertThat(adapter.getStatus(MedicalProfileOwnerType.USER, 2L))
                .isEqualTo(MedicalProfileOwnerStatus.INACTIVE);
        assertThat(adapter.getStatus(MedicalProfileOwnerType.USER, 3L))
                .isEqualTo(MedicalProfileOwnerStatus.ACTIVE);
    }

    @Test
    void classifiesDependentsAsNotFoundInactiveOrActive() {
        when(dependentsQuery.existsById(1L)).thenReturn(false);
        when(dependentsQuery.existsById(2L)).thenReturn(true);
        when(dependentsQuery.existsActiveById(2L)).thenReturn(false);
        when(dependentsQuery.existsById(3L)).thenReturn(true);
        when(dependentsQuery.existsActiveById(3L)).thenReturn(true);

        assertThat(adapter.getStatus(MedicalProfileOwnerType.DEPENDENT, 1L))
                .isEqualTo(MedicalProfileOwnerStatus.NOT_FOUND);
        assertThat(adapter.getStatus(MedicalProfileOwnerType.DEPENDENT, 2L))
                .isEqualTo(MedicalProfileOwnerStatus.INACTIVE);
        assertThat(adapter.getStatus(MedicalProfileOwnerType.DEPENDENT, 3L))
                .isEqualTo(MedicalProfileOwnerStatus.ACTIVE);
    }
}
