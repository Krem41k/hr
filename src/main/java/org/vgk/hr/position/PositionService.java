package org.vgk.hr.position;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vgk.hr.db.entity.FieldValueSource;
import org.vgk.hr.db.entity.PdfTemplate;
import org.vgk.hr.db.entity.TemplateField;
import org.vgk.hr.db.entity.WellKnownFieldCodes;
import org.vgk.hr.db.repository.PdfTemplateRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionRepository positionRepository;
    private final PdfTemplateRepository pdfTemplateRepository;

    @Transactional
    public PositionResponse create(PositionRequest request) {
        validateRequest(request, null);
        Position position = new Position(request.name().trim(), request.code().trim());
        if (request.active() != null) {
            position.setActive(request.active());
        }
        return PositionResponse.from(positionRepository.save(position));
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> list() {
        return positionRepository.findAllByOrderByNameAsc().stream()
                .map(PositionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PositionResponse get(Long id) {
        return PositionResponse.from(requirePosition(id));
    }

    @Transactional
    public PositionResponse update(Long id, PositionRequest request) {
        Position position = requirePosition(id);
        validateRequest(request, id);
        position.setName(request.name().trim());
        position.setCode(request.code().trim());
        if (request.active() != null) {
            position.setActive(request.active());
        }
        return PositionResponse.from(position);
    }

    @Transactional
    public void deactivate(Long id) {
        Position position = requirePosition(id);
        position.setActive(false);
    }

    @Transactional(readOnly = true)
    public FormSchemaResponse formSchema(Long positionId) {
        requirePosition(positionId);
        List<PdfTemplate> templates = pdfTemplateRepository
                .findByPositionIdAndDeletedFalseOrderBySortOrderAscIdAsc(positionId);

        Map<String, FormFieldSchema> unique = new LinkedHashMap<>();
        for (PdfTemplate template : templates) {
            for (TemplateField field : template.getActiveFields()) {
                if (field.getValueSource() != FieldValueSource.USER) {
                    continue;
                }
                String code = WellKnownFieldCodes.normalize(field.getFieldCode());
                unique.putIfAbsent(code, new FormFieldSchema(code, field.getLabel(), field.getValueType()));
            }
        }

        List<FormFieldSchema> fields = new ArrayList<>(unique.values());
        fields.sort(Comparator.comparing(FormFieldSchema::fieldCode));
        return new FormSchemaResponse(positionId, fields);
    }

    public Position requirePosition(Long id) {
        return positionRepository.findById(id)
                .orElseThrow(() -> new PositionNotFoundException(id));
    }

    private void validateRequest(PositionRequest request, Long currentId) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Position name must not be blank");
        }
        if (request.code() == null || request.code().isBlank()) {
            throw new IllegalArgumentException("Position code must not be blank");
        }
        String code = request.code().trim();
        boolean conflict = currentId == null
                ? positionRepository.existsByCodeIgnoreCase(code)
                : positionRepository.existsByCodeIgnoreCaseAndIdNot(code, currentId);
        if (conflict) {
            throw new IllegalArgumentException("Position code already exists: " + code);
        }
    }
}
