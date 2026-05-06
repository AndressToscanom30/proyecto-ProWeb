package com.cronos.gestiontributaria.empleados.model;

import com.cronos.gestiontributaria.common.Priority;
import com.cronos.gestiontributaria.common.TaskStatus;
import java.time.LocalDate;

public class Task {
    private String title;
    private String description;
    private Priority priority;
    private TaskStatus status;
    private LocalDate dueDate;

    public Task(String title, String description, Priority priority, TaskStatus status, LocalDate dueDate) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.dueDate = dueDate;
    }

    public void assign(Employee employee) {
        if (employee != null && employee.getTasks() != null && !employee.getTasks().contains(this)) {
            employee.getTasks().add(this);
        }
    }

    public void reassign(Employee employee) {
        assign(employee);
    }

    public void complete() {
        this.status = TaskStatus.COMPLETED;
    }

    public void cancel() {
        this.status = TaskStatus.CANCELLED;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
}
