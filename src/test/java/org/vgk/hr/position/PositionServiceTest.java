package org.vgk.hr.position;

import org.junit.jupiter.api.Test;
import org.vgk.hr.db.entity.FieldValueSource;
import org.vgk.hr.db.entity.FieldValueType;
import org.vgk.hr.db.entity.PdfTemplate;
import org.vgk.hr.db.entity.TemplateField;
import org.vgk.hr.db.entity.WellKnownFieldCodes;
import org.vgk.hr.db.repository.PdfTemplateRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PositionServiceTest {

    private final PositionRepository positionRepository = mock(PositionRepository.class);
    private final PdfTemplateRepository pdfTemplateRepository = mock(PdfTemplateRepository.class);
    private final PositionService service = new PositionService(positionRepository, pdfTemplateRepository);

    @Test
    void createsPosition() {
        when(positionRepository.existsByCodeIgnoreCase("driver")).thenReturn(false);
        when(positionRepository.save(any(Position.class))).thenAnswer(invocation -> {
            Position position = invocation.getArgument(0);
            position.setId(10L);
            return position;
        });

        PositionResponse response = service.create(new PositionRequest("Водитель", "driver", true));

        assertEquals(10L, response.id());
        assertEquals("Водитель", response.name());
        assertEquals("driver", response.code());
        assertTrue(response.active());
    }

    @Test
    void rejectsDuplicateCode() {
        when(positionRepository.existsByCodeIgnoreCase("driver")).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new PositionRequest("Водитель", "driver", true)));
    }

    @Test
    void buildsFormSchemaFromUniqueUserFields() {
        Position position = new Position("Водитель", "driver");
        position.setId(1L);
        when(positionRepository.findById(1L)).thenReturn(Optional.of(position));

        PdfTemplate first = template(
                field(WellKnownFieldCodes.FULL_NAME, "ФИО", FieldValueType.TEXT, FieldValueSource.USER),
                field(WellKnownFieldCodes.CURRENT_DATE, "Дата", FieldValueType.DATE, FieldValueSource.SYSTEM)
        );
        PdfTemplate second = template(
                field(WellKnownFieldCodes.FULL_NAME, "ФИО", FieldValueType.TEXT, FieldValueSource.USER),
                field(WellKnownFieldCodes.BIRTH_DATE, "Дата рождения", FieldValueType.DATE, FieldValueSource.USER)
        );
        when(pdfTemplateRepository.findByPositionIdAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
                .thenReturn(List.of(first, second));

        FormSchemaResponse schema = service.formSchema(1L);

        assertEquals(1L, schema.positionId());
        assertEquals(2, schema.fields().size());
        assertEquals(WellKnownFieldCodes.BIRTH_DATE, schema.fields().get(0).fieldCode());
        assertEquals(WellKnownFieldCodes.FULL_NAME, schema.fields().get(1).fieldCode());
    }

    @Test
    void deactivatesPosition() {
        Position position = new Position("Водитель", "driver");
        position.setId(1L);
        when(positionRepository.findById(1L)).thenReturn(Optional.of(position));

        service.deactivate(1L);

        assertEquals(false, position.isActive());
        verify(positionRepository).findById(1L);
    }

    private PdfTemplate template(TemplateField... fields) {
        PdfTemplate template = new PdfTemplate();
        template.replaceFields(List.of(fields));
        return template;
    }

    private TemplateField field(String code, String label, FieldValueType type, FieldValueSource source) {
        return new TemplateField(code, label, type, source, 1, 1, 1, 1, 1, 12, "DEJAVU_SANS", "#000", false, null);
    }
}
