package com.cronos.gestiontributaria.empleados.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.cronos.gestiontributaria.auth.model.Role;
import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.common.TaskStatus;
import com.cronos.gestiontributaria.notification.model.Notification;
import com.cronos.gestiontributaria.obligaciones.model.TaxObligation;

/**
 * Modelo de empleado que extiende la información base de un usuario.
 *
 * <p>
 * Agrega atributos operativos como cargo, teléfono, fecha de ingreso,
 * tareas asignadas y obligaciones tributarias.
 * </p>
 */
public class Employee extends User {
    private String position;
    private String phone;
    private LocalDate hireDate;
    private List<Task> tasks;
    private List<TaxObligation> obligations;

    /**
     * Crea un empleado con sus datos base, contacto y colecciones operativas.
     *
     * @param name          nombre del empleado
     * @param email         correo del empleado
     * @param passwordHash  contraseña cifrada
     * @param active        estado de la cuenta
     * @param role          rol asociado
     * @param notifications notificaciones del usuario
     * @param position      cargo o puesto
     * @param phone         teléfono de contacto
     * @param hireDate      fecha de ingreso
     * @param tasks         tareas asignadas
     * @param obligations   obligaciones tributarias asociadas
     */
    public Employee(String name, String email, String passwordHash, boolean active, Role role,
            List<Notification> notifications,
            String position, String phone, LocalDate hireDate,
            List<Task> tasks, List<TaxObligation> obligations) {
        super(name, email, passwordHash, active, role, notifications);
        this.position = position;
        this.phone = phone;
        this.hireDate = hireDate;
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        this.obligations = obligations != null ? obligations : new ArrayList<>();
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public List<TaxObligation> getObligations() {
        return obligations;
    }

    /**
     * Calcula el porcentaje de tareas completadas sobre el total asignado.
     *
     * @return valor entre 0.0 y 1.0 con el rendimiento del empleado
     */
    public double calculatePerformance() {
        if (tasks == null || tasks.isEmpty()) {
            return 0.0;
        }
        int completed = 0;
        for (Task task : tasks) {
            if (task != null && TaskStatus.COMPLETED.equals(task.getStatus())) {
                completed++;
            }
        }
        return (double) completed / (double) tasks.size();
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
    }

    public void setObligations(List<TaxObligation> obligations) {
        this.obligations = obligations != null ? obligations : new ArrayList<>();
    }
}