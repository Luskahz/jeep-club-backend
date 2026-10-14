package com.jeepclub.backend.platform.startup;

import com.jeepclub.backend.platform.startup.security.DevelopmentSecurityBootstrapService;
import com.jeepclub.backend.iam.identity.api.module.*;
import com.jeepclub.backend.iam.identity.core.application.service.user.AdminUserService;
import com.jeepclub.backend.iam.identity.api.http.dto.admin.user.AdminUpdateUserRequestDTO;
import com.jeepclub.backend.iam.authorization.api.module.role.RoleQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import jakarta.validation.Validator;
import tools.jackson.databind.json.JsonMapper;
import java.time.Clock;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={
        "security.bootstrap.admin.name=Bootstrap Owner",
        "security.bootstrap.admin.cpf=52998224725",
        "security.bootstrap.admin.email=bootstrap@example.com",
        "security.bootstrap.admin.password=bootstrap-password",
        "security.bootstrap.admin.role-name=ROOT"
})
@ActiveProfiles("test")
class BootstrapRootIdentityStabilityTest {
    @Autowired DevelopmentSecurityBootstrapService bootstrap;
    @Autowired UserQuery users;
    @Autowired UserRegistration registration;
    @Autowired AdminUserService admin;
    @Autowired RoleQuery roles;
    @Autowired Validator validator;
    @Autowired JsonMapper json;
    @Autowired Clock clock;

    @Test void oldBootstrapCpfCannotTransferRootRoleAfterOwnerCorrectsCpf() throws Exception {
        bootstrap.bootstrap();
        Long ownerId=users.findByCpf("52998224725").orElseThrow().id();
        assertThat(roles.hasRootRole(ownerId)).isTrue();
        admin.update(ownerId,ownerId,AdminUpdateUserRequestDTO.read(json.readTree("{\"cpf\":\"11144477735\"}"),validator));
        Long ordinaryId=registration.createWithPermanentCredential(new UserRegistrationData("Ordinary",null,"ordinary@example.com","52998224725",null,null,null,clock.instant()),"ordinary-password");
        bootstrap.bootstrap();
        assertThat(roles.hasRootRole(ordinaryId)).isFalse();
        assertThat(roles.hasRootRole(ownerId)).isTrue();
        assertThat(users.findById(ownerId).orElseThrow().cpf()).isEqualTo("11144477735");
        assertThat(users.findAfterId(0,10)).hasSize(2);
    }
}
