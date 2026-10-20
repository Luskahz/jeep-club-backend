package com.jeepclub.backend.publications.infra.persistence.mapper;

import com.jeepclub.backend.publications.core.domain.model.*;
import com.jeepclub.backend.publications.core.domain.enums.*;
import java.time.*;
import java.util.List;
import org.springframework.stereotype.Component;

/** Maps locking read projections only. Persistence commands continue through JPA. */
@Component
public class CurrentEventMapper {
    public Event toDomain(Object[] row,List<Object[]> images) {
        var gallery=images.stream().map(i -> new PublicationImage((String)i[0],((Number)i[1]).intValue(),
            Boolean.TRUE.equals(i[2]) || i[2] instanceof Number n && n.intValue()!=0)).toList();
        return Event.reconstitute(((Number)row[0]).longValue(),((Number)row[1]).longValue(),(String)row[2],(String)row[3],
            PublicationStatus.valueOf(row[4].toString()),instant(row[5]),instant(row[6]),instant(row[7]),instant(row[8]),
            gallery,instant(row[9]),instant(row[10]),EventStatus.valueOf(row[11].toString()));
    }
    private static Instant instant(Object value) {
        if(value==null)return null;
        if(value instanceof Instant instant)return instant;
        if(value instanceof java.sql.Timestamp timestamp)return timestamp.toInstant();
        if(value instanceof LocalDateTime local)return local.toInstant(ZoneOffset.UTC);
        if(value instanceof OffsetDateTime offset)return offset.toInstant();
        throw new IllegalArgumentException("Unsupported database timestamp representation.");
    }
}
