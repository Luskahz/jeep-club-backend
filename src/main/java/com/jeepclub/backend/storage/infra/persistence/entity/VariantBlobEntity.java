package com.jeepclub.backend.storage.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="storage_variant_blobs") @Getter @Setter
public class VariantBlobEntity {
    @Id @Column(name="variant_id") private Long variantId;
    @Column(name="sha256", nullable=false, length=64) private String sha256;
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="variant_id", insertable=false, updatable=false,
        foreignKey=@ForeignKey(name="fk_storage_binding_variant", options="ON DELETE RESTRICT ON UPDATE RESTRICT"))
    private MediaVariantEntity variant;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="sha256", insertable=false, updatable=false,
        foreignKey=@ForeignKey(name="fk_storage_binding_blob", options="ON DELETE RESTRICT ON UPDATE RESTRICT"))
    private StoredBlobEntity blob;
}
