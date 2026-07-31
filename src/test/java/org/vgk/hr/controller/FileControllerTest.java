package org.vgk.hr.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.vgk.hr.db.entity.PdfTemplate;
import org.vgk.hr.db.entity.TemplateField;
import org.vgk.hr.db.entity.TemplateFieldType;
import org.vgk.hr.domain.request.TemplateUploadRequest;
import org.vgk.hr.service.PdfEditorService;
import org.vgk.hr.service.PdfTemplateService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileControllerTest {

    private final PdfEditorService pdfEditorService = mock(PdfEditorService.class);
    private final PdfTemplateService pdfTemplateService = mock(PdfTemplateService.class);
    private final FileController controller = new FileController(pdfEditorService, pdfTemplateService);

    @Test
    void uploadsTemplateFromOctetStreamMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "template.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                new byte[]{1, 2, 3}
        );
        byte[] metadata = """
                {
                  "templateNumber": 1,
                  "fields": [
                    {"type":"CURRENT_DATE","pageNumber":1,"x":10,"y":10,"width":10,"height":10,"fontSize":12,"fontName":"DEJAVU_SANS","color":"#000000","bold":false,"dateFormat":"dd.MM.yyyy"},
                    {"type":"BIRTH_DATE","pageNumber":1,"x":10,"y":30,"width":10,"height":10,"fontSize":12,"fontName":"DEJAVU_SANS","color":"#000000","bold":false,"dateFormat":"dd.MM.yyyy"},
                    {"type":"FULL_NAME","pageNumber":1,"x":10,"y":20,"width":10,"height":10,"fontSize":12,"fontName":"DEJAVU_SANS","color":"#000000","bold":true,"dateFormat":null}
                  ]
                }
                """.getBytes();

        var response = controller.uploadTemplate(file, metadata);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        var requestCaptor = org.mockito.ArgumentCaptor.forClass(TemplateUploadRequest.class);
        verify(pdfTemplateService).saveTemplate(org.mockito.ArgumentMatchers.same(file), requestCaptor.capture());
        assertEquals(1, requestCaptor.getValue().templateNumber());
        assertEquals(3, requestCaptor.getValue().fields().size());
    }

    @Test
    void generatesPdfWithAllTemplateFields() throws Exception {
        PdfTemplate template = templateWithFields(
                new TemplateField(TemplateFieldType.FULL_NAME, 1, 10, 20, 200, 20, 12, "DEJAVU_SANS", "#000000", true, null),
                new TemplateField(TemplateFieldType.CURRENT_DATE, 2, 30, 40, 200, 20, 12, "DEJAVU_SANS", "#000000", false, "dd.MM.yyyy"),
                new TemplateField(TemplateFieldType.BIRTH_DATE, 1, 50, 60, 200, 20, 12, "DEJAVU_SANS", "#000000", false, "dd.MM.yyyy")
        );
        byte[] generatedPdf = {4, 5, 6};
        LocalDate birthDate = LocalDate.of(1990, 5, 15);
        when(pdfTemplateService.getTemplate(1)).thenReturn(template);
        when(pdfEditorService.editPdf(any(), any())).thenReturn(generatedPdf);

        var response = controller.generateInvoice(1, "Иванов Иван Иванович", birthDate);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertArrayEquals(generatedPdf, response.getBody());
        var editsCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(pdfEditorService).editPdf(org.mockito.ArgumentMatchers.same(template.getContent()), editsCaptor.capture());
        assertEquals("Иванов Иван Иванович", ((List<?>) editsCaptor.getValue()).stream()
                .map(edit -> ((org.vgk.hr.domain.request.TextEditRequest) edit).getText())
                .findFirst()
                .orElseThrow());
        assertTrue(((List<?>) editsCaptor.getValue()).stream()
                .map(edit -> ((org.vgk.hr.domain.request.TextEditRequest) edit).getText())
                .anyMatch(LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))::equals));
        assertTrue(((List<?>) editsCaptor.getValue()).stream()
                .map(edit -> ((org.vgk.hr.domain.request.TextEditRequest) edit).getText())
                .anyMatch("15.05.1990"::equals));
        assertTrue(((List<?>) editsCaptor.getValue()).stream()
                .map(edit -> ((org.vgk.hr.domain.request.TextEditRequest) edit).getBold())
                .anyMatch(Boolean.TRUE::equals));
    }

    @Test
    void usesIsoDateWhenTemplateFieldHasNoFormat() throws Exception {
        PdfTemplate template = templateWithFields(
                new TemplateField(TemplateFieldType.CURRENT_DATE, 1, 10, 20, 200, 20, 12, "DEJAVU_SANS", "#000000", false, null),
                new TemplateField(TemplateFieldType.BIRTH_DATE, 1, 10, 35, 200, 20, 12, "DEJAVU_SANS", "#000000", false, null),
                new TemplateField(TemplateFieldType.FULL_NAME, 1, 10, 50, 200, 20, 12, "DEJAVU_SANS", "#000000", false, null)
        );
        LocalDate birthDate = LocalDate.of(1990, 5, 15);
        when(pdfTemplateService.getTemplate(2)).thenReturn(template);
        when(pdfEditorService.editPdf(any(), any())).thenReturn(new byte[]{1});

        controller.generateInvoice(2, "Иванов Иван Иванович", birthDate);

        var editsCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(pdfEditorService).editPdf(org.mockito.ArgumentMatchers.same(template.getContent()), editsCaptor.capture());
        assertTrue(((List<?>) editsCaptor.getValue()).stream()
                .map(edit -> ((org.vgk.hr.domain.request.TextEditRequest) edit).getText())
                .anyMatch(LocalDate.now().toString()::equals));
        assertTrue(((List<?>) editsCaptor.getValue()).stream()
                .map(edit -> ((org.vgk.hr.domain.request.TextEditRequest) edit).getText())
                .anyMatch("1990-05-15"::equals));
    }

    @Test
    void ignoresDeletedFieldsWhenGeneratingPdf() throws Exception {
        TemplateField activeName = new TemplateField(
                TemplateFieldType.FULL_NAME, 1, 10, 50, 200, 20, 12, "DEJAVU_SANS", "#000000", false, null
        );
        TemplateField deletedName = new TemplateField(
                TemplateFieldType.FULL_NAME, 1, 10, 70, 200, 20, 12, "DEJAVU_SANS", "#000000", false, null
        );
        deletedName.setDeleted(true);
        PdfTemplate template = templateWithFields(
                new TemplateField(TemplateFieldType.CURRENT_DATE, 1, 10, 20, 200, 20, 12, "DEJAVU_SANS", "#000000", false, null),
                new TemplateField(TemplateFieldType.BIRTH_DATE, 1, 10, 35, 200, 20, 12, "DEJAVU_SANS", "#000000", false, null),
                activeName,
                deletedName
        );
        when(pdfTemplateService.getTemplate(3)).thenReturn(template);
        when(pdfEditorService.editPdf(any(), any())).thenReturn(new byte[]{1});

        controller.generateInvoice(3, "Иванов Иван Иванович", LocalDate.of(1990, 5, 15));

        var editsCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(pdfEditorService).editPdf(org.mockito.ArgumentMatchers.same(template.getContent()), editsCaptor.capture());
        assertEquals(3, editsCaptor.getValue().size());
    }

    private PdfTemplate templateWithFields(TemplateField... fields) {
        PdfTemplate template = new PdfTemplate();
        template.setContent(new byte[]{1});
        template.setFields(List.of(fields));
        return template;
    }
}
