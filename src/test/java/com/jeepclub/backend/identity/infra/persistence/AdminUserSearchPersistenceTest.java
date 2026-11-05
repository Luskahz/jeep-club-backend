package com.jeepclub.backend.identity.infra.persistence;

import com.jeepclub.backend.iam.identity.api.module.UserStatus;
import com.jeepclub.backend.iam.identity.core.application.query.user.*;
import com.jeepclub.backend.iam.identity.core.application.result.admin.user.AdminUserResult;
import com.jeepclub.backend.iam.identity.infra.persistence.entity.UserEntity;
import com.jeepclub.backend.iam.identity.infra.persistence.query.AdminUserJpaQueryRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.jeepclub.backend.identity.infra.persistence.AdminUserSearchPersistenceTest$Sql")
@ActiveProfiles("test")
@ContextConfiguration(classes = AdminUserSearchPersistenceTest.Config.class)
@Import(AdminUserJpaQueryRepository.class)
class AdminUserSearchPersistenceTest {
    @Autowired EntityManager em;
    @Autowired AdminUserJpaQueryRepository query;
    static final Set<AdminUserField> SUMMARY = Set.of(AdminUserField.ID, AdminUserField.NAME);

    @ParameterizedTest @ValueSource(strings = {"%", "_", "!", "\\", "!%_"})
    void partialFiltersAndQAreLiteral(String marker) {
        var match = user("Literal" + marker + "Name", "literal" + marker + "@example.test");
        user("LiteralXName", "literalx@example.test");
        for (var filter : List.of(filter(marker, null, null, null), filter(null, marker, null, null), filter(null, null, marker, null))) {
            var page = query.findAll(filter, SUMMARY, PageRequest.of(0, 1));
            assertThat(page.getContent()).extracting(r -> r.id()).containsExactly(match.getId());
            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getTotalPages()).isEqualTo(1);
        }
    }

    @Test void qOrSpecificAndAndCountApplyBeforeStablePagination() {
        var first = user("Repeated", "needle1@example.test");
        var second = user("Repeated", "needle2@example.test");
        user("Other", "needle3@example.test");
        var inactive = user("Repeated", "needle4@example.test");
        inactive.setStatus(UserStatus.INACTIVE);
        em.flush(); em.clear();
        var filter = filter("Repeated", null, "needle", UserStatus.ACTIVE);
        var a = query.findAll(filter, SUMMARY, PageRequest.of(0, 1, Sort.by("name")));
        var b = query.findAll(filter, SUMMARY, PageRequest.of(1, 1, Sort.by("name")));
        assertThat(a.getContent()).extracting(r -> r.id()).containsExactly(first.getId());
        assertThat(b.getContent()).extracting(r -> r.id()).containsExactly(second.getId());
        assertThat(b.getTotalElements()).isEqualTo(2);
        assertThat(b.getTotalPages()).isEqualTo(2);
        assertThat(query.findAll(filter, SUMMARY, PageRequest.of(2, 1))).isEmpty();
        assertThat(query.findAll(filter(null, null, "absent", null), SUMMARY, PageRequest.of(0, 2))).isEmpty();
    }

    @Test void projectionSelectsOnlyRequestedColumnsWithTwoQueriesRegardlessOfPageSize() {
        user("One", null); user("Two", null);
        em.flush(); em.clear(); Sql.statements.clear();
        var page = query.findAll(filter(null, null, "o", null), Set.of(AdminUserField.NAME), PageRequest.of(0, 50));
        assertThat(page.getContent()).allSatisfy(r -> {assertThat(r.id()).isNull(); assertThat(r.email()).isNull();});
        assertThat(Sql.statements).hasSize(2);
        assertThat(Sql.statements.get(0).substring(0, Sql.statements.get(0).indexOf(" from ")))
                .contains(".name").doesNotContain(".email", ".cpf", ".id");
        assertThat(Sql.statements.get(0)).contains("order by", "escape '!'");
    }

    @Test void missingMembershipMatchDoesNotSuppressTextualQOr() {
        var match = user("Textual", null);
        var filter = filter(null, null, "Textual", null).resolvedMembership(null, null);
        assertThat(query.findAll(filter, SUMMARY, PageRequest.of(0, 2)).getContent())
                .extracting(r -> r.id()).containsExactly(match.getId());
        var exact = new AdminUserFilter(null,null,null,null,null,null,null,null,null,null,null,null,null,"999",null,null);
        assertThat(query.findAll(exact, SUMMARY, PageRequest.of(0, 2))).isEmpty();
        var trimmedMembershipFilter = new com.jeepclub.backend.iam.identity.api.http.dto.admin.user.AdminUserFilterDTO(
                null, null, null, null, null, null, null, null, null, null, null, null, null, "  027  "
        ).toFilter().resolvedMembership(match.getId(), null);
        assertThat(trimmedMembershipFilter.membershipNumber()).isEqualTo("027");
        assertThat(query.findAll(trimmedMembershipFilter, SUMMARY, PageRequest.of(0, 2)).getContent())
                .extracting(AdminUserResult::id).containsExactly(match.getId());
        var projected = query.findAll(filter(null,null,null,null), Set.of(AdminUserField.MEMBERSHIP_NUMBER), PageRequest.of(0,2));
        assertThat(projected.getContent()).singleElement().satisfies(r -> {assertThat(r.id()).isEqualTo(match.getId());assertThat(r.name()).isNull();});
    }

    @Test void qMatchesFormattedCpfAndPhoneNumberAgainstDigitsOnlyColumns() {
        var cpfMatch = user("FormattedCpf", "cpf@example.test");
        cpfMatch.setCpf("52998224725");
        cpfMatch.setRg("112233445");
        cpfMatch.setPhoneNumber("5511987654321");

        var rgMatch = user("FormattedRg", "rg@example.test");
        rgMatch.setCpf("16899535009");
        rgMatch.setRg("123456789");
        rgMatch.setPhoneNumber("5511987659999");

        var numericNoise = user("NumericNoise", "noise@example.test");
        numericNoise.setCpf("99999123999");
        numericNoise.setRg("888123888");
        numericNoise.setPhoneNumber("5511900123000");

        var alphaNumericUser = user("maria123", "maria123@example.test");
        alphaNumericUser.setCpf("11144477735");
        alphaNumericUser.setRg("777666555");
        alphaNumericUser.setPhoneNumber("5521977776666");

        em.flush(); em.clear();

        var byFormattedCpf = query.findAll(filter(null, null, "529.982.247-25", null), SUMMARY, PageRequest.of(0, 5));
        assertThat(byFormattedCpf.getContent()).extracting(AdminUserResult::id).containsExactly(cpfMatch.getId());
        assertThat(byFormattedCpf.getTotalElements()).isEqualTo(1);

        var byFormattedRg = query.findAll(filter(null, null, "12.345.678-9", null), SUMMARY, PageRequest.of(0, 5));
        assertThat(byFormattedRg.getContent()).extracting(AdminUserResult::id).containsExactly(rgMatch.getId());
        assertThat(byFormattedRg.getTotalElements()).isEqualTo(1);

        var byFormattedPhone = query.findAll(filter(null, null, "+55 (11) 98765-4321", null), SUMMARY, PageRequest.of(0, 5));
        assertThat(byFormattedPhone.getContent()).extracting(AdminUserResult::id).containsExactly(cpfMatch.getId());
        assertThat(byFormattedPhone.getTotalElements()).isEqualTo(1);

        var byFormattedPhonePrefixPage0 = query.findAll(filter(null, null, "(11) 98765", null), SUMMARY, PageRequest.of(0, 1, Sort.by("name")));
        var byFormattedPhonePrefixPage1 = query.findAll(filter(null, null, "(11) 98765", null), SUMMARY, PageRequest.of(1, 1, Sort.by("name")));
        assertThat(byFormattedPhonePrefixPage0.getContent()).extracting(AdminUserResult::id).containsExactly(cpfMatch.getId());
        assertThat(byFormattedPhonePrefixPage1.getContent()).extracting(AdminUserResult::id).containsExactly(rgMatch.getId());
        assertThat(byFormattedPhonePrefixPage0.getTotalElements()).isEqualTo(2);
        assertThat(byFormattedPhonePrefixPage0.getTotalPages()).isEqualTo(2);

        var byAlphaNumeric = query.findAll(filter(null, null, "maria123", null), SUMMARY, PageRequest.of(0, 10));
        assertThat(byAlphaNumeric.getContent()).extracting(AdminUserResult::id).containsExactly(alphaNumericUser.getId());
        assertThat(byAlphaNumeric.getTotalElements()).isEqualTo(1);
    }

    private AdminUserFilter filter(String name, String email, String q, UserStatus status) {
        return new AdminUserFilter(null,name,null,email,null,null,null,status,null,null,null,null,q);
    }
    private UserEntity user(String name, String email) {
        var row = new UserEntity(); row.setName(name); row.setEmail(email);
        row.setCpf(String.format("%011d", em.createQuery("select count(u) from UserEntity u", Long.class).getSingleResult()+1));
        row.setStatus(UserStatus.ACTIVE); row.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        em.persist(row); return row;
    }
    public static class Sql implements StatementInspector {
        static final List<String> statements = new ArrayList<>();
        @Override public String inspect(String sql) { statements.add(sql); return sql; }
    }
    @SpringBootConfiguration @EnableAutoConfiguration @EntityScan(basePackageClasses = UserEntity.class)
    static class Config {}
}
