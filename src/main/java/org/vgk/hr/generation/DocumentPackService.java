package org.vgk.hr.generation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vgk.hr.db.entity.FieldValueSource;
import org.vgk.hr.db.entity.FieldValueType;
import org.vgk.hr.db.entity.PdfTemplate;
import org.vgk.hr.db.entity.TemplateField;
import org.vgk.hr.db.entity.WellKnownFieldCodes;
import org.vgk.hr.domain.request.TextEditRequest;
import org.vgk.hr.emailtemplate.EmailTemplateRenderer;
import org.vgk.hr.emailtemplate.EmailTemplateResponse;
import org.vgk.hr.emailtemplate.EmailTemplateService;
import org.vgk.hr.position.PositionResponse;
import org.vgk.hr.position.PositionService;
import org.vgk.hr.service.PdfEditorService;
import org.vgk.hr.shared.TextEditFactory;
import org.vgk.hr.template.DocumentTemplateService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Формирует пакет документов должности: заполняет каждый активный PDF и рендерит письмо.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentPackService {

    private final PositionService positionService;
    private final DocumentTemplateService documentTemplateService;
    private final EmailTemplateService emailTemplateService;
    private final EmailTemplateRenderer emailTemplateRenderer;
    private final FieldValueResolver fieldValueResolver;
    private final DocumentPackZipWriter zipWriter;
    private final PdfEditorService pdfEditorService;

    @Transactional(readOnly = true)
    public GeneratedDocumentPack generate(Long positionId, GenerateDocumentPackRequest request) throws IOException {
        PositionResponse position = positionService.get(positionId);
        if (!position.active()) {
            throw new PositionInactiveException(positionId);
        }

        List<PdfTemplate> templates = documentTemplateService.activeTemplatesWithContent(positionId);
        if (templates.isEmpty()) {
            throw new EmptyDocumentPackException(positionId);
        }

        Map<String, FieldValueType> requiredUserFields = requiredUserFields(templates);
        Map<String, String> userValues = validateUserValues(requiredUserFields, request);
        LocalDate today = LocalDate.now();

        List<DocumentPackZipWriter.PackEntry> entries = new ArrayList<>(templates.size());
        for (PdfTemplate template : templates) {
            entries.add(new DocumentPackZipWriter.PackEntry(
                    template.getName(),
                    pdfEditorService.editPdf(template.getContent(), buildEdits(template, userValues, today))
            ));
        }
        byte[] zip = zipWriter.write(entries);

        Map<String, String> emailValues = emailValues(requiredUserFields, userValues, today);
        Optional<EmailTemplateResponse> emailTemplate = emailTemplateService.find(positionId);
        String subject = emailTemplate
                .map(template -> emailTemplateRenderer.render(template.subjectTemplate(), emailValues))
                .orElse("");
        String body = emailTemplate
                .map(template -> emailTemplateRenderer.render(template.bodyTemplate(), emailValues))
                .orElse("");
        if (emailTemplate.isEmpty()) {
            log.warn("Position has no email template, generating pack without email: positionId={}", positionId);
        }

        log.info(
                "Document pack generated: positionId={}, templates={}, zipSize={} bytes",
                positionId,
                templates.size(),
                zip.length
        );
        return new GeneratedDocumentPack(zip, subject, body, "%s-documents.zip".formatted(position.code()));
    }

    private Map<String, FieldValueType> requiredUserFields(List<PdfTemplate> templates) {
        Map<String, FieldValueType> required = new LinkedHashMap<>();
        for (PdfTemplate template : templates) {
            for (TemplateField field : template.getActiveFields()) {
                if (field.getValueSource() != FieldValueSource.USER) {
                    continue;
                }
                required.putIfAbsent(WellKnownFieldCodes.normalize(field.getFieldCode()), field.getValueType());
            }
        }
        return required;
    }

    private Map<String, String> validateUserValues(
            Map<String, FieldValueType> requiredUserFields,
            GenerateDocumentPackRequest request
    ) {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (request != null && request.fields() != null) {
            request.fields().forEach((code, value) ->
                    normalized.put(WellKnownFieldCodes.normalize(code), value));
        }

        Set<String> unknown = new LinkedHashSet<>(normalized.keySet());
        unknown.removeAll(requiredUserFields.keySet());
        if (!unknown.isEmpty()) {
            throw new UnknownFieldCodesException(unknown);
        }

        Set<String> missing = new LinkedHashSet<>();
        for (String code : requiredUserFields.keySet()) {
            String value = normalized.get(code);
            if (value == null || value.isBlank()) {
                missing.add(code);
            }
        }
        if (!missing.isEmpty()) {
            throw new MissingFieldValuesException(missing);
        }
        return normalized;
    }

    private List<TextEditRequest> buildEdits(PdfTemplate template, Map<String, String> userValues, LocalDate today) {
        return template.getActiveFields().stream()
                .map(field -> TextEditFactory.from(field, fieldValueResolver.forPdf(field, userValues, today)))
                .toList();
    }

    private Map<String, String> emailValues(
            Map<String, FieldValueType> requiredUserFields,
            Map<String, String> userValues,
            LocalDate today
    ) {
        Map<String, String> values = new LinkedHashMap<>();
        requiredUserFields.forEach((code, valueType) ->
                values.put(code, fieldValueResolver.forEmail(code, valueType, userValues.get(code))));
        values.putIfAbsent(
                WellKnownFieldCodes.CURRENT_DATE,
                fieldValueResolver.forEmail(WellKnownFieldCodes.CURRENT_DATE, FieldValueType.DATE, today.toString())
        );
        return values;
    }
}
