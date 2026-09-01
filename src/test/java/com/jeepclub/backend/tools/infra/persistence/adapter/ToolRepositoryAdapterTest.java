package com.jeepclub.backend.tools.infra.persistence.adapter;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.tools.core.domain.exception.ToolAlreadyDeletedException;
import com.jeepclub.backend.tools.core.domain.model.Tool;
import com.jeepclub.backend.tools.infra.persistence.entity.ToolEntity;
import com.jeepclub.backend.tools.infra.persistence.entity.ToolHistoryEntity;
import com.jeepclub.backend.tools.infra.persistence.jpa.ToolHistoryJpaRepository;
import com.jeepclub.backend.tools.infra.persistence.jpa.ToolJpaRepository;
import com.jeepclub.backend.tools.infra.persistence.mapper.ToolHistoryMapper;
import com.jeepclub.backend.tools.infra.persistence.mapper.ToolMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = ToolRepositoryAdapterTest.JpaTestConfiguration.class)
@Import({
        ToolRepositoryAdapter.class,
        ToolMapper.class,
        ToolHistoryMapper.class
})
class ToolRepositoryAdapterTest {

    private static final LocalDateTime CREATED_AT =
            LocalDateTime.of(2026, 8, 31, 12, 0);
    private static final LocalDateTime DELETED_AT =
            CREATED_AT.plusMinutes(5);

    @Autowired
    private ToolRepositoryAdapter repository;
    @Autowired
    private ToolJpaRepository toolJpaRepository;
    @Autowired
    private ToolHistoryJpaRepository historyJpaRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void deleteSavesHistoryAndRemovesOperationalEntity() {
        Tool saved = repository.save(tool());
        entityManager.flush();

        repository.delete(saved, 99L, DELETED_AT);
        entityManager.flush();
        entityManager.clear();

        assertThat(toolJpaRepository.findById(saved.getId())).isEmpty();
        assertThat(historyJpaRepository.findAll()).singleElement()
                .satisfies(history -> {
                    assertThat(history.getId()).isNotNull();
                    assertThat(history.getToolId()).isEqualTo(saved.getId());
                    assertThat(history.getUserId()).isEqualTo(7L);
                    assertThat(history.getDeletedByUserId()).isEqualTo(99L);
                    assertThat(history.getDeletedAt()).isEqualTo(DELETED_AT);
                    assertThat(history.getStatus()).isEqualTo(ToolStatus.ACTIVE);
                });
    }

    @Test
    void historyToolIdIsUnique() {
        historyJpaRepository.saveAndFlush(history(42L));

        assertThatThrownBy(() -> historyJpaRepository.saveAndFlush(
                history(42L)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deleteRejectsAnAlreadyDeletedTool() {
        Tool saved = repository.save(tool());
        repository.delete(saved, 99L, DELETED_AT);
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> repository.delete(saved, 99L, DELETED_AT))
                .isInstanceOf(ToolAlreadyDeletedException.class);
    }

    private Tool tool() {
        return Tool.reconstitute(
                null,
                "Macaco hidráulico",
                "Duas toneladas",
                ToolStatus.ACTIVE,
                7L,
                CREATED_AT,
                CREATED_AT
        );
    }

    private ToolHistoryEntity history(Long toolId) {
        ToolHistoryEntity history = new ToolHistoryEntity();
        history.setToolId(toolId);
        history.setName("Macaco hidráulico");
        history.setDescription("Duas toneladas");
        history.setStatus(ToolStatus.ACTIVE);
        history.setUserId(7L);
        history.setDeletedByUserId(99L);
        history.setCreatedAt(CREATED_AT);
        history.setUpdatedAt(CREATED_AT);
        history.setDeletedAt(DELETED_AT);
        return history;
    }

    @TestConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = ToolJpaRepository.class)
    @EntityScan(basePackageClasses = ToolEntity.class)
    static class JpaTestConfiguration {
    }
}
