package com.cronos.gestiontributaria.obligaciones.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.cronos.gestiontributaria.common.TaxObligationType;
import com.cronos.gestiontributaria.common.TaxpayerType;

/**
 * Calcula la dueDate de obligaciones tributarias colombianas.
 * Fuente: Decreto 2229/2023 y Resolución DIAN 000227/2025.
 */
@Service
public class TaxObligationDateResolver {

    public LocalDate resolve(TaxObligationType type, String fiscalPeriod,
                             String identificacion, TaxpayerType taxpayerType,
                             boolean granContribuyente) {
        return switch (type) {
            case INCOME_TAX        -> resolveIncomeTax(fiscalPeriod, identificacion, taxpayerType, granContribuyente);
            case VAT               -> resolveVat(fiscalPeriod, identificacion, taxpayerType);
            case WITHHOLDING       -> resolveWithholding(fiscalPeriod, identificacion);
            case PATRIMONY         -> resolvePatrimony(fiscalPeriod, identificacion);
            case INDUSTRY_COMMERCE -> null;
        };
    }

    private LocalDate resolveIncomeTax(String fp, String id, TaxpayerType type, boolean gc) {
        if (type == TaxpayerType.NATURAL_PERSON) return resolveNPIncomeTax(id);
        int d = lastDigit(id);
        if (gc) {
            return switch (d) {
                case 1->LocalDate.of(2026,2,10); case 2->LocalDate.of(2026,2,11);
                case 3->LocalDate.of(2026,2,12); case 4->LocalDate.of(2026,2,13);
                case 5->LocalDate.of(2026,2,16); case 6->LocalDate.of(2026,2,17);
                case 7->LocalDate.of(2026,2,18); case 8->LocalDate.of(2026,2,19);
                case 9->LocalDate.of(2026,2,20); case 0->LocalDate.of(2026,2,23);
                default->throw new IllegalArgumentException("Dígito NIT inválido");
            };
        }
        return switch (d) {
            case 1->LocalDate.of(2026,5,12); case 2->LocalDate.of(2026,5,13);
            case 3->LocalDate.of(2026,5,14); case 4->LocalDate.of(2026,5,15);
            case 5->LocalDate.of(2026,5,19); case 6->LocalDate.of(2026,5,20);
            case 7->LocalDate.of(2026,5,21); case 8->LocalDate.of(2026,5,22);
            case 9->LocalDate.of(2026,5,25); case 0->LocalDate.of(2026,5,26);
            default->throw new IllegalArgumentException("Dígito NIT inválido");
        };
    }

    private LocalDate resolveNPIncomeTax(String id) {
        int d = lastTwoDigits(id);
        return switch (d) {
            case 1,2->LocalDate.of(2026,8,12); case 3,4->LocalDate.of(2026,8,13);
            case 5,6->LocalDate.of(2026,8,14); case 7,8->LocalDate.of(2026,8,18);
            case 9,10->LocalDate.of(2026,8,19); case 11,12->LocalDate.of(2026,8,20);
            case 13,14->LocalDate.of(2026,8,21); case 15,16->LocalDate.of(2026,8,24);
            case 17,18->LocalDate.of(2026,8,25); case 19,20->LocalDate.of(2026,8,26);
            case 21,22->LocalDate.of(2026,8,27); case 23,24->LocalDate.of(2026,8,28);
            case 25,26->LocalDate.of(2026,8,31); case 27,28->LocalDate.of(2026,9,1);
            case 29,30->LocalDate.of(2026,9,2); case 31,32->LocalDate.of(2026,9,3);
            case 33,34->LocalDate.of(2026,9,4); case 35,36->LocalDate.of(2026,9,7);
            case 37,38->LocalDate.of(2026,9,8); case 39,40->LocalDate.of(2026,9,9);
            case 41,42->LocalDate.of(2026,9,10); case 43,44->LocalDate.of(2026,9,11);
            case 45,46->LocalDate.of(2026,9,14); case 47,48->LocalDate.of(2026,9,15);
            case 49,50->LocalDate.of(2026,9,16); case 51,52->LocalDate.of(2026,9,17);
            case 53,54->LocalDate.of(2026,9,18); case 55,56->LocalDate.of(2026,9,21);
            case 57,58->LocalDate.of(2026,9,22); case 59,60->LocalDate.of(2026,9,23);
            case 61,62->LocalDate.of(2026,9,24); case 63,64->LocalDate.of(2026,9,25);
            case 65,66->LocalDate.of(2026,9,28); case 67,68->LocalDate.of(2026,10,1);
            case 69,70->LocalDate.of(2026,10,2); case 71,72->LocalDate.of(2026,10,5);
            case 73,74->LocalDate.of(2026,10,6); case 75,76->LocalDate.of(2026,10,7);
            case 77,78->LocalDate.of(2026,10,8); case 79,80->LocalDate.of(2026,10,9);
            case 81,82->LocalDate.of(2026,10,13); case 83,84->LocalDate.of(2026,10,14);
            case 85,86->LocalDate.of(2026,10,15); case 87,88->LocalDate.of(2026,10,16);
            case 89,90->LocalDate.of(2026,10,19); case 91,92->LocalDate.of(2026,10,20);
            case 93,94->LocalDate.of(2026,10,21); case 95,96->LocalDate.of(2026,10,22);
            case 97,98->LocalDate.of(2026,10,23); case 99,0->LocalDate.of(2026,10,26);
            default->throw new IllegalArgumentException("Últimos 2 dígitos fuera de rango: "+d);
        };
    }

    private LocalDate resolveWithholding(String fiscalPeriod, String id) {
        int d = lastDigit(id);
        int idx = (d == 0) ? 9 : d - 1;
        int[][] T = {
            {10,11,12,13,16,17,18,19,20,23},{10,11,12,13,16,17,18,19,20,24},
            {13,14,15,16,17,20,21,22,23,24},{12,13,14,15,19,20,21,22,25,26},
            {10,11,12,16,17,18,19,22,23,24},{9,10,13,14,15,16,17,21,22,23},
            {12,13,14,18,19,20,21,24,25,26},{9,10,11,14,15,16,17,18,21,22},
            {9,13,14,15,16,19,20,21,22,23},{11,12,13,17,18,19,20,23,24,25},
            {10,11,14,15,16,17,18,21,22,23},{13,14,15,18,19,20,21,22,25,26},
        };
        int month = Integer.parseInt(fiscalPeriod.split("-")[1]);
        int day = T[month-1][idx];
        int dm = (month==12)?1:month+1;
        int dy = (month==12)?2027:2026;
        return LocalDate.of(dy, dm, day);
    }

    private LocalDate resolveVat(String fp, String id, TaxpayerType type) {
        int d = lastDigit(id);
        int idx = (d == 0) ? 9 : d - 1;
        if (fp.contains("-B")) {
            int[][] T = {
                {10,11,12,13,16,17,18,19,20,24},{12,13,14,15,19,20,21,22,25,26},
                {9,10,13,14,15,16,17,21,22,23},{9,10,11,14,15,16,17,18,21,22},
                {11,12,13,17,18,19,20,23,24,25},{13,14,15,18,19,20,21,22,25,26},
            };
            int b = Integer.parseInt(fp.split("B")[1])-1;
            int[] ms={3,5,7,9,11,1}; int[] ys={2026,2026,2026,2026,2026,2027};
            return LocalDate.of(ys[b], ms[b], T[b][idx]);
        }
        int[][] T = {
            {12,13,14,15,19,20,21,22,25,26},{9,10,11,14,15,16,17,18,21,22},
            {13,14,15,18,19,20,21,22,25,26},
        };
        int q = Integer.parseInt(fp.split("Q")[1])-1;
        int[] ms={5,9,1}; int[] ys={2026,2026,2027};
        return LocalDate.of(ys[q], ms[q], T[q][idx]);
    }

    private LocalDate resolvePatrimony(String fp, String id) {
        int d = lastDigit(id);
        return switch (d) {
            case 1->LocalDate.of(2026,5,12); case 2->LocalDate.of(2026,5,13);
            case 3->LocalDate.of(2026,5,14); case 4->LocalDate.of(2026,5,15);
            case 5->LocalDate.of(2026,5,19); case 6->LocalDate.of(2026,5,20);
            case 7->LocalDate.of(2026,5,21); case 8->LocalDate.of(2026,5,22);
            case 9->LocalDate.of(2026,5,25); case 0->LocalDate.of(2026,5,26);
            default->throw new IllegalArgumentException("Dígito inválido");
        };
    }

    private int lastDigit(String id) {
        String c = id.replaceAll("[^0-9]", "");
        return Integer.parseInt(String.valueOf(c.charAt(c.length()-1)));
    }

    private int lastTwoDigits(String id) {
        String c = id.replaceAll("[^0-9]", "");
        return Integer.parseInt(c.substring(Math.max(0, c.length()-2)));
    }
}
