package org.vgk.hr.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.vgk.hr.db.entity.FieldValueSource;
import org.vgk.hr.db.entity.FieldValueType;
import org.vgk.hr.db.entity.PdfTemplate;
import org.vgk.hr.db.entity.TemplateField;
import org.vgk.hr.db.entity.WellKnownFieldCodes;
import org.vgk.hr.db.repository.PdfTemplateRepository;
import org.vgk.hr.domain.request.TemplateFieldRequest;
import org.vgk.hr.domain.request.TemplateUploadRequest;

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
        if (request.fields().stream().anyMatch(field ->
                field.fieldCode() == null || field.fieldCode().isBlank() || field.pageNumber() < 1)) {
            throw new IllegalArgumentException("Every field must have a fieldCode and a positive page number");
        }
    }

    private TemplateField toTemplateField(TemplateFieldRequest request) {
        String fieldCode = WellKnownFieldCodes.normalize(request.fieldCode());
        FieldValueType valueType = request.valueType() != null
                ? request.valueType()
                : defaultValueType(fieldCode);
        FieldValueSource valueSource = request.valueSource() != null
                ? request.valueSource()
                : defaultValueSource(fieldCode);
        String label = request.label() == null || request.label().isBlank()
                ? fieldCode
                : request.label();
        return new TemplateField(
                fieldCode,
                label,
                valueType,
                valueSource,
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

    private FieldValueType defaultValueType(String fieldCode) {
        if (WellKnownFieldCodes.BIRTH_DATE.equals(fieldCode)
                || WellKnownFieldCodes.CURRENT_DATE.equals(fieldCode)) {
            return FieldValueType.DATE;
        }
        return FieldValueType.TEXT;
    }

    private FieldValueSource defaultValueSource(String fieldCode) {
        if (WellKnownFieldCodes.CURRENT_DATE.equals(fieldCode)) {
            return FieldValueSource.SYSTEM;
        }
        return FieldValueSource.USER;
    }
}
