package com.jeepclub.backend.publications.infra.persistence.jpa;
import com.jeepclub.backend.publications.infra.persistence.entity.EventGuestRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EventGuestRequestJpaRepository extends JpaRepository<EventGuestRequestEntity, Long> {
    List<EventGuestRequestEntity> findByEventId(Long eventId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select g from EventGuestRequestEntity g where g.id in :ids order by g.id")
    List<EventGuestRequestEntity> findCurrentByIds(java.util.Collection<Long> ids);
    long countByCpfAndStatus(String cpf, String status);
    @org.springframework.data.jpa.repository.Query("select g.cpf, count(g) from EventGuestRequestEntity g where g.cpf in :cpfs and g.status = 'APPROVED' group by g.cpf")
    java.util.List<Object[]> approvedVisits(java.util.Collection<String> cpfs);
}
