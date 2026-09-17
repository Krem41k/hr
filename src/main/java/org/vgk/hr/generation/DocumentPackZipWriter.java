package org.vgk.hr.generation;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Упаковывает заполненные PDF в ZIP с предсказуемыми уникальными именами файлов.
 */
@Component
public class DocumentPackZipWriter {

    public byte[] write(List<PackEntry> entries) throws IOException {
        Set<String> usedNames = new HashSet<>();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            int index = 1;
            for (PackEntry entry : entries) {
                zip.putNextEntry(new ZipEntry(uniqueName(usedNames, index++, entry.name())));
                zip.write(entry.content());
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }

    private String uniqueName(Set<String> usedNames, int index, String rawName) {
        String base = "%02d-%s".formatted(index, sanitize(rawName));
        String candidate = base + ".pdf";
        int suffix = 2;
        while (!usedNames.add(candidate)) {
            candidate = "%s-%d.pdf".formatted(base, suffix++);
        }
        return candidate;
    }

    private String sanitize(String rawName) {
        String withoutExtension = rawName == null || rawName.isBlank()
                ? "document"
                : rawName.replaceFirst("(?i)\\.pdf$", "");
        String cleaned = withoutExtension
                .replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "")
                .replaceAll("\\s+", "_")
                .trim();
        return cleaned.isEmpty() ? "document" : cleaned;
    }

    public record PackEntry(String name, byte[] content) {
    }
}
