package com.cronos.gestiontributaria.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.clientes.repository.TaxPayerRepository;
import com.cronos.gestiontributaria.common.TaxpayerType;

@Component
public class DataRecoveryRunner implements CommandLineRunner {

    private final TaxPayerRepository taxPayerRepository;

    public DataRecoveryRunner(TaxPayerRepository taxPayerRepository) {
        this.taxPayerRepository = taxPayerRepository;
    }

    @Override
    public void run(String... args) {
        String testId = "test-taxpayer-id";
        if (!taxPayerRepository.existsById(testId)) {
            TaxPayer tp = new TaxPayer();
            tp.setId(testId);
            tp.setBusinessName("Contribuyente de Prueba (Recuperado)");
            tp.setIdentificacion("900123456-1");
            tp.setEmail("contribuyente@prueba.com");
            tp.setType(TaxpayerType.LEGAL_ENTITY);
            tp.setActive(true);
            taxPayerRepository.save(tp);
            System.out.println("========== RECOVERY EXITOSO: Contribuyente test-taxpayer-id recreado ==========");
        }
    }
}
