package com.jeepclub.backend.publications.api.http.dto;
import com.jeepclub.backend.platform.web.pagination.PageResponse;
import org.springframework.data.domain.*;
import java.util.List;
/** Stable page envelope for bounded event operational collections, already materialized for eligibility. */
public final class EventPages {
    private EventPages() {}
    public static <T> PageResponse<T> of(List<T> rows, Pageable page) {
        if (page.isUnpaged()) return PageResponse.from(new PageImpl<>(rows));
        int start = (int) Math.min(page.getOffset(), rows.size());
        int end = Math.min(start + page.getPageSize(), rows.size());
        return PageResponse.from(new PageImpl<>(rows.subList(start, end), page, rows.size()));
    }
}
