package org.vgk.hr.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.vgk.hr.db.entity.PdfTemplate;
import org.vgk.hr.db.entity.TemplateField;
import org.vgk.hr.db.entity.TemplateFieldType;
import org.vgk.hr.db.repository.PdfTemplateRepository;
import org.vgk.hr.domain.request.TemplateFieldRequest;
import org.vgk.hr.domain.request.TemplateUploadRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PdfTemplateServiceTest {

    private final PdfTemplateRepository repository = mock(PdfTemplateRepository.class);
    private final PdfTemplateService service = new PdfTemplateService(repository);

    @Test
    void savesNewTemplateAndConvertsFields() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "template.pdf", "application/pdf", new byte[]{1, 2});
        TemplateUploadRequest request = validRequest(1);
        when(repository.findByTemplateNumber(1)).thenReturn(Optional.empty());
        when(repository.save(any(PdfTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.saveTemplate(file, request);

        var templateCaptor = org.mockito.ArgumentCaptor.forClass(PdfTemplate.class);
        verify(repository).save(templateCaptor.capture());
        PdfTemplate savedTemplate = templateCaptor.getValue();
        assertEquals(1, savedTemplate.getTemplateNumber());
        assertEquals("template.pdf", savedTemplate.getOriginalFileName());
        assertEquals(3, savedTemplate.getFields().size());
        assertEquals(3, savedTemplate.getActiveFields().size());
        assertEquals(TemplateFieldType.CURRENT_DATE, savedTemplate.getFields().getFirst().getType());
        assertEquals(false, savedTemplate.getFields().getFirst().isBold());
        assertFalse(savedTemplate.getFields().getFirst().isDeleted());
    }

    @Test
    void updatesExistingTemplateAndSoftDeletesOldFields() throws Exception {
        PdfTemplate existingTemplate = new PdfTemplate();
        TemplateField oldField = new TemplateField(
                TemplateFieldType.FULL_NAME, 1, 1, 1, 10, 10, 12, "DEJAVU_SANS", "#000000", false, null
        );
        oldField.setTemplate(existingTemplate);
        existingTemplate.setFields(new ArrayList<>(List.of(oldField)));
        MockMultipartFile file = new MockMultipartFile("file", "replacement.pdf", "application/pdf", new byte[]{3});
        when(repository.findByTemplateNumber(1)).thenReturn(Optional.of(existingTemplate));

        service.saveTemplate(file, validRequest(1));

        verify(repository).save(existingTemplate);
        assertEquals("replacement.pdf", existingTemplate.getOriginalFileName());
        assertEquals(4, existingTemplate.getFields().size());
        assertTrue(oldField.isDeleted());
        assertEquals(3, existingTemplate.getActiveFields().size());
        assertTrue(existingTemplate.getActiveFields().stream().noneMatch(TemplateField::isDeleted));
    }

    @Test
    void returnsTemplateWhenItExists() {
        PdfTemplate template = new PdfTemplate();
        when(repository.findByTemplateNumber(1)).thenReturn(Optional.of(template));

        assertEquals(template, service.getTemplate(1));
    }

    @Test
    void throwsWhenTemplateDoesNotExist() {
        when(repository.findByTemplateNumber(9)).thenReturn(Optional.empty());

        assertThrows(TemplateNotFoundException.class, () -> service.getTemplate(9));
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "template.pdf", "application/pdf", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> service.saveTemplate(file, validRequest(1)));
    }

    @Test
    void rejectsNonPositiveTemplateNumber() {
        MockMultipartFile file = validFile();

        assertThrows(IllegalArgumentException.class, () -> service.saveTemplate(file, validRequest(0)));
    }

    @Test
    void rejectsMissingFields() {
        TemplateUploadRequest request = new TemplateUploadRequest(1, List.of());

        assertThrows(IllegalArgumentException.class, () -> service.saveTemplate(validFile(), request));
    }

    @Test
    void rejectsFieldWithoutTypeOrPage() {
        TemplateFieldRequest invalidField = new TemplateFieldRequest(
                null, 0, 0, 0, 10, 10, 12, "DEJAVU_SANS", "#000000", false, null
        );
        TemplateUploadRequest request = new TemplateUploadRequest(1, List.of(invalidField));

        assertThrows(IllegalArgumentException.class, () -> service.saveTemplate(validFile(), request));
    }

    @Test
    void rejectsRequestWithoutRequiredFieldTypes() {
        TemplateUploadRequest request = new TemplateUploadRequest(
                1,
                List.of(field(TemplateFieldType.FULL_NAME, 1))
        );

        assertThrows(IllegalArgumentException.class, () -> service.saveTemplate(validFile(), request));
    }

    @Test
    void rejectsRequestWithoutBirthDateField() {
        TemplateUploadRequest request = new TemplateUploadRequest(
                1,
                List.of(
                        field(TemplateFieldType.CURRENT_DATE, 1),
                        field(TemplateFieldType.FULL_NAME, 1)
                )
        );

        assertThrows(IllegalArgumentException.class, () -> service.saveTemplate(validFile(), request));
    }

    private TemplateUploadRequest validRequest(int templateNumber) {
        return new TemplateUploadRequest(
                templateNumber,
                List.of(
                        field(TemplateFieldType.CURRENT_DATE, 1),
                        field(TemplateFieldType.BIRTH_DATE, 1),
                        field(TemplateFieldType.FULL_NAME, 1)
                )
        );
    }

    private TemplateFieldRequest field(TemplateFieldType type, int pageNumber) {
        return new TemplateFieldRequest(
                type, pageNumber, 10, 20, 100, 20, 12, "DEJAVU_SANS", "#000000", false, "dd.MM.yyyy"
        );
    }

    private MockMultipartFile validFile() {
        return new MockMultipartFile("file", "template.pdf", "application/pdf", new byte[]{1});
    }
}
