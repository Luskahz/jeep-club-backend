package com.jeepclub.backend.tools.core.application.service.tool;

import com.jeepclub.backend.tools.core.application.exception.ToolNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminToolServiceTest {
    private static final Instant INSTANT = Instant.parse("2026-09-14T12:05:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(INSTANT, ZoneOffset.UTC);
    @Mock ToolRepository repository;

    private AdminToolService service() {
        return new AdminToolService(repository, Clock.fixed(INSTANT, ZoneOffset.UTC));
    }

    @Test
    void listsFilteredToolsAndReadsAnyOwnersTool() {
        var pageable = PageRequest.of(0, 10);
        Tool otherOwnersTool = tool(8L, ToolStatus.INACTIVE);
        when(repository.findAll("macaco", ToolStatus.INACTIVE, pageable))
                .thenReturn(new PageImpl<>(List.of(otherOwnersTool)));
        when(repository.findById(1L)).thenReturn(Optional.of(otherOwnersTool));

        assertThat(service().listAllTools("macaco", ToolStatus.INACTIVE, pageable).getContent())
                .containsExactly(otherOwnersTool);
        assertThat(service().getToolDetails(1L)).isSameAs(otherOwnersTool);
    }

    @Test
    void missingToolIsRejectedBeforeAdministrativeMutation() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getToolDetails(1L))
                .isInstanceOf(ToolNotFoundException.class);
        assertThatThrownBy(() -> service().updateTool(1L, "Novo", null))
                .isInstanceOf(ToolNotFoundException.class);
        verify(repository, never()).save(any(Tool.class));
    }

    @Test
    void updatesAnotherOwnersToolAndPersistsChangedFields() {
        Tool tool = tool(8L, ToolStatus.ACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(tool));
        when(repository.save(tool)).thenReturn(tool);

        assertThat(service().updateTool(1L, "  Novo  ", "  Descrição  ")).isSameAs(tool);
        assertThat(tool.getName()).isEqualTo("Novo");
        assertThat(tool.getDescription()).isEqualTo("Descrição");
        assertThat(tool.getUpdatedAt()).isEqualTo(NOW);
        verify(repository).save(tool);
    }

    @Test
    void transitionsPersistOnlyChangesAndDeleteUsesAdministratorsId() {
        Tool tool = tool(8L, ToolStatus.ACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(tool));
        when(repository.save(tool)).thenReturn(tool);

        assertThat(service().activateTool(1L)).isSameAs(tool);
        verify(repository, never()).save(any(Tool.class));
        assertThat(service().deactivateTool(1L).getStatus()).isEqualTo(ToolStatus.INACTIVE);
        verify(repository).save(tool);
        assertThat(service().deactivateTool(1L)).isSameAs(tool);
        verify(repository).save(tool);
        assertThat(service().activateTool(1L).getStatus()).isEqualTo(ToolStatus.ACTIVE);
        verify(repository, org.mockito.Mockito.times(2)).save(tool);

        service().deleteTool(1L, 99L);
        verify(repository).delete(tool, 99L, NOW);
    }

    private Tool tool(Long ownerId, ToolStatus status) {
        return Tool.reconstitute(1L, "Macaco", "Original", status, ownerId,
                NOW.minusMinutes(5), NOW.minusMinutes(5));
    }
}
