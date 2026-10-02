package com.jeepclub.backend.publications.infra.persistence.jpa;
import com.jeepclub.backend.publications.infra.persistence.entity.EventGuestRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EventGuestRequestJpaRepository extends JpaRepository<EventGuestRequestEntity, Long> {
    List<EventGuestRequestEntity> findByEventId(Long eventId);
    long countByCpfAndStatus(String cpf, String status);
    @org.springframework.data.jpa.repository.Query("select g.cpf, count(g) from EventGuestRequestEntity g where g.cpf in :cpfs and g.status = 'APPROVED' group by g.cpf")
    java.util.List<Object[]> approvedVisits(java.util.Collection<String> cpfs);
}
