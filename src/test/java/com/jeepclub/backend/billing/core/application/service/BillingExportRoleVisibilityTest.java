package com.jeepclub.backend.billing.core.application.service;

import com.jeepclub.backend.billing.core.application.query.BillingExportFilter;
import com.jeepclub.backend.billing.core.application.service.export.BillingExportService;
import com.jeepclub.backend.billing.core.repository.BillingExportQuery;
import com.jeepclub.backend.iam.authorization.api.module.RolePresentationQuery;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.publications.api.module.EventPresentationQuery;
import com.jeepclub.backend.shared.export.*;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import static com.jeepclub.backend.billing.support.BillingFixtures.CLOCK;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BillingExportRoleVisibilityTest {
    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test void hidesRoleIdAndNameFromDefinitionExportWithoutPermission() {
        var query = mock(BillingExportQuery.class);
        var roles = mock(RolePresentationQuery.class);
        var renderer = mock(ExportRenderer.class);
        var cells = new Object[18]; cells[17] = 42L;
        when(query.read(eq(BillingExportQuery.Product.DEFINITIONS), any(), eq(0)))
                .thenReturn(List.of(new BillingExportQuery.Entry(null, 42L, null, null, ExportRow.of(cells))));
        var rows = new ArrayList<ExportRow>();
        when(renderer.render(any(), eq(ExportFormat.CSV))).thenAnswer(invocation -> {
            invocation.<ExportDocument>getArgument(0).rows().read(rows::add);
            return new ExportFile("test.csv", "text/csv", new byte[0]);
        });
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "n/a", List.of(new SimpleGrantedAuthority("BILLING_EXPORT"))));
        var service = new BillingExportService(query, mock(UserQuery.class), roles, mock(EventPresentationQuery.class), renderer, CLOCK);
        service.export(BillingExportQuery.Product.DEFINITIONS, new BillingExportFilter(null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null), ExportFormat.CSV);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).cells().get(17)).isEmpty();
        assertThat(rows.get(0).cells().get(20)).isEmpty();
        verifyNoInteractions(roles);
    }
}
