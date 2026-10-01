package com.jeepclub.backend.billing.core.application.service;

import com.jeepclub.backend.billing.core.application.service.export.BillingExportService;
import com.jeepclub.backend.billing.core.application.query.BillingExportFilter;
import com.jeepclub.backend.billing.core.repository.BillingExportQuery;
import com.jeepclub.backend.billing.core.repository.BillingExportQuery.Product;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.iam.authorization.api.module.RolePresentationQuery;
import com.jeepclub.backend.publications.api.module.EventPresentationQuery;
import com.jeepclub.backend.shared.export.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.time.LocalDate;
import static com.jeepclub.backend.billing.support.BillingFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BillingExportPreconditionsTest {
    @ParameterizedTest @EnumSource(Product.class)
    void nonexistentFinancialReferenceAndInvalidProductStatusFailBeforeRendering(Product product) {
        var query = mock(BillingExportQuery.class); var users = mock(UserQuery.class); var renderer = mock(ExportRenderer.class);
        var service = new BillingExportService(query, users, mock(RolePresentationQuery.class), mock(EventPresentationQuery.class), renderer, CLOCK);
        var invalid = new BillingExportFilter(null,null,null,null,null,null,null,null,"NOT_A_STATE",null,null,null,null,null,null,null);
        assertThatThrownBy(() -> service.export(product, invalid, ExportFormat.CSV)).isInstanceOf(ExportException.class);
        var missing = new BillingExportFilter(404L,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null);
        assertThatThrownBy(() -> service.export(product, missing, ExportFormat.CSV)).isInstanceOf(ExportException.class);
        verifyNoInteractions(renderer);
    }
    @ParameterizedTest @EnumSource(value=Product.class,names={"DEFINITIONS","CYCLES"})
    void configurationExportsRejectChargeStateFilters(Product product) {
        var renderer = mock(ExportRenderer.class);
        var service = new BillingExportService(mock(BillingExportQuery.class), mock(UserQuery.class), mock(RolePresentationQuery.class), mock(EventPresentationQuery.class), renderer, CLOCK);
        var filter = new BillingExportFilter(null,null,null,null,null,null,null,null,null,null,"PENDING",null,null,null,null,null);
        assertThatThrownBy(() -> service.export(product, filter, ExportFormat.CSV)).isInstanceOf(ExportException.class);
        verifyNoInteractions(renderer);
    }
}
