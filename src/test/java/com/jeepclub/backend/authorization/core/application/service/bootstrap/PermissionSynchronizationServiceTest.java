package com.jeepclub.backend.authorization.core.application.service.bootstrap;

import com.jeepclub.backend.iam.authorization.core.application.service.bootstrap.PermissionSynchronizationService;
import com.jeepclub.backend.iam.authorization.core.domain.model.Permission;
import com.jeepclub.backend.iam.authorization.core.repository.PermissionRepository;
import com.jeepclub.backend.shared.authorization.PermissionCode;
import com.jeepclub.backend.shared.authorization.PermissionDefinition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermissionSynchronizationServiceTest {
    @Test
    void synchronizedCatalogDoesNotIssueRedundantSavesOrRewriteTimestamps() {
        var repository=mock(PermissionRepository.class);
        var original=Instant.parse("2026-08-13T12:00:00Z");
        var catalog=new java.util.EnumMap<PermissionCode,Permission>(PermissionCode.class);
        for(var definition:PermissionDefinition.values())catalog.put(definition.getCode(),Permission.fromDefinition(definition,original));
        when(repository.findByCode(any(PermissionCode.class))).thenAnswer(call -> Optional.of(catalog.get(call.getArgument(0))));
        var service=new PermissionSynchronizationService(repository,Clock.fixed(original.plusSeconds(60),ZoneOffset.UTC));
        service.synchronizePermissions();
        org.mockito.Mockito.verify(repository,org.mockito.Mockito.never()).save(any());
        assertThat(catalog.values()).allSatisfy(permission -> assertThat(permission.getUpdatedAt()).isEqualTo(original));
    }
    @Test
    void savesOnlyTheExistingPermissionWhoseSemanticCatalogMetadataChanged() {
        var repository=mock(PermissionRepository.class);
        var original=Instant.parse("2026-08-13T12:00:00Z");
        var synchronizedAt=original.plusSeconds(60);
        var catalog=new java.util.EnumMap<PermissionCode,Permission>(PermissionCode.class);
        for(var definition:PermissionDefinition.values())catalog.put(definition.getCode(),Permission.fromDefinition(definition,original));
        var definition=PermissionDefinition.AUTHORIZATION_ROLE_READ;
        var stale=Permission.reconstitute(5L,definition.getCode(),"Outdated description",definition.getModule(),original,original);
        catalog.put(definition.getCode(),stale);
        when(repository.findByCode(any(PermissionCode.class))).thenAnswer(call -> Optional.of(catalog.get(call.getArgument(0))));
        new PermissionSynchronizationService(repository,Clock.fixed(synchronizedAt,ZoneOffset.UTC)).synchronizePermissions();
        verify(repository).save(stale);
        assertThat(stale.getDescription()).isEqualTo(definition.getDescription());
        assertThat(stale.getUpdatedAt()).isEqualTo(synchronizedAt);
        org.mockito.Mockito.verify(repository,times(1)).save(any());
    }

    @Test
    void createsEveryCatalogPermissionWhenRepositoryIsEmpty() {
        PermissionRepository permissionRepository = mock(PermissionRepository.class);
        when(permissionRepository.findByCode(any(PermissionCode.class)))
                .thenReturn(Optional.empty());
        when(permissionRepository.save(any(Permission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Instant synchronizedAt = Instant.parse("2026-08-13T12:00:00Z");
        Clock clock = Clock.fixed(synchronizedAt, ZoneOffset.UTC);
        PermissionSynchronizationService service = new PermissionSynchronizationService(
                permissionRepository,
                clock
        );

        service.synchronizePermissions();

        ArgumentCaptor<Permission> permissionCaptor = ArgumentCaptor.forClass(Permission.class);
        verify(permissionRepository, times(PermissionDefinition.values().length))
                .save(permissionCaptor.capture());

        assertThat(permissionCaptor.getAllValues())
                .extracting(Permission::getCode)
                .containsExactlyInAnyOrder(PermissionCode.values());
        assertThat(permissionCaptor.getAllValues()).allSatisfy(permission -> {
            PermissionDefinition definition = PermissionDefinition.from(permission.getCode());
            assertThat(permission.getModule()).isEqualTo(definition.getModule());
            assertThat(permission.getDescription()).isEqualTo(definition.getDescription());
            assertThat(permission.getCreatedAt()).isEqualTo(synchronizedAt);
            assertThat(permission.getUpdatedAt()).isEqualTo(synchronizedAt);
        });
    }
}
