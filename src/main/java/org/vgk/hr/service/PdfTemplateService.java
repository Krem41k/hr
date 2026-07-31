package org.vgk.hr.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.vgk.hr.db.entity.PdfTemplate;
import org.vgk.hr.db.entity.TemplateField;
import org.vgk.hr.db.entity.TemplateFieldType;
import org.vgk.hr.db.repository.PdfTemplateRepository;
import org.vgk.hr.domain.request.TemplateFieldRequest;
import org.vgk.hr.domain.request.TemplateUploadRequest;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfTemplateService {

    private final PdfTemplateRepository pdfTemplateRepository;

    @Transactional
    public void saveTemplate(MultipartFile file, TemplateUploadRequest request) throws IOException {
        validateUpload(file, request);

        PdfTemplate template = pdfTemplateRepository.findByTemplateNumber(request.templateNumber())
                .orElseGet(PdfTemplate::new);
        template.setTemplateNumber(request.templateNumber());
        template.setOriginalFileName(file.getOriginalFilename());
        template.setContent(file.getBytes());
        template.replaceFields(request.fields().stream()
                .map(this::toTemplateField)
                .toList());
        pdfTemplateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public PdfTemplate getTemplate(Integer templateNumber) {
        return pdfTemplateRepository.findByTemplateNumber(templateNumber)
                .orElseThrow(() -> new TemplateNotFoundException(templateNumber));
    }

    private void validateUpload(MultipartFile file, TemplateUploadRequest request) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("PDF template must not be empty");
        }
        if (request.templateNumber() == null || request.templateNumber() < 1) {
            throw new IllegalArgumentException("Template number must be positive");
        }
        if (request.fields() == null || request.fields().isEmpty()) {
            throw new IllegalArgumentException("At least one template field is required");
        }
        if (request.fields().stream().anyMatch(field -> field.type() == null || field.pageNumber() < 1)) {
            throw new IllegalArgumentException("Every field must have a type and a positive page number");
        }
        if (countFields(request.fields(), TemplateFieldType.CURRENT_DATE) == 0
                || countFields(request.fields(), TemplateFieldType.BIRTH_DATE) == 0
                || countFields(request.fields(), TemplateFieldType.FULL_NAME) == 0) {
            throw new IllegalArgumentException("Date, birth date and full name fields are required");
        }
    }

    private TemplateField toTemplateField(TemplateFieldRequest request) {
        return new TemplateField(
                request.type(),
                request.pageNumber(),
                request.x(),
                request.y(),
                request.width(),
                request.height(),
                request.fontSize(),
                request.fontName(),
                request.color(),
                Boolean.TRUE.equals(request.bold()),
                request.dateFormat()
        );
    }

    private long countFields(List<TemplateFieldRequest> fields, TemplateFieldType type) {
        return fields.stream()
                .filter(field -> field.type() == type)
                .count();
    }
}
