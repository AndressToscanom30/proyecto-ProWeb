package com.cronos.gestiontributaria.obligaciones.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.clientes.repository.TaxPayerRepository;
import com.cronos.gestiontributaria.clientes.service.TaxPayerService;
import com.cronos.gestiontributaria.obligaciones.model.Document;
import com.cronos.gestiontributaria.obligaciones.repository.DocumentRepository;

@SpringBootTest
@ActiveProfiles("test")
public class DocumentServiceIntegrationTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private TaxPayerService taxPayerService;

    @Autowired
    private TaxPayerRepository taxPayerRepository;

    @AfterEach
    void tearDown() {
        documentRepository.deleteAll();
        taxPayerRepository.deleteAll();
    }

    @Test
    void subirDocumentoExitoso() {
        TaxPayer tp = new TaxPayer();
        tp.setBusinessName("Doc Test");
        tp.setIdentificacion("DOC-001");
        TaxPayer guardado = taxPayerService.create(tp);

        MultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "Contenido PDF".getBytes());

        Document doc = documentService.uploadDocument(guardado.getId(), null, file, "Prueba");

        assertNotNull(doc.getId());
        assertEquals(guardado.getId(), doc.getTaxPayerId());
        assertEquals("test.pdf", doc.getFileName());
        assertEquals("application/pdf", doc.getFileType());
        
        List<Document> docs = documentService.findByTaxPayer(guardado.getId());
        assertEquals(1, docs.size());
    }

    @Test
    void subirDocumentoExtensionInvalida() {
        TaxPayer tp = new TaxPayer();
        tp.setBusinessName("Doc Test 2");
        tp.setIdentificacion("DOC-002");
        TaxPayer guardado = taxPayerService.create(tp);

        MultipartFile file = new MockMultipartFile("file", "test.exe", "application/x-msdownload", "Malware".getBytes());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            documentService.uploadDocument(guardado.getId(), null, file, "Prueba");
        });
        
        assertEquals("Extensión de archivo no permitida.", ex.getMessage());
    }
}
