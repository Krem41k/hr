package org.vgk.hr.generation;

import org.junit.jupiter.api.Test;
import org.vgk.hr.db.entity.FieldValueSource;
import org.vgk.hr.db.entity.FieldValueType;
import org.vgk.hr.db.entity.PdfTemplate;
import org.vgk.hr.db.entity.TemplateField;
import org.vgk.hr.db.entity.WellKnownFieldCodes;
import org.vgk.hr.emailtemplate.EmailTemplateRenderer;
import org.vgk.hr.emailtemplate.EmailTemplateResponse;
import org.vgk.hr.emailtemplate.EmailTemplateService;
import org.vgk.hr.position.PositionResponse;
import org.vgk.hr.position.PositionService;
import org.vgk.hr.service.PdfEditorService;
import org.vgk.hr.template.DocumentTemplateService;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentPackServiceTest {

    private final PositionService positionService = mock(PositionService.class);
    private final DocumentTemplateService documentTemplateService = mock(DocumentTemplateService.class);
    private final EmailTemplateService emailTemplateService = mock(EmailTemplateService.class);
    private final PdfEditorService pdfEditorService = mock(PdfEditorService.class);

    private final DocumentPackService service = new DocumentPackService(
            positionService,
            documentTemplateService,
            emailTemplateService,
            new EmailTemplateRenderer(),
            new FieldValueResolver(),
            new DocumentPackZipWriter(),
            pdfEditorService
    );

    @Test
    void generatesZipWithAllTemplatesAndRendersEmail() throws Exception {
        givenActivePosition();
        when(documentTemplateService.activeTemplatesWithContent(1L)).thenReturn(List.of(
                template("Направление",
                        field(WellKnownFieldCodes.FULL_NAME, FieldValueType.TEXT, FieldValueSource.USER),
                        field(WellKnownFieldCodes.CURRENT_DATE, FieldValueType.DATE, FieldValueSource.SYSTEM)),
                template("Согласие",
                        field(WellKnownFieldCodes.FULL_NAME, FieldValueType.TEXT, FieldValueSource.USER),
                        field(WellKnownFieldCodes.BIRTH_DATE, FieldValueType.DATE, FieldValueSource.USER))
        ));
        when(emailTemplateService.find(1L)).thenReturn(Optional.of(new EmailTemplateResponse(
                5L, 1L, "Документы {{fullName}}", "ДР {{birthDate}}, дата {{currentDate}}"
        )));
        when(pdfEditorService.editPdf(any(), any())).thenReturn(new byte[]{9, 9});

        GeneratedDocumentPack pack = service.generate(1L, request(Map.of(
                "fullName", "Иванов Иван Иванович",
                "birthDate", "1990-05-15"
        )));

        assertEquals(List.of("01-Направление.pdf", "02-Согласие.pdf"), entryNames(pack.zip()));
        assertEquals("driver-documents.zip", pack.fileName());
        assertEquals("Документы Иванов Иван Иванович", pack.emailSubject());
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        assertEquals("ДР 15.05.1990, дата %s".formatted(today), pack.emailBody());
    }

    @Test
    void returnsEmptyEmailWhenPositionHasNoEmailTemplate() throws Exception {
        givenActivePosition();
        when(documentTemplateService.activeTemplatesWithContent(1L)).thenReturn(List.of(
                template("Направление", field(WellKnownFieldCodes.FULL_NAME, FieldValueType.TEXT, FieldValueSource.USER))
        ));
        when(emailTemplateService.find(1L)).thenReturn(Optional.empty());
        when(pdfEditorService.editPdf(any(), any())).thenReturn(new byte[]{1});

        GeneratedDocumentPack pack = service.generate(1L, request(Map.of("fullName", "Иванов")));

        assertEquals("", pack.emailSubject());
        assertEquals("", pack.emailBody());
        assertTrue(pack.zip().length > 0);
    }

    @Test
    void rejectsMissingAndBlankUserValues() {
        givenActivePosition();
        when(documentTemplateService.activeTemplatesWithContent(1L)).thenReturn(List.of(
                template("Направление",
                        field(WellKnownFieldCodes.FULL_NAME, FieldValueType.TEXT, FieldValueSource.USER),
                        field(WellKnownFieldCodes.BIRTH_DATE, FieldValueType.DATE, FieldValueSource.USER))
        ));

        Map<String, String> fields = new HashMap<>();
        fields.put("fullName", "   ");

        MissingFieldValuesException ex = assertThrows(MissingFieldValuesException.class,
                () -> service.generate(1L, request(fields)));

        assertTrue(ex.getMessage().contains("fullName"));
        assertTrue(ex.getMessage().contains("birthDate"));
    }

    @Test
    void rejectsUnknownFieldCodes() {
        givenActivePosition();
        when(documentTemplateService.activeTemplatesWithContent(1L)).thenReturn(List.of(
                template("Направление", field(WellKnownFieldCodes.FULL_NAME, FieldValueType.TEXT, FieldValueSource.USER))
        ));

        UnknownFieldCodesException ex = assertThrows(UnknownFieldCodesException.class,
                () -> service.generate(1L, request(Map.of("fullName", "Иванов", "salary", "100"))));

        assertTrue(ex.getMessage().contains("salary"));
    }

    @Test
    void rejectsPositionWithoutActiveTemplates() {
        givenActivePosition();
        when(documentTemplateService.activeTemplatesWithContent(1L)).thenReturn(List.of());

        assertThrows(EmptyDocumentPackException.class, () -> service.generate(1L, request(Map.of())));
    }

    @Test
    void rejectsInactivePosition() {
        when(positionService.get(1L)).thenReturn(new PositionResponse(1L, "Водитель", "driver", false));

        assertThrows(PositionInactiveException.class, () -> service.generate(1L, request(Map.of())));
    }

    @Test
    void treatsNullRequestBodyAsNoValues() {
        givenActivePosition();
        when(documentTemplateService.activeTemplatesWithContent(1L)).thenReturn(List.of(
                template("Направление", field(WellKnownFieldCodes.FULL_NAME, FieldValueType.TEXT, FieldValueSource.USER))
        ));

        assertThrows(MissingFieldValuesException.class, () -> service.generate(1L, null));
    }

    private void givenActivePosition() {
        when(positionService.get(1L)).thenReturn(new PositionResponse(1L, "Водитель", "driver", true));
    }

    private GenerateDocumentPackRequest request(Map<String, String> fields) {
        return new GenerateDocumentPackRequest(fields);
    }

    private PdfTemplate template(String name, TemplateField... fields) {
        PdfTemplate template = new PdfTemplate();
        template.setName(name);
        template.setContent(new byte[]{1, 2, 3});
        template.replaceFields(List.of(fields));
        return template;
    }

    private TemplateField field(String code, FieldValueType type, FieldValueSource source) {
        String dateFormat = type == FieldValueType.DATE ? "dd.MM.yyyy" : null;
        return new TemplateField(code, code, type, source, 1, 10, 20, 100, 20, 12, "DEJAVU_SANS", "#000", false, dateFormat);
    }

    private List<String> entryNames(byte[] zip) throws Exception {
        List<String> names = new ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        return names;
    }
}
