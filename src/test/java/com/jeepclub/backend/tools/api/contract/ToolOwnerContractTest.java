package com.jeepclub.backend.tools.api.contract;

import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.tools.api.http.exception.ToolExceptionHandler;
import com.jeepclub.backend.tools.core.application.exception.ToolOwnerDisabledException;
import com.jeepclub.backend.tools.core.application.exception.ToolOwnerNotFoundException;
import com.jeepclub.backend.tools.infra.integration.identity.IdentityToolOwnerAdapter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ToolOwnerContractTest {

    @Test
    void adapterUsesOnlyPublicIdentityQuery() {
        UserQuery identity = mock(UserQuery.class);
        when(identity.existsById(7L)).thenReturn(true);
        when(identity.isAdministrativelyActive(7L)).thenReturn(false);
        IdentityToolOwnerAdapter adapter = new IdentityToolOwnerAdapter(identity);

        assertThat(adapter.existsById(7L)).isTrue();
        assertThat(adapter.isAdministrativelyActive(7L)).isFalse();
        verify(identity).existsById(7L);
        verify(identity).isAdministrativelyActive(7L);
    }

    @Test
    void invalidOwnerHasStableProblemStatusAndCode() {
        ToolExceptionHandler handler = new ToolExceptionHandler();

        var missing = handler.handleOwnerNotFound(new ToolOwnerNotFoundException());
        assertThat(missing.getStatusCode().value()).isEqualTo(404);
        assertThat(missing.getBody().getCode()).isEqualTo("TOOL_OWNER_NOT_FOUND");
        var disabled = handler.handleOwnerDisabled(new ToolOwnerDisabledException());
        assertThat(disabled.getStatusCode().value()).isEqualTo(409);
        assertThat(disabled.getBody().getCode()).isEqualTo("TOOL_OWNER_DISABLED");
    }
}
