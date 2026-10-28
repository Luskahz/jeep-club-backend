package com.jeepclub.backend.identity.api.http.controller.admin;

import com.jeepclub.backend.iam.identity.api.http.controller.admin.AdminUserOptionsController;
import com.jeepclub.backend.iam.identity.core.application.result.admin.user.UserOptionResult;
import com.jeepclub.backend.iam.identity.core.application.service.user.UserOptionsService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminUserOptionsControllerTest {
    @Test
    void returnsMinimalPageAndDisablesCache() throws Exception {
        var service = mock(UserOptionsService.class);
        when(service.find("Maria", 0, 20)).thenReturn(new PageImpl<>(
                List.of(new UserOptionResult(7L, "Maria Silva")), PageRequest.of(0, 20), 1));
        var mvc = MockMvcBuilders.standaloneSetup(new AdminUserOptionsController(service)).build();
        mvc.perform(get("/identity/admin/users/options").param("name", "Maria"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.content[0].name").value("Maria Silva"))
                .andExpect(jsonPath("$.content[0].cpf").doesNotExist())
                .andExpect(jsonPath("$.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
