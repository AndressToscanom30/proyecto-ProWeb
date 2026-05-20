package com.cronos.gestiontributaria.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cronos.gestiontributaria.auth.model.Role;
import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.auth.repository.UserRepository;
import com.cronos.gestiontributaria.common.TaxObligationStatus;
import com.cronos.gestiontributaria.common.TaxObligationType;
import com.cronos.gestiontributaria.common.TaxpayerType;
import com.cronos.gestiontributaria.notification.model.Notification;
import com.cronos.gestiontributaria.obligaciones.dto.TaxObligationResponseDTO;
import com.cronos.gestiontributaria.obligaciones.service.TaxObligationService;

@ExtendWith(MockitoExtension.class)
class NotificationCenterServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaxObligationService obligationService;

    @InjectMocks
    private NotificationCenterService notificationCenterService;

    @Test
    void refreshInbox_sincronizaAlertasYPreservaLeidas() {
        User user = buildUser();
        Notification legacy = new Notification(
                "Vencida · IVA",
                "Mensaje histórico",
                "warning",
                true,
                LocalDateTime.now().minusDays(5),
                "obl-1",
                "/obligaciones/obl-1");
        Notification manual = new Notification(
                "Mensaje manual",
                "primary",
                false);
        user.setNotifications(new ArrayList<>(List.of(legacy, manual)));

        when(userRepository.findByEmail("gerente@cronos.com"))
                .thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(obligationService.findByTaxPayerId("tp-001"))
                .thenReturn(List.of(
                        obligation("obl-1", "tp-001", "Cliente Uno", LocalDate.now().plusDays(3), TaxObligationStatus.PENDING),
                        obligation("obl-2", "tp-001", "Cliente Dos", LocalDate.now().minusDays(1), TaxObligationStatus.OVERDUE)));

        User refreshed = notificationCenterService.refreshInbox(user);

        verify(obligationService).findByTaxPayerId("tp-001");
        verify(userRepository).save(any(User.class));

        assertEquals(3, refreshed.getNotifications().size());

        Notification updated = refreshed.getNotifications().stream()
                .filter(notification -> "obl-1".equals(notification.getSourceId()))
                .findFirst()
                .orElseThrow();
        assertTrue(updated.isRead(), "La notificación existente debe conservar el estado leído");
        assertEquals("warning", updated.getType());

        Notification urgent = refreshed.getNotifications().stream()
                .filter(notification -> "obl-2".equals(notification.getSourceId()))
                .findFirst()
                .orElseThrow();
        assertFalse(urgent.isRead(), "La nueva alerta debe llegar sin leer");
        assertEquals("danger", urgent.getType());

        Notification manualNotification = refreshed.getNotifications().stream()
                .filter(notification -> notification.getSourceId() == null)
                .findFirst()
                .orElseThrow();
        assertNotNull(manualNotification.getId());
    }

    @Test
    void markAsRead_actualizaUnaNotificacionPorId() {
        User user = buildUser();
        Notification notification = new Notification(
                "Vence pronto · IVA",
                "Mensaje",
                "warning",
                false,
                LocalDateTime.now().minusHours(2),
                "obl-9",
                "/obligaciones/obl-9");
        notification.setId("note-1");
        user.setNotifications(new ArrayList<>(List.of(notification)));

        when(userRepository.findByEmail("gerente@cronos.com"))
                .thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User updated = notificationCenterService.markAsRead(user, "note-1");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertTrue(captor.getValue().getNotifications().get(0).isRead());
        assertTrue(updated.getNotifications().get(0).isRead());
    }

    private User buildUser() {
        User user = new User();
        user.setId("user-1");
        user.setName("Gerente");
        user.setEmail("gerente@cronos.com");
        user.setActive(true);
        user.setRole(new Role("ROLE_GERENTE", "Gerente", new ArrayList<>()));
        user.setTaxPayerId("tp-001");
        user.setNotifications(new ArrayList<>());
        return user;
    }

    private TaxObligationResponseDTO obligation(String id, String taxPayerId, String name, LocalDate dueDate,
                                                TaxObligationStatus status) {
        return new TaxObligationResponseDTO(
                id,
                taxPayerId,
                name,
                "900123456-7",
                TaxpayerType.LEGAL_ENTITY,
                TaxObligationType.VAT,
                "2026-B1",
                2026,
                dueDate,
                false,
                null,
                status,
                null,
                null,
                null,
                List.of());
    }
}