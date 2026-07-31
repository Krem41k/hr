package org.vgk.hr.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.vgk.hr.domain.request.TextEditRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class PdfEditorService {

    public byte[] editPdf(byte[] templateContent, List<TextEditRequest> edits) throws IOException {
        try (PDDocument document = Loader.loadPDF(templateContent)) {
            for (TextEditRequest edit : edits) {
                applyEdit(document, edit);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }

    private void applyEdit(PDDocument document, TextEditRequest edit) throws IOException {
        int pageIndex = edit.getPageNumber() - 1;
        if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
            throw new IllegalArgumentException("Page number out of range: " + edit.getPageNumber());
        }

        PDPage page = document.getPage(pageIndex);
        try (PDPageContentStream contentStream = new PDPageContentStream(
                document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

            if (Boolean.TRUE.equals(edit.getOverwrite())) {
                contentStream.setNonStrokingColor(1f, 1f, 1f);
                contentStream.addRect(
                        (float) edit.getX(),
                        (float) edit.getY(),
                        (float) edit.getWidth(),
                        (float) edit.getHeight()
                );
                contentStream.fill();
            }

            float[] rgb = parseColor(edit.getColor());
            contentStream.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);

            PDFont font = resolveFont(document, Boolean.TRUE.equals(edit.getBold()));
            contentStream.setFont(font, (float) edit.getFontSize());
            contentStream.beginText();
            contentStream.newLineAtOffset((float) edit.getX(), (float) edit.getY());
            contentStream.showText(edit.getText());
            contentStream.endText();
        }
    }

    private PDFont resolveFont(PDDocument document, boolean bold) throws IOException {
        String fontPath = bold
                ? "fonts/dejavu-sans-ttf-2.37/ttf/DejaVuSans-Bold.ttf"
                : "fonts/dejavu-sans-ttf-2.37/ttf/DejaVuSans.ttf";
        ClassPathResource fontResource = new ClassPathResource(fontPath);
        try (InputStream fontStream = fontResource.getInputStream()) {
            return PDType0Font.load(document, fontStream, true);
        }
    }

    private float[] parseColor(String color) {
        if (color == null || !color.startsWith("#") || color.length() != 7) {
            return new float[]{0f, 0f, 0f};
        }
        int r = Integer.parseInt(color.substring(1, 3), 16);
        int g = Integer.parseInt(color.substring(3, 5), 16);
        int b = Integer.parseInt(color.substring(5, 7), 16);
        return new float[]{r / 255f, g / 255f, b / 255f};
    }
}
