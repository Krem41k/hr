package org.vgk.hr.generation;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentPackZipWriterTest {

    private final DocumentPackZipWriter writer = new DocumentPackZipWriter();

    @Test
    void numbersEntriesAndStripsPdfExtension() throws Exception {
        byte[] zip = writer.write(List.of(
                new DocumentPackZipWriter.PackEntry("Направление.pdf", new byte[]{1}),
                new DocumentPackZipWriter.PackEntry("Согласие на обработку", new byte[]{2})
        ));

        assertEquals(List.of("01-Направление.pdf", "02-Согласие_на_обработку.pdf"), entryNames(zip));
    }

    @Test
    void makesDuplicateNamesUnique() throws Exception {
        byte[] zip = writer.write(List.of(
                new DocumentPackZipWriter.PackEntry("doc", new byte[]{1}),
                new DocumentPackZipWriter.PackEntry("doc", new byte[]{2}),
                new DocumentPackZipWriter.PackEntry("doc", new byte[]{3})
        ));

        assertEquals(List.of("01-doc.pdf", "02-doc.pdf", "03-doc.pdf"), entryNames(zip));
    }

    @Test
    void replacesUnsafeCharactersAndEmptyNames() throws Exception {
        byte[] zip = writer.write(List.of(
                new DocumentPackZipWriter.PackEntry("a/b:c*?.pdf", new byte[]{1}),
                new DocumentPackZipWriter.PackEntry("  ", new byte[]{2})
        ));

        assertEquals(List.of("01-abc.pdf", "02-document.pdf"), entryNames(zip));
    }

    @Test
    void keepsEntryContent() throws Exception {
        byte[] zip = writer.write(List.of(new DocumentPackZipWriter.PackEntry("doc", new byte[]{7, 8, 9})));

        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            in.getNextEntry();
            assertArrayEquals(new byte[]{7, 8, 9}, in.readAllBytes());
        }
    }

    private List<String> entryNames(byte[] zip) throws Exception {
        List<String> names = new ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        return names;
    }
}
