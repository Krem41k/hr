package org.vgk.hr.template;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.vgk.hr.db.entity.PdfTemplate;
import org.vgk.hr.db.entity.TemplateField;
import org.vgk.hr.db.repository.PdfTemplateRepository;
import org.vgk.hr.domain.request.TemplateFieldRequest;
import org.vgk.hr.position.Position;
import org.vgk.hr.position.PositionService;
import org.vgk.hr.shared.TemplateFieldMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentTemplateService {

    private final PdfTemplateRepository pdfTemplateRepository;
    private final PositionService positionService;

    @Transactional(readOnly = true)
    public List<DocumentTemplateResponse> list(Long positionId) {
        positionService.requirePosition(positionId);
        return pdfTemplateRepository.findByPositionIdAndDeletedFalseOrderBySortOrderAscIdAsc(positionId).stream()
                .map(DocumentTemplateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentTemplateResponse get(Long positionId, Long templateId) {
        return DocumentTemplateResponse.from(requireTemplate(positionId, templateId));
    }

    @Transactional
    public DocumentTemplateResponse create(Long positionId, MultipartFile file, DocumentTemplateUploadRequest request)
            throws IOException {
        Position position = positionService.requirePosition(positionId);
        validateUpload(file, request);

        List<TemplateField> incoming = toFields(request.fields());
        validateNoConflicts(positionId, null, incoming);

        PdfTemplate template = new PdfTemplate();
        template.setPosition(position);
        template.setTemplateNumber(nextTemplateNumber());
        template.setName(resolveName(request.name(), file.getOriginalFilename()));
        template.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        template.setDeleted(false);
        template.setOriginalFileName(file.getOriginalFilename());
        template.setContent(file.getBytes());
        template.replaceFields(incoming);

        return DocumentTemplateResponse.from(pdfTemplateRepository.save(template));
    }

    @Transactional
    public DocumentTemplateResponse update(
            Long positionId,
            Long templateId,
            MultipartFile file,
            DocumentTemplateUploadRequest request
    ) throws IOException {
        positionService.requirePosition(positionId);
        PdfTemplate template = requireTemplate(positionId, templateId);
        validateUpload(file, request);

        List<TemplateField> incoming = toFields(request.fields());
        validateNoConflicts(positionId, templateId, incoming);

        template.setName(resolveName(request.name(), file.getOriginalFilename()));
        if (request.sortOrder() != null) {
            template.setSortOrder(request.sortOrder());
        }
        template.setOriginalFileName(file.getOriginalFilename());
        template.setContent(file.getBytes());
        template.replaceFields(incoming);

        return DocumentTemplateResponse.from(template);
    }

    @Transactional
    public void softDelete(Long positionId, Long templateId) {
        PdfTemplate template = requireTemplate(positionId, templateId);
        template.setDeleted(true);
    }

    private void validateNoConflicts(Long positionId, Long excludeTemplateId, List<TemplateField> incoming) {
        List<TemplateField> existing = new ArrayList<>();
        for (PdfTemplate template : pdfTemplateRepository
                .findByPositionIdAndDeletedFalseOrderBySortOrderAscIdAsc(positionId)) {
            if (excludeTemplateId != null && excludeTemplateId.equals(template.getId())) {
                continue;
            }
            existing.addAll(template.getActiveFields());
        }
        FieldCodeConflictValidator.validateCompatible(existing, incoming);
    }

    private PdfTemplate requireTemplate(Long positionId, Long templateId) {
        return pdfTemplateRepository.findByIdAndPositionIdAndDeletedFalse(templateId, positionId)
                .orElseThrow(() -> new DocumentTemplateNotFoundException(positionId, templateId));
    }

    private Integer nextTemplateNumber() {
        return pdfTemplateRepository.findMaxTemplateNumber() + 1;
    }

    private List<TemplateField> toFields(List<TemplateFieldRequest> fields) {
        return fields.stream().map(TemplateFieldMapper::toEntity).toList();
    }

    private void validateUpload(MultipartFile file, DocumentTemplateUploadRequest request) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("PDF template must not be empty");
        }
        if (request.fields() == null || request.fields().isEmpty()) {
            throw new IllegalArgumentException("At least one template field is required");
        }
        if (request.fields().stream().anyMatch(field ->
                field.fieldCode() == null || field.fieldCode().isBlank() || field.pageNumber() < 1)) {
            throw new IllegalArgumentException("Every field must have a fieldCode and a positive page number");
        }
    }

    private String resolveName(String name, String originalFileName) {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        if (originalFileName != null && !originalFileName.isBlank()) {
            return originalFileName;
        }
        return "template";
    }
}
