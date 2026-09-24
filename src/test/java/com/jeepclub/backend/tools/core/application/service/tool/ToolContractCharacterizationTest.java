package com.jeepclub.backend.tools.core.application.service.tool;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.tools.core.domain.model.Tool;
import com.jeepclub.backend.tools.core.repository.ToolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolContractCharacterizationTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 9, 14, 12, 0);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-14T12:05:00Z"), ZoneOffset.UTC);

    @Mock
    private ToolRepository repository;

    @Test
    void memberListingDoesNotFilterInactiveTools() {
        var pageable = PageRequest.of(0, 20);
        Tool active = tool(1L, ToolStatus.ACTIVE);
        Tool inactive = tool(2L, ToolStatus.INACTIVE);
        when(repository.findByUserId(7L, pageable)).thenReturn(new PageImpl<>(List.of(active, inactive)));

        ToolService service = new ToolService(repository, CLOCK);

        assertThat(service.listUserTools(7L, pageable).getContent())
                .extracting(Tool::getStatus)
                .containsExactly(ToolStatus.ACTIVE, ToolStatus.INACTIVE);
    }

    @Test
    void inactiveToolRemainsReadableEditableDeletableAndReactivatable() {
        Tool inactive = tool(1L, ToolStatus.INACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(inactive));
        when(repository.save(any(Tool.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ToolService service = new ToolService(repository, CLOCK);

        assertThat(service.getToolDetails(1L, 7L).getStatus()).isEqualTo(ToolStatus.INACTIVE);
        assertThat(service.updateTool(1L, "  Novo nome  ", null, 7L).getName()).isEqualTo("Novo nome");
        assertThat(service.activateTool(1L, 7L).getStatus()).isEqualTo(ToolStatus.ACTIVE);
        assertThat(service.deactivateTool(1L, 7L).getStatus()).isEqualTo(ToolStatus.INACTIVE);

        service.deleteTool(1L, 7L);

        verify(repository).delete(inactive, 7L, LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC));
    }

    @Test
    void updateTreatsNullAndBlankFieldsAccordingToCurrentPartialContract() {
        Tool tool = tool(1L, ToolStatus.ACTIVE);
        LocalDateTime updateTime = CREATED_AT.plusMinutes(1);

        tool.updateDetails(null, null, updateTime);
        assertThat(tool.getName()).isEqualTo("Macaco");
        assertThat(tool.getDescription()).isEqualTo("Hidráulico");

        tool.updateDetails("   ", "", updateTime.plusMinutes(1));
        assertThat(tool.getName()).isEqualTo("Macaco");
        assertThat(tool.getDescription()).isEmpty();

        tool.updateDetails("  Novo nome  ", null, updateTime.plusMinutes(2));
        assertThat(tool.getName()).isEqualTo("Novo nome");
        assertThat(tool.getDescription()).isEmpty();
    }

    @Test
    void idempotentTransitionsDoNotPersistUnchangedStatus() {
        Tool active = tool(1L, ToolStatus.ACTIVE);
        Tool inactive = tool(2L, ToolStatus.INACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(active));
        when(repository.findById(2L)).thenReturn(Optional.of(inactive));
        ToolService service = new ToolService(repository, CLOCK);

        assertThat(service.activateTool(1L, 7L)).isSameAs(active);
        assertThat(service.deactivateTool(2L, 7L)).isSameAs(inactive);

        verify(repository, never()).save(any(Tool.class));
    }

    @Test
    void transitionsReportWhetherStatusActuallyChanged() {
        Tool tool = tool(1L, ToolStatus.ACTIVE);
        LocalDateTime changedAt = CREATED_AT.plusMinutes(1);

        assertThat(tool.activate(changedAt)).isFalse();
        assertThat(tool.getUpdatedAt()).isEqualTo(CREATED_AT);
        assertThat(tool.deactivate(changedAt)).isTrue();
        assertThat(tool.getUpdatedAt()).isEqualTo(changedAt);
        assertThat(tool.deactivate(changedAt.plusMinutes(1))).isFalse();
        assertThat(tool.getUpdatedAt()).isEqualTo(changedAt);
        assertThat(tool.activate(changedAt.plusMinutes(2))).isTrue();
        assertThat(tool.getStatus()).isEqualTo(ToolStatus.ACTIVE);
    }

    @Test
    void administrativeCreationAcceptsTheSuppliedScalarUserIdWithoutIdentityValidation() {
        when(repository.save(any(Tool.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AdminToolService service = new AdminToolService(repository, CLOCK);

        Tool created = service.createToolForUser(999L, "Macaco", "Hidráulico");

        assertThat(created.getUserId()).isEqualTo(999L);
        assertThat(created.getStatus()).isEqualTo(ToolStatus.ACTIVE);
        verify(repository).save(created);
    }

    private Tool tool(Long id, ToolStatus status) {
        return Tool.reconstitute(id, "Macaco", "Hidráulico", status, 7L, CREATED_AT, CREATED_AT);
    }
}
