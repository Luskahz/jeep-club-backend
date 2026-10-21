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
    @Query(value="""
        select p.id,p.author_user_id,p.title,p.content,p.status,p.created_at,p.updated_at,p.published_at,p.archived_at,
            e.starts_at,e.ends_at,e.event_status
        from publications p join publication_events e on e.publication_id=p.id where p.id=:id for update
        """,nativeQuery=true)
    java.util.List<Object[]> currentEvent(@Param("id") Long id);
    @Query(value="select storage_key,position,is_primary from publication_images where publication_id=:id order by position for update",nativeQuery=true)
    java.util.List<Object[]> currentImages(@Param("id") Long id);
}
