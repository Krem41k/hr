package org.vgk.hr.generation;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentPackControllerTest {

    private final DocumentPackService documentPackService = mock(DocumentPackService.class);
    private final DocumentPackController controller = new DocumentPackController(documentPackService);

    @Test
    void returnsZipWithBase64EmailHeaders() throws Exception {
        byte[] zip = {1, 2, 3};
        GenerateDocumentPackRequest request = new GenerateDocumentPackRequest(Map.of("fullName", "Иванов"));
        when(documentPackService.generate(1L, request)).thenReturn(
                new GeneratedDocumentPack(zip, "Документы Иванов", "Здравствуйте!", "driver-documents.zip")
        );

        var response = controller.generate(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("application/zip", response.getHeaders().getContentType().toString());
        assertArrayEquals(zip, response.getBody());
        assertEquals(
                DocumentPackController.EMAIL_ENCODING,
                response.getHeaders().getFirst(DocumentPackController.EMAIL_ENCODING_HEADER)
        );
        assertEquals("Документы Иванов", decode(response.getHeaders()
                .getFirst(DocumentPackController.EMAIL_SUBJECT_HEADER)));
        assertEquals("Здравствуйте!", decode(response.getHeaders()
                .getFirst(DocumentPackController.EMAIL_BODY_HEADER)));
        assertTrue(response.getHeaders().getContentDisposition().toString().contains("driver-documents.zip"));
    }

    private String decode(String headerValue) {
        return new String(Base64.getDecoder().decode(headerValue), StandardCharsets.UTF_8);
    }
}
