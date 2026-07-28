package com.dbtraining.reconx.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class LiquibaseMigrationsIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void liquibase_applied_all_expected_changesets_and_seed_data() {
        Integer applied = jdbc.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog", Integer.class);
        assertThat(applied).isGreaterThanOrEqualTo(19);

        Integer counterparties = jdbc.queryForObject(
                "SELECT COUNT(*) FROM counterparties", Integer.class);
        assertThat(counterparties).isGreaterThanOrEqualTo(10);

        Integer instruments = jdbc.queryForObject(
                "SELECT COUNT(*) FROM instruments", Integer.class);
        assertThat(instruments).isGreaterThanOrEqualTo(15);

        Integer users = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE enabled = TRUE", Integer.class);
        assertThat(users).isGreaterThanOrEqualTo(4);

        Integer trades = jdbc.queryForObject(
                "SELECT COUNT(*) FROM trades WHERE deleted_at IS NULL", Integer.class);
        assertThat(trades).isGreaterThanOrEqualTo(10);
    }
}
