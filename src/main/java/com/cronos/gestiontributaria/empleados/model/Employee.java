package com.cronos.gestiontributaria.empleados.model;

import com.cronos.gestiontributaria.auth.model.Role;
import com.cronos.gestiontributaria.auth.model.User;
import com.cronos.gestiontributaria.common.History;
import com.cronos.gestiontributaria.common.TaskStatus;
import com.cronos.gestiontributaria.notification.model.Notification;
import com.cronos.gestiontributaria.obligaciones.model.TaxObligation;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Employee extends User {
    private String position;
    private String phone;
    private LocalDate hireDate;
    private List<Task> tasks;
    private List<TaxObligation> obligations;

    public Employee(String name, String email, String passwordHash, boolean active, Role role,
                    List<Notification> notifications, List<History> history,
                    String position, String phone, LocalDate hireDate,
                    List<Task> tasks, List<TaxObligation> obligations) {
        super(name, email, passwordHash, active, role, notifications, history);
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
