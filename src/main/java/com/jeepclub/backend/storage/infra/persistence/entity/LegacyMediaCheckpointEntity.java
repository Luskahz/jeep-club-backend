package com.jeepclub.backend.storage.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="storage_legacy_checkpoints") @Getter @Setter
public class LegacyMediaCheckpointEntity {
    @Id @Column(name="source_id",length=64) private String sourceId;
    @Column(name="legacy_key",length=512,nullable=false) private String legacyKey;
    @Column(name="media_id") private Long mediaId;
    @Column(name="rolled_back",nullable=false) private boolean rolledBack;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="media_id",insertable=false,updatable=false,
        foreignKey=@ForeignKey(name="fk_storage_checkpoint_asset",options="ON DELETE RESTRICT ON UPDATE RESTRICT"))
    private MediaAssetEntity asset;
}
