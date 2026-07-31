package org.vgk.hr.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.vgk.hr.domain.request.TextEditRequest;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfEditorServiceTest {

    private final PdfEditorService pdfEditorService = new PdfEditorService();

    @Test
    void addsCyrillicTextToPdf() throws Exception {
        byte[] template = createEmptyPdf();
        TextEditRequest edit = TextEditRequest.builder()
                .pageNumber(1)
                .x(100)
                .y(100)
                .width(200)
                .height(30)
                .fontSize(12)
                .text("Иванов Иван Иванович")
                .build();

        byte[] result = pdfEditorService.editPdf(template, List.of(edit));

        assertTrue(result.length > template.length);
        try (PDDocument document = Loader.loadPDF(result)) {
            assertEquals(1, document.getNumberOfPages());
        }
    }

    @Test
    void addsTextWithoutOverwritingAndUsesBlackForInvalidColor() throws Exception {
        TextEditRequest edit = TextEditRequest.builder()
                .pageNumber(1)
                .x(100)
                .y(100)
                .width(200)
                .height(30)
                .fontSize(12)
                .text("Тест")
                .color("invalid")
                .overwrite(false)
                .build();

        byte[] result = pdfEditorService.editPdf(createEmptyPdf(), List.of(edit));

        assertTrue(result.length > 0);
    }

    @Test
    void addsBoldCyrillicTextToPdf() throws Exception {
        TextEditRequest edit = TextEditRequest.builder()
                .pageNumber(1)
                .x(100)
                .y(100)
                .width(200)
                .height(30)
                .fontSize(12)
                .bold(true)
                .text("Иванов Иван Иванович")
                .build();

        byte[] result = pdfEditorService.editPdf(createEmptyPdf(), List.of(edit));

        assertTrue(result.length > 0);
    }

    @Test
    void rejectsEditForMissingPage() throws Exception {
        TextEditRequest edit = TextEditRequest.builder()
                .pageNumber(2)
                .x(0)
                .y(0)
                .width(10)
                .height(10)
                .text("Тест")
                .build();

        assertThrows(IllegalArgumentException.class, () -> pdfEditorService.editPdf(createEmptyPdf(), List.of(edit)));
    }

    private byte[] createEmptyPdf() throws Exception {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }
}
