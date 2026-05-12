package com.cronos.gestiontributaria.common.view;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.cronos.gestiontributaria.clientes.model.TaxPayer;
import com.cronos.gestiontributaria.common.TaxObligationStatus;
import com.cronos.gestiontributaria.common.TaxObligationType;
import com.cronos.gestiontributaria.obligaciones.model.TaxObligation;

/**

 * Documentación de la entidad DashboardSummary.

 */

public record DashboardSummary(
        String monthLabel,
        List<MetricCard> metricCards,
        List<CalendarDay> calendarDays,
        List<DeadlineItem> upcomingDeadlines,
        List<WorkloadBar> weeklyWorkload,
        List<MonthlyPoint> monthlyCompliance,
        String monthlyLinePath,
        List<DistributionSlice> distributionSlices,
        String distributionStyle,
        List<TaskRow> recentTasks,
        List<CriticalClient> criticalClients) {

    public static DashboardSummary build(List<TaxPayer> taxPayers, List<TaxObligation> obligations) {
        Locale locale = Locale.forLanguageTag("es-CO");
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);

        List<TaxPayer> safeTaxPayers = taxPayers == null ? List.of()
                : taxPayers.stream().filter(Objects::nonNull).toList();
        List<TaxObligation> safeObligations = obligations == null ? List.of()
                : obligations.stream().filter(Objects::nonNull).toList();

        Map<String, TaxPayer> clientsById = safeTaxPayers.stream()
                .filter(payer -> payer.getId() != null)
                .collect(Collectors.toMap(TaxPayer::getId, Function.identity(), (left, right) -> left,
                        LinkedHashMap::new));

        int pendingCount = 0;
        int overdueCount = 0;
        int completedCount = 0;
        int monthCount = 0;
        int activeClients = 0;
        int relevantCount = 0;
        Map<TaxObligationType, Integer> typeCounts = new EnumMap<>(TaxObligationType.class);

        for (TaxObligation obligation : safeObligations) {
            if (obligation == null) {
                continue;
            }

            TaxObligationStatus status = obligation.getStatus();
            boolean cancelled = status == TaxObligationStatus.CANCELLED;
            boolean completed = status == TaxObligationStatus.COMPLETED;
            boolean overdue = isOverdue(obligation, today);

            if (!cancelled) {
                relevantCount++;
            }
            if (completed) {
                completedCount++;
            }
            if (!completed && !cancelled) {
                if (overdue) {
                    overdueCount++;
                } else {
                    pendingCount++;
                }
            }
            if (obligation.getDueDate() != null && !cancelled && YearMonth.from(obligation.getDueDate()).equals(currentMonth)) {
                monthCount++;
            }
            if (obligation.getType() != null) {
                typeCounts.merge(obligation.getType(), 1, Integer::sum);
            }
        }

        for (TaxPayer taxPayer : safeTaxPayers) {
            if (taxPayer.isActive()) {
                activeClients++;
            }
        }

        double compliance = relevantCount == 0 ? 100.0 : (completedCount * 100.0 / relevantCount);

        List<MetricCard> metricCards = List.of(
                new MetricCard(
                        "Tareas pendientes",
                        formatCount(pendingCount),
                        "En seguimiento operativo",
                        "warning",
                        "pending"),
                new MetricCard(
                        "Tareas vencidas",
                        formatCount(overdueCount),
                        "Requieren prioridad inmediata",
                        "danger",
                        "overdue"),
                new MetricCard(
                        "Obligaciones del mes",
                        formatCount(monthCount),
                        formatMonthLabel(currentMonth, locale),
                        "primary",
                        "calendar"),
                new MetricCard(
                        "Clientes activos",
                        formatCount(activeClients),
                        "Base operativa disponible",
                        "success",
                        "clients"),
                new MetricCard(
                        "Cumplimiento",
                        formatPercent(compliance),
                        "Cierre del ciclo fiscal",
                        "info",
                        "compliance"));

        List<CalendarDay> calendarDays = buildCalendarDays(safeObligations, today, currentMonth);
        List<DeadlineItem> upcomingDeadlines = buildUpcomingDeadlines(safeObligations, clientsById, today);
        List<WorkloadBar> weeklyWorkload = buildWeeklyWorkload(safeObligations, today);
        List<MonthlyPoint> monthlyCompliance = buildMonthlyCompliance(safeObligations, today);
        String monthlyLinePath = buildLinePath(monthlyCompliance);
        List<DistributionSlice> distributionSlices = buildDistributionSlices(safeObligations);
        String distributionStyle = buildDistributionStyle(distributionSlices);
        List<TaskRow> recentTasks = buildRecentTasks(safeObligations, clientsById, today);
        List<CriticalClient> criticalClients = buildCriticalClients(safeObligations, clientsById, today);

        return new DashboardSummary(
                formatMonthLabel(currentMonth, locale),
                metricCards,
                calendarDays,
                upcomingDeadlines,
                weeklyWorkload,
                monthlyCompliance,
                monthlyLinePath,
                distributionSlices,
                distributionStyle,
                recentTasks,
                criticalClients);
    }

    private static List<CalendarDay> buildCalendarDays(List<TaxObligation> obligations, LocalDate today,
            YearMonth month) {
        List<CalendarDay> days = new ArrayList<>();
        LocalDate cursor = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = month.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        while (!cursor.isAfter(end)) {
            LocalDate currentDay = cursor;
            List<TaxObligation> dayObligations = obligations.stream()
                .filter(obligation -> obligation != null && obligation.getDueDate() != null
                    && obligation.getDueDate().isEqual(currentDay))
                    .toList();
            days.add(new CalendarDay(
                currentDay.getDayOfMonth(),
                    dayObligations.size(),
                calendarTone(dayObligations, currentDay, today),
                YearMonth.from(currentDay).equals(month),
                currentDay.isEqual(today)));
            cursor = cursor.plusDays(1);
        }

        return days;
    }

    private static List<DeadlineItem> buildUpcomingDeadlines(List<TaxObligation> obligations,
            Map<String, TaxPayer> clientsById, LocalDate today) {
        LocalDate windowEnd = today.plusDays(14);

        return obligations.stream()
                .filter(obligation -> obligation != null && !isCancelled(obligation) && !isCompleted(obligation))
                .filter(obligation -> obligation.getDueDate() != null)
                .filter(obligation -> !obligation.getDueDate().isBefore(today) && !obligation.getDueDate().isAfter(windowEnd))
                .sorted(Comparator.comparing(TaxObligation::getDueDate)
                        .thenComparing(obligation -> describeType(obligation.getType())))
                .limit(6)
                .map(obligation -> new DeadlineItem(
                    obligation.getTaxPayerId(),
                        resolveClientName(obligation, clientsById),
                        describeType(obligation.getType()),
                        formatDate(obligation.getDueDate()),
                        describeStatus(obligation.getStatus(), obligation, today),
                        deadlineTone(obligation, today)))
                .toList();
    }

    private static List<WorkloadBar> buildWeeklyWorkload(List<TaxObligation> obligations, LocalDate today) {
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        String[] labels = { "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom" };
        int[] counts = new int[7];
        int max = 0;

        for (int index = 0; index < 7; index++) {
            LocalDate day = weekStart.plusDays(index);
            int count = 0;
            for (TaxObligation obligation : obligations) {
                if (obligation != null && obligation.getDueDate() != null && obligation.getDueDate().isEqual(day)) {
                    count++;
                }
            }
            counts[index] = count;
            max = Math.max(max, count);
        }

        List<WorkloadBar> bars = new ArrayList<>();
        for (int index = 0; index < 7; index++) {
            double percent = max == 0 ? 0.0 : (counts[index] * 100.0 / max);
            bars.add(new WorkloadBar(labels[index], counts[index], percent, "primary"));
        }

        return bars;
    }

    private static List<MonthlyPoint> buildMonthlyCompliance(List<TaxObligation> obligations, LocalDate today) {
        Locale locale = Locale.forLanguageTag("es-CO");
        YearMonth currentMonth = YearMonth.from(today);
        List<YearMonth> months = new ArrayList<>();
        for (int offset = 5; offset >= 0; offset--) {
            months.add(currentMonth.minusMonths(offset));
        }

        double width = 100.0;
        double height = 40.0;
        double padding = 4.0;
        double step = months.size() == 1 ? 0.0 : (width - (padding * 2.0)) / (months.size() - 1);

        List<MonthlyPoint> points = new ArrayList<>();
        for (int index = 0; index < months.size(); index++) {
            YearMonth month = months.get(index);
            int total = 0;
            int completed = 0;

            for (TaxObligation obligation : obligations) {
                if (obligation == null || obligation.getDueDate() == null || isCancelled(obligation)) {
                    continue;
                }
                if (YearMonth.from(obligation.getDueDate()).equals(month)) {
                    total++;
                    if (isCompleted(obligation)) {
                        completed++;
                    }
                }
            }

            double compliance = total == 0 ? 0.0 : (completed * 100.0 / total);
            int x = months.size() == 1 ? 50 : (int) Math.round(padding + (step * index));
            int y = (int) Math.round(height - padding - (compliance / 100.0) * (height - (padding * 2.0)));
            points.add(new MonthlyPoint(
                    capitalize(month.getMonth().getDisplayName(TextStyle.SHORT, locale)),
                    formatPercent(compliance),
                    x,
                    y));
        }

        return points;
    }

    private static List<DistributionSlice> buildDistributionSlices(List<TaxObligation> obligations) {
        Map<TaxObligationType, Integer> typeCounts = new EnumMap<>(TaxObligationType.class);
        int total = 0;

        for (TaxObligation obligation : obligations) {
            if (obligation == null || obligation.getType() == null) {
                continue;
            }
            if (!isCancelled(obligation)) {
                typeCounts.merge(obligation.getType(), 1, Integer::sum);
                total++;
            }
        }

        List<TaxObligationType> order = List.of(
                TaxObligationType.INCOME_TAX,
                TaxObligationType.VAT,
                TaxObligationType.WITHHOLDING,
                TaxObligationType.INDUSTRY_COMMERCE,
                TaxObligationType.PATRIMONY);
        List<String> tones = List.of("primary", "warning", "success", "info", "danger");

        List<DistributionSlice> slices = new ArrayList<>();
        for (int index = 0; index < order.size(); index++) {
            TaxObligationType type = order.get(index);
            int count = typeCounts.getOrDefault(type, 0);
            double percent = total == 0 ? 0.0 : (count * 100.0 / total);
            slices.add(new DistributionSlice(
                    describeType(type),
                    formatCount(count) + " obligaciones • " + formatPercent(percent),
                    percent,
                    tones.get(index)));
        }

        return slices;
    }

    private static List<TaskRow> buildRecentTasks(List<TaxObligation> obligations, Map<String, TaxPayer> clientsById,
            LocalDate today) {
        return obligations.stream()
                .filter(obligation -> obligation != null && !isCancelled(obligation))
                .sorted(Comparator.comparingInt((TaxObligation obligation) -> taskOrder(obligation, today))
                        .thenComparing(TaxObligation::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(obligation -> describeType(obligation.getType())))
                .limit(8)
                .map(obligation -> {
                    String statusLabel = describeStatus(obligation.getStatus(), obligation, today);
                    String statusTone = statusTone(obligation, today);
                    String priorityLabel = priorityLabel(obligation, today);
                    String priorityTone = priorityTone(obligation, today);
                    return new TaskRow(
                            obligation.getTaxPayerId(),
                            resolveClientName(obligation, clientsById),
                            describeType(obligation.getType()) + " · " + obligation.getFiscalPeriod(),
                            responsibleFor(obligation.getType()),
                            priorityLabel,
                            priorityTone,
                            formatDate(obligation.getDueDate()),
                            statusLabel,
                            statusTone);
                })
                .toList();
    }

    private static List<CriticalClient> buildCriticalClients(List<TaxObligation> obligations,
            Map<String, TaxPayer> clientsById, LocalDate today) {
        Map<String, ClientStats> statsByClient = new LinkedHashMap<>();

        for (TaxObligation obligation : obligations) {
            if (obligation == null || obligation.getTaxPayerId() == null || isCancelled(obligation)) {
                continue;
            }

                String clientId = obligation.getTaxPayerId();
                ClientStats stats = statsByClient.computeIfAbsent(clientId,
                    id -> new ClientStats(id, resolveClientName(id, clientsById)));

            if (!isCompleted(obligation)) {
                stats.pendingCount++;
                if (isOverdue(obligation, today)) {
                    stats.overdueCount++;
                }
                if (obligation.getDueDate() != null
                        && (stats.nextDueDate == null || obligation.getDueDate().isBefore(stats.nextDueDate))) {
                    stats.nextDueDate = obligation.getDueDate();
                }
            }
        }

        return statsByClient.values().stream()
                .filter(stats -> stats.pendingCount > 0)
                .sorted(Comparator.comparingInt((ClientStats stats) -> stats.overdueCount * 10 + stats.pendingCount)
                        .reversed()
                        .thenComparing(stats -> stats.nextDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(stats -> stats.name))
                .limit(4)
                .map(stats -> {
                    String riskLabel = riskLabel(stats, today);
                    return new CriticalClient(
                            stats.clientId,
                            stats.name,
                            stats.pendingCount,
                            stats.nextDueDate != null ? formatDate(stats.nextDueDate) : "Sin fecha",
                            riskLabel,
                            riskTone(riskLabel));
                })
                .toList();
    }

    private static String buildLinePath(List<MonthlyPoint> points) {
        if (points.isEmpty()) {
            return "M 4 36 L 96 36";
        }

        StringBuilder path = new StringBuilder();
        for (int index = 0; index < points.size(); index++) {
            MonthlyPoint point = points.get(index);
            if (index == 0) {
                path.append("M ").append(point.x()).append(' ').append(point.y());
            } else {
                path.append(" L ").append(point.x()).append(' ').append(point.y());
            }
        }
        return path.toString();
    }

    private static String buildDistributionStyle(List<DistributionSlice> slices) {
        if (slices.isEmpty()) {
            return "conic-gradient(var(--border) 0deg 360deg)";
        }

        StringBuilder gradient = new StringBuilder("conic-gradient(");
        double start = 0.0;
        for (int index = 0; index < slices.size(); index++) {
            DistributionSlice slice = slices.get(index);
            double end = start + (slice.percent() * 3.6);
            if (index > 0) {
                gradient.append(", ");
            }
            gradient.append(colorForTone(slice.tone()))
                    .append(' ')
                    .append(formatAngle(start))
                    .append("deg ")
                    .append(formatAngle(end))
                    .append("deg");
            start = end;
        }
        gradient.append(')');
        return gradient.toString();
    }

    private static String formatAngle(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private static String formatPercent(double value) {
        return String.format(Locale.US, "%.1f%%", value);
    }

    private static String formatCount(long value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(value);
    }

    private static String formatMonthLabel(YearMonth month, Locale locale) {
        return capitalize(month.getMonth().getDisplayName(TextStyle.FULL, locale)) + " " + month.getYear();
    }

    private static String formatDate(LocalDate date) {
        if (date == null) {
            return "Sin fecha";
        }
        Locale locale = Locale.forLanguageTag("es-CO");
        return capitalize(date.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale)));
    }

    private static String resolveClientName(TaxObligation obligation, Map<String, TaxPayer> clientsById) {
        if (obligation == null || obligation.getTaxPayerId() == null) {
            return "Contribuyente";
        }
        return resolveClientName(obligation.getTaxPayerId(), clientsById);
    }

    private static String resolveClientName(String clientId, Map<String, TaxPayer> clientsById) {
        TaxPayer taxPayer = clientsById.get(clientId);
        if (taxPayer == null || taxPayer.getBusinessName() == null || taxPayer.getBusinessName().isBlank()) {
            return "Contribuyente";
        }
        return taxPayer.getBusinessName();
    }

    private static String describeType(TaxObligationType type) {
        if (type == null) {
            return "Obligación";
        }
        return switch (type) {
            case INCOME_TAX -> "Impuesto de renta";
            case VAT -> "IVA";
            case WITHHOLDING -> "Retención";
            case INDUSTRY_COMMERCE -> "Industria y comercio";
            case PATRIMONY -> "Patrimonio";
        };
    }

    private static String describeStatus(TaxObligationStatus status, TaxObligation obligation, LocalDate today) {
        if (isOverdue(obligation, today)) {
            return "Vencido";
        }
        if (status == null) {
            return "Pendiente";
        }
        return switch (status) {
            case PENDING -> "Pendiente";
            case IN_PROGRESS -> "En proceso";
            case COMPLETED -> "Completado";
            case OVERDUE -> "Vencido";
            case CANCELLED -> "Cancelado";
        };
    }

    private static boolean isCompleted(TaxObligation obligation) {
        return obligation != null && obligation.getStatus() == TaxObligationStatus.COMPLETED;
    }

    private static boolean isCancelled(TaxObligation obligation) {
        return obligation != null && obligation.getStatus() == TaxObligationStatus.CANCELLED;
    }

    private static boolean isOverdue(TaxObligation obligation, LocalDate today) {
        if (obligation == null) {
            return false;
        }
        TaxObligationStatus status = obligation.getStatus();
        if (status == TaxObligationStatus.COMPLETED || status == TaxObligationStatus.CANCELLED) {
            return false;
        }
        if (status == TaxObligationStatus.OVERDUE) {
            return true;
        }
        LocalDate dueDate = obligation.getDueDate();
        return dueDate != null && dueDate.isBefore(today);
    }

    private static String calendarTone(List<TaxObligation> dayObligations, LocalDate day, LocalDate today) {
        if (dayObligations.isEmpty()) {
            return "neutral";
        }

        boolean hasOverdue = false;
        boolean hasPending = false;
        boolean hasInProgress = false;
        boolean hasCompleted = false;

        for (TaxObligation obligation : dayObligations) {
            if (isOverdue(obligation, today)) {
                hasOverdue = true;
            } else if (obligation.getStatus() == TaxObligationStatus.PENDING) {
                hasPending = true;
            } else if (obligation.getStatus() == TaxObligationStatus.IN_PROGRESS) {
                hasInProgress = true;
            } else if (obligation.getStatus() == TaxObligationStatus.COMPLETED) {
                hasCompleted = true;
            }
        }

        if (hasOverdue || day.isBefore(today)) {
            return "danger";
        }
        if (day.isEqual(today) || hasPending) {
            return "warning";
        }
        if (hasInProgress) {
            return "primary";
        }
        if (hasCompleted) {
            return "success";
        }
        return "info";
    }

    private static String deadlineTone(TaxObligation obligation, LocalDate today) {
        if (obligation == null || obligation.getDueDate() == null) {
            return "neutral";
        }
        long daysUntil = ChronoUnit.DAYS.between(today, obligation.getDueDate());
        if (daysUntil <= 3) {
            return "danger";
        }
        if (daysUntil <= 7) {
            return "warning";
        }
        return "success";
    }

    private static String priorityLabel(TaxObligation obligation, LocalDate today) {
        if (obligation == null || isOverdue(obligation, today)) {
            return "Alta";
        }
        if (obligation.getDueDate() == null) {
            return "Media";
        }
        long daysUntil = ChronoUnit.DAYS.between(today, obligation.getDueDate());
        if (daysUntil <= 3) {
            return "Alta";
        }
        if (daysUntil <= 7) {
            return "Media";
        }
        return "Baja";
    }

    private static String priorityTone(TaxObligation obligation, LocalDate today) {
        String priority = priorityLabel(obligation, today);
        return switch (priority) {
            case "Alta" -> "danger";
            case "Media" -> "warning";
            default -> "success";
        };
    }

    private static String statusTone(TaxObligation obligation, LocalDate today) {
        if (isOverdue(obligation, today)) {
            return "danger";
        }
        TaxObligationStatus status = obligation != null ? obligation.getStatus() : null;
        if (status == null) {
            return "neutral";
        }
        return switch (status) {
            case PENDING -> "warning";
            case IN_PROGRESS -> "primary";
            case COMPLETED -> "success";
            case OVERDUE -> "danger";
            case CANCELLED -> "neutral";
        };
    }

    private static int taskOrder(TaxObligation obligation, LocalDate today) {
        if (isOverdue(obligation, today)) {
            return 0;
        }
        TaxObligationStatus status = obligation != null ? obligation.getStatus() : null;
        if (status == TaxObligationStatus.PENDING) {
            return 1;
        }
        if (status == TaxObligationStatus.IN_PROGRESS) {
            return 2;
        }
        if (status == TaxObligationStatus.COMPLETED) {
            return 3;
        }
        return 4;
    }

    private static String responsibleFor(TaxObligationType type) {
        if (type == null) {
            return "Equipo fiscal";
        }
        return switch (type) {
            case INCOME_TAX, WITHHOLDING -> "Contador";
            case VAT -> "Auxiliar contable";
            case INDUSTRY_COMMERCE, PATRIMONY -> "Gerencia";
        };
    }

    private static String riskLabel(ClientStats stats, LocalDate today) {
        if (stats.overdueCount >= 2 || stats.pendingCount >= 4
                || (stats.nextDueDate != null && !stats.nextDueDate.isAfter(today.plusDays(3)))) {
            return "Alto";
        }
        if (stats.pendingCount >= 2
                || (stats.nextDueDate != null && !stats.nextDueDate.isAfter(today.plusDays(10)))) {
            return "Medio";
        }
        return "Bajo";
    }

    private static String riskTone(String riskLabel) {
        return switch (riskLabel) {
            case "Alto" -> "danger";
            case "Medio" -> "warning";
            default -> "success";
        };
    }

    private static String colorForTone(String tone) {
        return switch (tone) {
            case "danger" -> "var(--danger)";
            case "warning" -> "var(--warning)";
            case "success" -> "var(--success)";
            case "info" -> "var(--primary-2)";
            case "primary" -> "var(--primary)";
            default -> "var(--border)";
        };
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String formatMonthShort(YearMonth month) {
        Locale locale = Locale.forLanguageTag("es-CO");
        return capitalize(month.getMonth().getDisplayName(TextStyle.SHORT, locale));
    }

    public record MetricCard(String label, String value, String detail, String tone, String variant) {
    }

    public record CalendarDay(int dayNumber, int taskCount, String tone, boolean currentMonth, boolean today) {
    }

        public record DeadlineItem(String clientId, String client, String obligation, String dueDateLabel,
            String statusLabel, String tone) {
    }

    public record WorkloadBar(String label, int value, double percent, String tone) {
    }

    public record MonthlyPoint(String label, String valueLabel, int x, int y) {
    }

    public record DistributionSlice(String label, String summary, double percent, String tone) {
    }

        public record TaskRow(String clientId, String client, String task, String responsible, String priority, String priorityTone,
            String dueDateLabel, String statusLabel, String statusTone) {
    }

        public record CriticalClient(String clientId, String name, int pendingCount, String nextDueDateLabel, String riskLabel,
            String riskTone) {
    }

    private static final class ClientStats {
        private final String clientId;
        private final String name;
        private int pendingCount;
        private int overdueCount;
        private LocalDate nextDueDate;

        private ClientStats(String clientId, String name) {
            this.clientId = clientId;
            this.name = name;
        }
    }
}