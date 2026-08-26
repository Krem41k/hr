package org.vgk.hr;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
class HrApplicationTests {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void contextLoads() {
    }

    @Test
    void flywayCreatesTemplateTables() {
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                        SELECT count(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name IN ('pdf_templates', 'template_field', 'positions', 'email_templates')
                        """,
                Integer.class
        );

        assertEquals(4, tableCount);
    }

    @Test
    void flywayAddsPositionColumnsToPdfTemplates() {
        Integer columnCount = jdbcTemplate.queryForObject(
                """
                        SELECT count(*)
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'pdf_templates'
                          AND column_name IN ('position_id', 'name', 'sort_order', 'deleted')
                        """,
                Integer.class
        );

        assertEquals(4, columnCount);
    }

    @Test
    void flywayAddsIdentifierToTemplateFields() {
        Integer columnCount = jdbcTemplate.queryForObject(
                """
                        SELECT count(*)
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'template_field'
                          AND column_name = 'id'
                        """,
                Integer.class
        );

        assertEquals(1, columnCount);
    }

    @Test
    void flywayAddsDeletedFlagToTemplateFields() {
        Integer columnCount = jdbcTemplate.queryForObject(
                """
                        SELECT count(*)
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'template_field'
                          AND column_name = 'deleted'
                        """,
                Integer.class
        );

        assertEquals(1, columnCount);
    }

    @Test
    void flywayAddsFieldCodeColumnsToTemplateFields() {
        Integer columnCount = jdbcTemplate.queryForObject(
                """
                        SELECT count(*)
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'template_field'
                          AND column_name IN ('field_code', 'label', 'value_type', 'value_source')
                        """,
                Integer.class
        );

        assertEquals(4, columnCount);
    }

}
