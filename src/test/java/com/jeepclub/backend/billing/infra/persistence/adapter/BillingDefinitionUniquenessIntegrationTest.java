package com.jeepclub.backend.billing.infra.persistence.adapter;

import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
import com.jeepclub.backend.billing.core.domain.model.ChargeDefinition;
import com.jeepclub.backend.billing.core.repository.ChargeDefinitionRepository;
import com.jeepclub.backend.billing.infra.persistence.mapper.ChargeDefinitionMapper;
import jakarta.persistence.EntityManager;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import static com.jeepclub.backend.billing.support.BillingFixtures.*;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({ChargeDefinitionAdapter.class, ChargeDefinitionMapper.class})
class BillingDefinitionUniquenessIntegrationTest {
    @Autowired ChargeDefinitionRepository definitions;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactionManager;

    private ChargeDefinition definition(String name) {
        return ChargeDefinition.create(name, null, AMOUNT, ChargeRecurrenceType.ONE_TIME,
                true, PaymentAcceptancePolicy.AFTER_DUE_DATE, null, NOW);
    }

    @Test void spellingSurvivesCreateAndUpdateAndQueriesIgnoreCase() {
        var saved = definitions.save(definition("MeNsAl"));
        em.clear();
        var restored = definitions.findById(saved.getId()).orElseThrow();
        assertThat(restored.getName()).isEqualTo("MeNsAl");
        assertThat(definitions.existsByName("mensal")).isTrue();
        restored.update("MENSAL", null, AMOUNT, ChargeRecurrenceType.ONE_TIME,
                true, PaymentAcceptancePolicy.AFTER_DUE_DATE, null, NOW.plusSeconds(1));
        definitions.save(restored);
        em.clear();
        assertThat(definitions.findById(saved.getId()).orElseThrow().getName()).isEqualTo("MENSAL");
    }

    @Test void databaseRejectsUpdateToAnotherDefinitionsNameIgnoringCase() {
        definitions.save(definition("Mensalidade"));
        var other = definitions.save(definition("Passeio"));
        other.update("MENSALIDADE", null, AMOUNT, ChargeRecurrenceType.ONE_TIME,
                true, PaymentAcceptancePolicy.AFTER_DUE_DATE, null, NOW.plusSeconds(1));
        assertThatThrownBy(() -> definitions.save(other))
                .isInstanceOf(com.jeepclub.backend.billing.core.application.exception.definition.ChargeDefinitionAlreadyExistsException.class);
    }

    @Test @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void databaseRejectsConcurrentCaseVariants() throws Exception {
        var tx = new TransactionTemplate(transactionManager);
        var pool = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var sequence = new java.util.concurrent.atomic.AtomicInteger();
        Callable<Boolean> write = () -> {
            String name = sequence.getAndIncrement() == 0 ? "CoNcUrReNt" : "cOnCuRrEnT";
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("start timed out");
            try { tx.executeWithoutResult(status -> definitions.save(definition(name))); return true; }
            catch (com.jeepclub.backend.billing.core.application.exception.definition.ChargeDefinitionAlreadyExistsException expected) { return false; }
        };
        try {
            var first = pool.submit(write); var second = pool.submit(write);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(java.util.List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        } finally {
            start.countDown(); pool.shutdownNow();
            tx.executeWithoutResult(status -> em.createQuery("delete from ChargeDefinitionEntity d where lower(d.name) = 'concurrent'").executeUpdate());
        }
    }
}
