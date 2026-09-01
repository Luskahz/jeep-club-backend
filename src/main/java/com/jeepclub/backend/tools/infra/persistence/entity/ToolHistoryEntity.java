package com.jeepclub.backend.tools.infra.persistence.entity;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "tools_tool_history",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tool_history_tool_id",
                        columnNames = "tool_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_tool_history_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_tool_history_deleted_at",
                        columnList = "deleted_at"
                )
        }
)
public class ToolHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "tool_id",
            nullable = false
    )
    private Long toolId;

    @Column(nullable = false)
    private String name;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ToolStatus status;

    @Column(
            name = "user_id",
            nullable = false
    )
    private Long userId;

    @Column(
            name = "deleted_by_user_id",
            nullable = false
    )
    private Long deletedByUserId;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(
            name = "deleted_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime deletedAt;
}
