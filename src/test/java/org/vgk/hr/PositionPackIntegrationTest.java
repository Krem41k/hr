package org.vgk.hr;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.vgk.hr.db.entity.FieldValueSource;
import org.vgk.hr.db.entity.FieldValueType;
import org.vgk.hr.db.entity.WellKnownFieldCodes;
import org.vgk.hr.domain.request.TemplateFieldRequest;
import org.vgk.hr.emailtemplate.EmailTemplateRequest;
import org.vgk.hr.emailtemplate.EmailTemplateResponse;
import org.vgk.hr.emailtemplate.EmailTemplateService;
import org.vgk.hr.position.FormSchemaResponse;
import org.vgk.hr.position.PositionRequest;
import org.vgk.hr.position.PositionResponse;
import org.vgk.hr.position.PositionService;
import org.vgk.hr.template.DocumentTemplateResponse;
import org.vgk.hr.template.DocumentTemplateService;
import org.vgk.hr.template.DocumentTemplateUploadRequest;
import org.vgk.hr.template.FieldCodeConflictException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class PositionPackIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private PositionService positionService;
    @Autowired
    private DocumentTemplateService documentTemplateService;
    @Autowired
    private EmailTemplateService emailTemplateService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    @Transactional
    void positionWithTwoPdfsEmailAndFormSchema() throws Exception {
        PositionResponse position = positionService.create(new PositionRequest("Водитель", "driver-car", true));

        DocumentTemplateResponse first = documentTemplateService.create(
                position.id(),
                pdf("dir.pdf"),
                new DocumentTemplateUploadRequest(
                        "Направление",
                        0,
                        List.of(
                                field(WellKnownFieldCodes.FULL_NAME, "ФИО", FieldValueType.TEXT, FieldValueSource.USER),
                                field(WellKnownFieldCodes.CURRENT_DATE, "Дата", FieldValueType.DATE, FieldValueSource.SYSTEM)
                        )
                )
        );
        DocumentTemplateResponse second = documentTemplateService.create(
                position.id(),
                pdf("consent.pdf"),
                new DocumentTemplateUploadRequest(
                        "Согласие",
                        1,
                        List.of(
                                field(WellKnownFieldCodes.FULL_NAME, "ФИО", FieldValueType.TEXT, FieldValueSource.USER),
                                field(WellKnownFieldCodes.BIRTH_DATE, "Дата рождения", FieldValueType.DATE, FieldValueSource.USER)
                        )
                )
        );

        EmailTemplateResponse email = emailTemplateService.upsert(
                position.id(),
                new EmailTemplateRequest("Документы {{fullName}}", "Здравствуйте, {{fullName}}!")
        );

        FormSchemaResponse schema = positionService.formSchema(position.id());

        assertEquals(2, documentTemplateService.list(position.id()).size());
        assertEquals("Направление", first.name());
        assertEquals("Согласие", second.name());
        assertEquals("Документы {{fullName}}", email.subjectTemplate());
        assertEquals(2, schema.fields().size());
        assertTrue(schema.fields().stream().anyMatch(f -> WellKnownFieldCodes.FULL_NAME.equals(f.fieldCode())));
        assertTrue(schema.fields().stream().anyMatch(f -> WellKnownFieldCodes.BIRTH_DATE.equals(f.fieldCode())));
        assertTrue(schema.fields().stream().noneMatch(f -> WellKnownFieldCodes.CURRENT_DATE.equals(f.fieldCode())));

        Integer positionTables = jdbcTemplate.queryForObject(
                """
                        SELECT count(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name IN ('positions', 'email_templates')
                        """,
                Integer.class
        );
        assertEquals(2, positionTables);
    }

    @Test
    @Transactional
    void rejectsFieldCodeLabelCollisionOnSecondTemplate() throws Exception {
        PositionResponse position = positionService.create(new PositionRequest("Кассир", "cashier", true));
        documentTemplateService.create(
                position.id(),
                pdf("a.pdf"),
                new DocumentTemplateUploadRequest(
                        "A",
                        0,
                        List.of(field(WellKnownFieldCodes.FULL_NAME, "ФИО", FieldValueType.TEXT, FieldValueSource.USER))
                )
        );

        assertThrows(FieldCodeConflictException.class, () -> documentTemplateService.create(
                position.id(),
                pdf("b.pdf"),
                new DocumentTemplateUploadRequest(
                        "B",
                        1,
                        List.of(field(WellKnownFieldCodes.FULL_NAME, "Имя", FieldValueType.TEXT, FieldValueSource.USER))
                )
        ));
    }

    private MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, MediaType.APPLICATION_PDF_VALUE, new byte[]{1, 2, 3});
    }

    private TemplateFieldRequest field(String code, String label, FieldValueType type, FieldValueSource source) {
        return new TemplateFieldRequest(code, label, type, source, 1, 10, 20, 100, 20, 12, "DEJAVU_SANS", "#000", false, null);
    }
}
