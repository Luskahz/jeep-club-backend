package com.jeepclub.backend.publications.infra.persistence.jpa;

import com.jeepclub.backend.publications.infra.persistence.entity.PublicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PublicationJpaRepository extends JpaRepository<PublicationEntity, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<PublicationEntity> {
    @Query("select e from EventEntity e")
    org.springframework.data.domain.Page<PublicationEntity> findEvents(org.springframework.data.domain.Pageable pageable);
    // Lock the root row first. Hibernate's follow-on locking of a polymorphic JOINED
    // select can fail when a concurrent delete removes the row while it waits.
    @Query(value = "select id from publications where id = :id for update", nativeQuery = true)
    Optional<Long> lockRootId(@Param("id") Long id);
}
