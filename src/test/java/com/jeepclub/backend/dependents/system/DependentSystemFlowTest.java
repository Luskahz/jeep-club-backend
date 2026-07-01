package com.jeepclub.backend.dependents.system;

import com.jeepclub.backend.authentication.core.domain.enums.AccountStatus;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationStatus;
import com.jeepclub.backend.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.authentication.infra.persistence.entity.UserEntity;
import com.jeepclub.backend.authentication.infra.persistence.jpa.UserJpaRepository;
import com.jeepclub.backend.dependents.core.application.service.CreateDependentService;
import com.jeepclub.backend.dependents.core.application.service.DeleteDependentService;
import com.jeepclub.backend.dependents.core.application.service.GetDependentService;
import com.jeepclub.backend.dependents.core.application.service.UpdateDependentService;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.infra.persistence.jpa.DependentJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DependentSystemFlowTest {

    private static final Instant NOW = Instant.parse("2026-06-30T12:00:00Z");

    @Autowired
    private CreateDependentService createDependentService;
    @Autowired
    private GetDependentService getDependentService;
    @Autowired
    private UpdateDependentService updateDependentService;
    @Autowired
    private DeleteDependentService deleteDependentService;
    @Autowired
    private UserJpaRepository userJpaRepository;
    @Autowired
    private DependentJpaRepository dependentJpaRepository;

    private UserEntity socio;

    @BeforeEach
    void setUp() {
        dependentJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
        socio = userJpaRepository.saveAndFlush(user("Titular Fluxo", "11122233344"));
    }

    @AfterEach
    void tearDown() {
        dependentJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
    }

    @Test
    void createConsultListUpdateAndRemoveDependent() {
        Dependent created = createDependentService.create(
                "Pedro Silva",
                "123.456.789-00",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "(11) 98888-7777",
                true,
                socio.getId()
        );

        assertThat(created.getId()).isNotNull();
        assertThat(created.getCpf()).isEqualTo("12345678900");

        Dependent found = getDependentService.getById(created.getId(), socio.getId(), false);
        assertThat(found.getName()).isEqualTo("Pedro Silva");

        assertThat(getDependentService.getBySocioId(socio.getId(), socio.getId(), false))
                .extracting(Dependent::getId)
                .containsExactly(created.getId());

        Dependent updated = updateDependentService.update(
                created.getId(),
                "Pedro Silva Ramos",
                "987.654.321-00",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "(11) 97777-6666",
                true,
                socio.getId(),
                false
        );

        assertThat(updated.getName()).isEqualTo("Pedro Silva Ramos");
        assertThat(updated.getCpf()).isEqualTo("98765432100");
        assertThat(updated.getPhoneNumber()).isEqualTo("11977776666");

        deleteDependentService.delete(created.getId(), socio.getId(), false);

        assertThat(dependentJpaRepository.findById(created.getId())).isEmpty();
    }

    private UserEntity user(String name, String cpf) {
        UserEntity user = new UserEntity();
        user.setName(name);
        user.setCpf(cpf);
        user.setPasswordHash("{noop}Senha@123");
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setAuthenticationStatus(AuthenticationStatus.ENABLED);
        user.setCredentialStatus(CredentialStatus.PERMANENT);
        user.setCreatedAt(NOW);
        return user;
    }
}
