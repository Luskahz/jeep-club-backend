package com.jeepclub.backend.storage.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="storage_media_control") @Getter @Setter
public class MediaControlEntity {
    @Id @Column(name="media_id") private Long mediaId;
    @Column(name="received_sha256",nullable=false,length=64) private String receivedSha256;
    @Column(name="state",nullable=false,length=24) private String state;
    @Column(name="retention_sensitive",nullable=false) private boolean sensitive;
    @Column(name="legal_hold",nullable=false) private boolean legalHold;
    @Column(name="discarded",nullable=false) private boolean discarded;
    @Version @Column(name="version",nullable=false) private Long version;
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="media_id",insertable=false,updatable=false,
        foreignKey=@ForeignKey(name="fk_storage_control_asset",options="ON DELETE RESTRICT ON UPDATE RESTRICT"))
    private MediaAssetEntity asset;
}
