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
    void adapterUsesPublicIdentityExistenceQueryAndCurrentEligibilityLock() {
        UserQuery identity = mock(UserQuery.class);
        when(identity.existsById(7L)).thenReturn(true);
        var identityLock=mock(com.jeepclub.backend.iam.identity.api.module.UserIdentityLock.class);
        when(identityLock.byId(7L)).thenReturn(java.util.Optional.of(new com.jeepclub.backend.iam.identity.api.module.UserDetails(
                7L,"Synthetic inactive",null,null,null,null,null,null,false,java.time.Instant.EPOCH,java.time.Instant.EPOCH,null)));
        IdentityToolOwnerAdapter adapter = new IdentityToolOwnerAdapter(identityLock, identity);

        assertThat(adapter.existsById(7L)).isTrue();
        assertThat(adapter.isAdministrativelyActive(7L)).isFalse();
        verify(identity).existsById(7L);
        verify(identityLock).byId(7L);
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
