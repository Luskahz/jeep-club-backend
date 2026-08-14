package com.jeepclub.backend.dependents.infra.persistence.adapter;

import com.jeepclub.backend.authentication.core.domain.enums.AccountStatus;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationStatus;
import com.jeepclub.backend.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.authentication.infra.persistence.entity.UserEntity;
import com.jeepclub.backend.authentication.infra.persistence.jpa.UserJpaRepository;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.infra.persistence.entity.DependentEntity;
import com.jeepclub.backend.dependents.infra.persistence.jpa.DependentJpaRepository;
import com.jeepclub.backend.dependents.infra.persistence.mapper.DependentMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({DependentRepositoryAdapter.class, DependentMapper.class})
class DependentRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-06-30T12:00:00Z");

    @Autowired
    private DependentRepositoryAdapter repository;
    @Autowired
    private DependentJpaRepository dependentJpaRepository;
    @Autowired
    private UserJpaRepository userJpaRepository;
    @Autowired
    private DependentMapper dependentMapper;
    @Autowired
    private EntityManager entityManager;

    private UserEntity socio;

    @BeforeEach
    void setUp() {
        socio = userJpaRepository.save(user("Titular", "52998224725"));
    }

    @Test
    void savesFindsListsAndDeletesDependentWithoutMedicalColumns() {
        Dependent saved = repository.save(dependent("12345678900", socio.getId()));
        entityManager.flush();
        entityManager.clear();

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).get()
                .satisfies(found -> {
                    assertThat(found.getCpf()).isEqualTo("12345678900");
                    assertThat(found.getSocioId()).isEqualTo(socio.getId());
                    assertThat(found.getPhoneNumber()).isEqualTo("11988887777");
                });
        assertThat(repository.findAllBySocioId(socio.getId()))
                .extracting(Dependent::getCpf)
                .containsExactly("12345678900");
        assertThat(dependentJpaRepository.existsByIdAndSocioId(saved.getId(), socio.getId())).isTrue();
        assertThat(dependentJpaRepository.existsByIdAndSocioId(saved.getId(), 999L)).isFalse();

        repository.deleteById(saved.getId());
        entityManager.flush();

        assertThat(repository.findById(saved.getId())).isEmpty();
    }

    @Test
    void detectsCpfUniqueness() {
        Dependent saved = repository.save(dependent("12345678900", socio.getId()));
        entityManager.flush();

        assertThat(repository.existsByCpf("12345678900")).isTrue();
        assertThat(repository.existsByCpfAndIdNot("12345678900", saved.getId())).isFalse();
        assertThat(repository.existsByCpfAndIdNot("12345678900", saved.getId() + 1)).isTrue();
    }

    @Test
    void mapsDomainToEntityWithIdentityBirthDateAndConsentTimestamp() {
        Dependent domain = Dependent.reconstitute(
                42L,
                "Pedro Silva",
                "12345678900",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "11988887777",
                true,
                NOW,
                socio.getId(),
                NOW.minusSeconds(60),
                NOW
        );

        DependentEntity entity = dependentMapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(42L);
        assertThat(entity.getBirthDate()).isEqualTo(LocalDate.of(2010, 5, 20));
        assertThat(entity.getConsentAcceptedAt()).isEqualTo(NOW);
        assertThat(entity.getSocio().getId()).isEqualTo(socio.getId());
    }

    @Test
    void membershipDependentsSchemaDoesNotExposeLegacyMedicalColumns() {
        var columns = entityManager.createNativeQuery("""
                        SELECT COLUMN_NAME
                          FROM INFORMATION_SCHEMA.COLUMNS
                         WHERE TABLE_NAME = 'MEMBERSHIP_DEPENDENTS'
                        """)
                .getResultList();

        assertThat(columns)
                .extracting(Object::toString)
                .doesNotContain("BLOOD_TYPE", "ALLERGIES", "CHRONIC_DISEASES", "MEDICATIONS", "MEDICAL_NOTES");
    }

    private Dependent dependent(String cpf, Long socioId) {
        return Dependent.create(
                "Pedro Silva",
                cpf,
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "(11) 98888-7777",
                true,
                socioId,
                NOW
        );
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

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = {
            DependentJpaRepository.class,
            UserJpaRepository.class
    })
    @EntityScan(basePackageClasses = {
            DependentEntity.class,
            UserEntity.class
    })
    static class TestConfiguration {
    }
}
