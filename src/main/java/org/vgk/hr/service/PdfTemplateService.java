package org.vgk.hr.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.vgk.hr.db.entity.PdfTemplate;
import org.vgk.hr.db.repository.PdfTemplateRepository;
import org.vgk.hr.domain.request.TemplateUploadRequest;
import org.vgk.hr.shared.TemplateFieldMapper;

import java.io.IOException;

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
        if (template.getName() == null || template.getName().isBlank()) {
            template.setName(file.getOriginalFilename());
        }
        template.setContent(file.getBytes());
        template.replaceFields(request.fields().stream()
                .map(TemplateFieldMapper::toEntity)
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
        if (request.fields().stream().anyMatch(field ->
                field.fieldCode() == null || field.fieldCode().isBlank() || field.pageNumber() < 1)) {
            throw new IllegalArgumentException("Every field must have a fieldCode and a positive page number");
        }
    }
}
