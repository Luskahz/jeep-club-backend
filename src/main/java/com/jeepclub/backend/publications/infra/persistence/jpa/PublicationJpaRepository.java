package com.jeepclub.backend.publications.infra.persistence.jpa;

import com.jeepclub.backend.publications.infra.persistence.entity.PublicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PublicationJpaRepository extends JpaRepository<PublicationEntity, Long> {
    // Lock the root row first. Hibernate's follow-on locking of a polymorphic JOINED
    // select can fail when a concurrent delete removes the row while it waits.
    @Query(value = "select id from publications where id = :id for update", nativeQuery = true)
    Optional<Long> lockRootId(@Param("id") Long id);
}
