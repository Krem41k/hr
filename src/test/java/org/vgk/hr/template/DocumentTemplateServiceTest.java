package org.vgk.hr.template;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.vgk.hr.db.entity.FieldValueSource;
import org.vgk.hr.db.entity.FieldValueType;
import org.vgk.hr.db.entity.PdfTemplate;
import org.vgk.hr.db.entity.TemplateField;
import org.vgk.hr.db.entity.WellKnownFieldCodes;
import org.vgk.hr.db.repository.PdfTemplateRepository;
import org.vgk.hr.domain.request.TemplateFieldRequest;
import org.vgk.hr.position.Position;
import org.vgk.hr.position.PositionService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentTemplateServiceTest {

    private final PdfTemplateRepository repository = mock(PdfTemplateRepository.class);
    private final PositionService positionService = mock(PositionService.class);
    private final DocumentTemplateService service = new DocumentTemplateService(repository, positionService);

    @Test
    void createsTemplateForPosition() throws Exception {
        Position position = new Position("Водитель", "driver");
        position.setId(5L);
        when(positionService.requirePosition(5L)).thenReturn(position);
        when(repository.findByPositionIdAndDeletedFalseOrderBySortOrderAscIdAsc(5L)).thenReturn(List.of());
        when(repository.findMaxTemplateNumber()).thenReturn(3);
        when(repository.save(any(PdfTemplate.class))).thenAnswer(invocation -> {
            PdfTemplate template = invocation.getArgument(0);
            template.setId(100L);
            return template;
        });

        DocumentTemplateResponse response = service.create(
                5L,
                new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1}),
                new DocumentTemplateUploadRequest("Направление", 1, List.of(fieldRequest(WellKnownFieldCodes.FULL_NAME, "ФИО")))
        );

        assertEquals(100L, response.id());
        assertEquals(5L, response.positionId());
        assertEquals("Направление", response.name());
        assertEquals(1, response.sortOrder());
        assertEquals(4, response.templateNumber());
        assertEquals(1, response.fields().size());
    }

    @Test
    void rejectsConflictingFieldCodesAcrossTemplates() {
        Position position = new Position("Водитель", "driver");
        position.setId(5L);
        when(positionService.requirePosition(5L)).thenReturn(position);

        PdfTemplate existing = new PdfTemplate();
        existing.setId(1L);
        existing.replaceFields(List.of(
                new TemplateField(WellKnownFieldCodes.FULL_NAME, "ФИО", FieldValueType.TEXT, FieldValueSource.USER,
                        1, 1, 1, 1, 1, 12, null, null, false, null)
        ));
        when(repository.findByPositionIdAndDeletedFalseOrderBySortOrderAscIdAsc(5L)).thenReturn(List.of(existing));

        assertThrows(FieldCodeConflictException.class, () -> service.create(
                5L,
                new MockMultipartFile("file", "b.pdf", "application/pdf", new byte[]{1}),
                new DocumentTemplateUploadRequest(
                        "Другой",
                        2,
                        List.of(fieldRequest(WellKnownFieldCodes.FULL_NAME, "Другая подпись"))
                )
        ));
    }

    @Test
    void softDeletesTemplate() {
        PdfTemplate template = new PdfTemplate();
        template.setId(9L);
        when(repository.findByIdAndPositionIdAndDeletedFalse(9L, 5L)).thenReturn(Optional.of(template));

        service.softDelete(5L, 9L);

        assertTrue(template.isDeleted());
        verify(repository).findByIdAndPositionIdAndDeletedFalse(9L, 5L);
    }

    private TemplateFieldRequest fieldRequest(String code, String label) {
        return new TemplateFieldRequest(
                code, label, FieldValueType.TEXT, FieldValueSource.USER,
                1, 10, 20, 30, 40, 12, "DEJAVU_SANS", "#000000", false, null
        );
    }
}
