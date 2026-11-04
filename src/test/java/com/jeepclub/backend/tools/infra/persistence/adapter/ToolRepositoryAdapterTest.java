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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties="spring.jpa.properties.hibernate.session_factory.statement_inspector=com.jeepclub.backend.tools.infra.persistence.adapter.ToolRepositoryAdapterTest$Sql")
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
    void administrativeSearchFiltersBeforeCountAndProjectsOnlyRequestedColumns() {
        var first = repository.save(tool());
        var second = repository.save(tool());
        repository.save(Tool.create("Different", "Macaco only in description", 8L, CREATED_AT));
        entityManager.flush(); entityManager.clear();
        var filter = new com.jeepclub.backend.tools.core.application.query.AdminToolFilter("  MACACO ", "mac", ToolStatus.ACTIVE, 7L);
        var fields = java.util.Set.of(com.jeepclub.backend.tools.core.application.query.AdminToolField.NAME);
        Sql.statements.clear();
        var page = repository.search(filter, fields, PageRequest.of(1,1,org.springframework.data.domain.Sort.by("name")));
        assertThat(Sql.statements).hasSize(2);
        assertThat(Sql.statements.get(0).substring(0,Sql.statements.get(0).indexOf(" from ")))
                .contains(".name").doesNotContain(".description", ".user_id", ".id");
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).singleElement().satisfies(r -> {assertThat(r.name()).isEqualTo(first.getName());assertThat(r.id()).isNull();assertThat(r.userId()).isNull();});
        var all = java.util.EnumSet.allOf(com.jeepclub.backend.tools.core.application.query.AdminToolField.class);
        assertThat(repository.search(filter,all,PageRequest.of(1,1,org.springframework.data.domain.Sort.by("name"))).getContent())
                .extracting(r -> r.id()).containsExactly(second.getId());
        assertThat(repository.search(filter,all,PageRequest.of(2,1))).isEmpty();
        assertThatThrownBy(() -> repository.search(filter,all,PageRequest.of(0,1,org.springframework.data.domain.Sort.by("ownerName"))))
                .isInstanceOf(com.jeepclub.backend.tools.core.application.exception.InvalidToolSortException.class);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"%","_","!","!%_","\\"})
    void administrativeNameAndQEscapeLiteralCharacters(String marker) {
        var match = repository.save(Tool.create("Jack"+marker+"stand",null,7L,CREATED_AT));
        repository.save(Tool.create("JackXstand",null,7L,CREATED_AT));
        for (var filter : java.util.List.of(
                new com.jeepclub.backend.tools.core.application.query.AdminToolFilter(marker,null,null,null),
                new com.jeepclub.backend.tools.core.application.query.AdminToolFilter(null,marker,null,null))) {
            var page = repository.search(filter,java.util.EnumSet.allOf(com.jeepclub.backend.tools.core.application.query.AdminToolField.class),PageRequest.of(0,10));
            assertThat(page.getContent()).extracting(r -> r.id()).containsExactly(match.getId());
            assertThat(page.getTotalElements()).isEqualTo(1);
        }
    }

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

    @Test
    void memberListingIsOwnerScopedAndAdminFiltersCombineNameAndStatus() {
        repository.save(tool());
        Tool second = Tool.reconstitute(null, "Chave de roda", "Reserva", ToolStatus.INACTIVE,
                7L, CREATED_AT, CREATED_AT);
        repository.save(second);
        repository.save(Tool.reconstitute(null, "Chave alheia", "Outro dono", ToolStatus.INACTIVE,
                8L, CREATED_AT, CREATED_AT));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findByUserId(7L, PageRequest.of(0, 10)).getContent())
                .extracting(Tool::getName).containsExactlyInAnyOrder("Macaco hidráulico", "Chave de roda");
        assertThat(repository.findByUserId(8L, PageRequest.of(0, 10)).getContent())
                .extracting(Tool::getName).containsExactly("Chave alheia");
        assertThat(repository.findAll("cHaVe", ToolStatus.INACTIVE, PageRequest.of(0, 10)).getContent())
                .extracting(Tool::getName).containsExactlyInAnyOrder("Chave de roda", "Chave alheia");
        assertThat(repository.findAll("chave", ToolStatus.ACTIVE, PageRequest.of(0, 10)).getContent())
                .isEmpty();
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

    public static class Sql implements org.hibernate.resource.jdbc.spi.StatementInspector {
        static final java.util.List<String> statements = new java.util.ArrayList<>();
        @Override public String inspect(String sql) {statements.add(sql);return sql;}
    }
}
