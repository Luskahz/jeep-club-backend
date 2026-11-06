package com.jeepclub.backend.billing.core.application.service;

import com.jeepclub.backend.billing.core.application.query.BillingExportFilter;
import com.jeepclub.backend.billing.core.application.service.export.BillingExportService;
import com.jeepclub.backend.billing.core.repository.BillingExportQuery;
import com.jeepclub.backend.iam.authorization.api.module.RolePresentationQuery;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.publications.api.module.EventPresentationQuery;
import com.jeepclub.backend.shared.export.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static com.jeepclub.backend.billing.support.BillingFixtures.CLOCK;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BillingExportRoleVisibilityTest {
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
        var service = new BillingExportService(query, mock(UserQuery.class), roles, mock(EventPresentationQuery.class), renderer, CLOCK);
        service.export(BillingExportQuery.Product.DEFINITIONS, new BillingExportFilter(null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null), ExportFormat.CSV, false);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).cells().get(17)).isEmpty();
        assertThat(rows.get(0).cells().get(20)).isEmpty();
        verifyNoInteractions(roles);
    }

    @Test void exportsWithNoRoleIdsAreValidWithOrWithoutRoleReadPermission() {
        var filter = new BillingExportFilter(null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null);
        for (var product : List.of(BillingExportQuery.Product.DEFINITIONS, BillingExportQuery.Product.CYCLES,
                BillingExportQuery.Product.PAYMENTS, BillingExportQuery.Product.REFUNDS)) {
            for (boolean canReadRoles : List.of(false, true)) {
                var query = mock(BillingExportQuery.class);
                var roles = mock(RolePresentationQuery.class);
                var renderer = mock(ExportRenderer.class);
                int columns = switch (product) {
                    case DEFINITIONS -> 18;
                    case CYCLES -> 22;
                    case PAYMENTS -> 26;
                    case REFUNDS -> 32;
                    default -> throw new IllegalStateException();
                };
                when(query.read(eq(product), any(), eq(0))).thenReturn(List.of(
                        new BillingExportQuery.Entry(null, null, null, null, ExportRow.of(new Object[columns]))));
                var rows = new ArrayList<ExportRow>();
                when(renderer.render(any(), eq(ExportFormat.CSV))).thenAnswer(invocation -> {
                    invocation.<ExportDocument>getArgument(0).rows().read(rows::add);
                    return new ExportFile("test.csv", "text/csv", new byte[0]);
                });
                var service = new BillingExportService(query, mock(UserQuery.class), roles,
                        mock(EventPresentationQuery.class), renderer, CLOCK);
                service.export(product, filter, ExportFormat.CSV, canReadRoles);
                assertThat(rows).as(product + " (role permission=" + canReadRoles + ")").hasSize(1);
                verifyNoInteractions(roles);
            }
        }
    }

    @Test void authorizedDefinitionExportPreservesRoleIdAndResolvesName() {
        var query = mock(BillingExportQuery.class);
        var roles = mock(RolePresentationQuery.class);
        var renderer = mock(ExportRenderer.class);
        var cells = new Object[18]; cells[17] = 42L;
        when(query.read(eq(BillingExportQuery.Product.DEFINITIONS), any(), eq(0)))
                .thenReturn(List.of(new BillingExportQuery.Entry(null, 42L, null, null, ExportRow.of(cells))));
        when(roles.findByIds(List.of(42L))).thenReturn(List.of(new RolePresentationQuery.Details(42L, "Diretoria")));
        var rows = new ArrayList<ExportRow>();
        when(renderer.render(any(), eq(ExportFormat.CSV))).thenAnswer(invocation -> {
            invocation.<ExportDocument>getArgument(0).rows().read(rows::add);
            return new ExportFile("test.csv", "text/csv", new byte[0]);
        });
        var service = new BillingExportService(query, mock(UserQuery.class), roles,
                mock(EventPresentationQuery.class), renderer, CLOCK);
        service.export(BillingExportQuery.Product.DEFINITIONS,
                new BillingExportFilter(null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null),
                ExportFormat.CSV, true);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).cells().get(17)).isEqualTo("42");
        assertThat(rows.get(0).cells().get(20)).isEqualTo("Diretoria");
        verify(roles).findByIds(List.of(42L));
    }
}
