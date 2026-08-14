package com.jeepclub.backend.tools.core.application.service.tool;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.tools.core.domain.exception.ToolAccessDeniedException;
import com.jeepclub.backend.tools.core.domain.model.Tool;
import com.jeepclub.backend.tools.core.repository.ToolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolServiceTest {

    @Mock
    private ToolRepository toolRepository;

    private ToolService service;

    @BeforeEach
    void setUp() {
        service = new ToolService(toolRepository);
    }

    @Test
    void createsToolFromPrimitiveApplicationArguments() {
        when(toolRepository.save(any(Tool.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Tool tool = service.createTool("Macaco", "Hidráulico", 7L);

        assertThat(tool.getName()).isEqualTo("Macaco");
        assertThat(tool.getDescription()).isEqualTo("Hidráulico");
        assertThat(tool.getStatus()).isEqualTo(ToolStatus.ACTIVE);
        assertThat(tool.getUserId()).isEqualTo(7L);
    }

    @Test
    void listsAndFindsOwnedTools() {
        var pageable = PageRequest.of(0, 10);
        Tool tool = tool(7L);
        when(toolRepository.findByUserId(7L, pageable))
                .thenReturn(new PageImpl<>(List.of(tool)));
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));

        assertThat(service.listUserTools(7L, pageable).getContent()).containsExactly(tool);
        assertThat(service.getToolDetails(1L, 7L)).isSameAs(tool);
    }

    @Test
    void preservesOwnershipCheck() {
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool(7L)));

        assertThatThrownBy(() -> service.getToolDetails(1L, 8L))
                .isInstanceOf(ToolAccessDeniedException.class);
    }

    @Test
    void updatesStateAndSoftDeletesOwnedTool() {
        Tool tool = tool(7L);
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));
        when(toolRepository.save(tool)).thenReturn(tool);

        assertThat(service.updateTool(1L, "Novo", "Descrição", 7L).getName())
                .isEqualTo("Novo");
        assertThat(service.deactivateTool(1L, 7L).getStatus())
                .isEqualTo(ToolStatus.INACTIVE);
        assertThat(service.activateTool(1L, 7L).getStatus())
                .isEqualTo(ToolStatus.ACTIVE);

        service.deleteTool(1L, 7L);
        assertThat(tool.getStatus()).isEqualTo(ToolStatus.DELETED);
        verify(toolRepository, org.mockito.Mockito.atLeastOnce()).save(tool);
    }

    private Tool tool(Long userId) {
        LocalDateTime now = LocalDateTime.of(2026, 8, 13, 12, 0);
        return Tool.reconstitute(
                1L, "Macaco", "Hidráulico", ToolStatus.ACTIVE,
                userId, now, now, null
        );
    }
}
