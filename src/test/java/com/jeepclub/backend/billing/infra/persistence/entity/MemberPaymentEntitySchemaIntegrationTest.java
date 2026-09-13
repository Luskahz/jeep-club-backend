package com.jeepclub.backend.billing.infra.persistence.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class MemberPaymentEntitySchemaIntegrationTest {

    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void finalSchemaContainsStorageKeyAndNoReceiptUrlColumn() {
        List<String> columns = jdbcTemplate.queryForList(
                "select column_name from information_schema.columns where table_name = 'BILLING_MEMBER_PAYMENTS'",
                String.class
        );

        assertThat(columns).contains("RECEIPT_STORAGE_KEY").doesNotContain("RECEIPT_URL");
    }
}
