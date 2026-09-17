package org.vgk.hr.generation;

/**
 * Результат генерации: ZIP с заполненными PDF и текст письма.
 */
public record GeneratedDocumentPack(
        byte[] zip,
        String emailSubject,
        String emailBody,
        String fileName
) {
}
