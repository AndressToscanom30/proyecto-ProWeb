package com.cronos.gestiontributaria.obligaciones.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.cronos.gestiontributaria.obligaciones.model.Document;
import com.cronos.gestiontributaria.obligaciones.model.TaxObligation;
import com.cronos.gestiontributaria.obligaciones.repository.DocumentRepository;
import com.cronos.gestiontributaria.obligaciones.repository.TaxObligationRepository;

/**
 * Tests unitarios de {@link DocumentService}.
 *
 * <p>Los repositorios se mockean con Mockito y la carpeta de subida se aísla
 * con {@code @TempDir} para que los tests no dejen residuos en disco.</p>
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private TaxObligationRepository taxObligationRepository;

    @InjectMocks
    private DocumentService documentService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void configurarPropiedades() {
        ReflectionTestUtils.setField(documentService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(documentService, "maxFileSizeMb", 10);
        ReflectionTestUtils.setField(documentService, "allowedTypes", new String[] {
                "application/pdf", "image/jpeg", "image/png",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        });
    }

    @Test
    void uploadDocument_sinObligacion_guardaSoloEnDocumentRepository() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "contrato.pdf", "application/pdf",
                new byte[1024 * 1024]); // 1 MB

        when(documentRepository.save(any(Document.class)))
                .thenAnswer(inv -> {
                    Document d = inv.getArgument(0);
                    d.setId("doc-001");
                    return d;
                });

        Document saved = documentService.uploadDocument(
                "tp-001", null, file, "contrato anual");

        assertEquals("tp-001", saved.getTaxPayerId());
        assertNull(saved.getObligationId());
        assertEquals("contrato.pdf", saved.getFileName());
        verify(documentRepository).save(any(Document.class));
        verify(taxObligationRepository, never()).save(any(TaxObligation.class));
    }

    @Test
    void uploadDocument_conObligacion_actualizaDocumentIds() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "factura.pdf", "application/pdf",
                new byte[1024 * 1024]); // 1 MB

        TaxObligation ob = new TaxObligation();
        ob.setId("ob-001");
        ob.setDocumentIds(new ArrayList<>());

        when(documentRepository.save(any(Document.class)))
                .thenAnswer(inv -> {
                    Document d = inv.getArgument(0);
                    d.setId("doc-002");
                    return d;
                });
        when(taxObligationRepository.findById("ob-001"))
                .thenReturn(Optional.of(ob));

        Document saved = documentService.uploadDocument(
                "tp-001", "ob-001", file, null);

        assertEquals("ob-001", saved.getObligationId());

        ArgumentCaptor<TaxObligation> captor = ArgumentCaptor.forClass(TaxObligation.class);
        verify(taxObligationRepository).save(captor.capture());
        TaxObligation actualizada = captor.getValue();
        assertEquals(1, actualizada.getDocumentIds().size(),
                "El id del documento debe haberse agregado a documentIds");
        assertEquals("doc-002", actualizada.getDocumentIds().get(0));
    }

    @Test
    void uploadDocument_archivoDemasiadoGrande_lanzaIllegalArgument() {
        // 11 MB supera el límite de 10 MB.
        MockMultipartFile file = new MockMultipartFile(
                "file", "grande.pdf", "application/pdf",
                new byte[11 * 1024 * 1024]);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> documentService.uploadDocument("tp-001", null, file, null));

        assertTrue(ex.getMessage().toLowerCase().contains("tamaño máximo"),
                "El mensaje debe mencionar 'tamaño máximo': " + ex.getMessage());
        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void uploadDocument_tipoMimeNoPermitido_lanzaIllegalArgument() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", "video/mp4",
                new byte[1024]);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> documentService.uploadDocument("tp-001", null, file, null));

        assertTrue(ex.getMessage().toLowerCase().contains("no permitido"),
                "El mensaje debe mencionar 'no permitido': " + ex.getMessage());
        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void findByTaxPayer_delegaAlRepositorio() {
        List<Document> esperados = List.of(new Document(), new Document(), new Document());
        when(documentRepository.findByTaxPayerIdOrderByUploadedAtDesc("tp-001"))
                .thenReturn(esperados);

        List<Document> resultado = documentService.findByTaxPayer("tp-001");

        assertNotNull(resultado);
        assertEquals(3, resultado.size());
        assertSame(esperados, resultado, "Debe retornar la misma lista del repositorio");
    }
}
