package com.cronos.gestiontributaria.common.view;

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

public final class PageViewModels {

    /**

     * Documentación de la entidad CalendarObligation.

     */

    public record CalendarObligation(
        String id,
        String clientName,
        String obligationType,
        String statusLabel,
        String statusTone
    ) {}

    public record CalendarDayDetail(
        int dayNumber,
        int taskCount,
        String tone,
        boolean currentMonth,
        boolean today,
        List<CalendarObligation> obligations
    ) {}

    public record MonthNav(
        String label,
        int year,
        int month
    ) {}

    public record TaskSummaryRow(
        String clientId,
        String clientName,
        String taskDesc,
        String responsible,
        String priority,
        String priorityTone,
        String dueDateLabel,
        String statusLabel,
        String statusTone
    ) {}

    public record TasksPageData(
        List<TaskSummaryRow> allTasks,
        List<DashboardSummary.CriticalClient> criticalClients,
        int pendingCount,
        int inProgressCount,
        int completedCount,
        int overdueCount
    ) {}

    public record DistributionSummary(
        String label,
        long count,
        double percent,
        String tone
    ) {}

    public record ReportsPageData(
        List<DashboardSummary.WorkloadBar> weeklyWorkload,
        List<DashboardSummary.MonthlyPoint> monthlyCompliance,
        String monthlyLinePath,
        List<DashboardSummary.DistributionSlice> distributionSlices,
        String distributionStyle,
        List<DistributionSummary> typeBreakdown,
        long totalObligations,
        long completedObligations,
        double compliancePercent
    ) {}

    public static List<CalendarDayDetail> buildCalendarDayDetails(
            List<TaxObligation> obligations, Map<String, TaxPayer> clientsById,
            LocalDate today, YearMonth month) {
        List<CalendarDayDetail> days = new ArrayList<>();
        LocalDate cursor = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = month.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        while (!cursor.isAfter(end)) {
            LocalDate currentDay = cursor;
            List<TaxObligation> dayObligations = obligations.stream()
                .filter(obl -> obl != null && obl.getDueDate() != null && obl.getDueDate().isEqual(currentDay))
                .toList();

            List<CalendarObligation> details = dayObligations.stream()
                .map(obl -> new CalendarObligation(
                    obl.getId(),
                    resolveClientName(obl, clientsById),
                    describeType(obl.getType()),
                    describeStatus(obl.getStatus(), obl, today),
                    statusTone(obl.getStatus(), obl, today)))
                .sorted(Comparator.comparing((CalendarObligation o) -> o.statusTone())
                    .thenComparing(o -> o.clientName()))
                .toList();

            int taskCount = dayObligations.size();
            String tone = calendarTone(dayObligations, currentDay, today);

            days.add(new CalendarDayDetail(
                currentDay.getDayOfMonth(), taskCount, tone,
                YearMonth.from(currentDay).equals(month),
                currentDay.isEqual(today),
                details));
            cursor = cursor.plusDays(1);
        }
        return days;
    }

    public static TasksPageData buildTasksPageData(
            List<TaxObligation> obligations, Map<String, TaxPayer> clientsById, LocalDate today) {
        List<TaskSummaryRow> allRows = new ArrayList<>();
        int pendingCount = 0;
        int inProgressCount = 0;
        int completedCount = 0;
        int overdueCount = 0;

        List<TaxObligation> sorted = obligations.stream()
            .filter(Objects::nonNull)
            .filter(o -> o.getStatus() != TaxObligationStatus.CANCELLED)
            .sorted(Comparator.comparingInt((TaxObligation o) -> taskOrder(o, today))
                .thenComparing(TaxObligation::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(o -> describeType(o.getType())))
            .toList();

        for (TaxObligation obl : sorted) {
            TaxObligationStatus status = obl.getStatus();
            boolean overdue = isOverdue(obl, today);
            if (overdue) overdueCount++;
            else if (status == TaxObligationStatus.PENDING) pendingCount++;
            else if (status == TaxObligationStatus.IN_PROGRESS) inProgressCount++;
            else if (status == TaxObligationStatus.COMPLETED) completedCount++;

            allRows.add(new TaskSummaryRow(
                obl.getTaxPayerId(),
                resolveClientName(obl, clientsById),
                describeType(obl.getType()) + " · " + (obl.getFiscalPeriod() != null ? obl.getFiscalPeriod() : ""),
                responsibleFor(obl.getType()),
                priorityLabel(obl, today),
                priorityTone(obl, today),
                formatDate(obl.getDueDate()),
                describeStatus(obl.getStatus(), obl, today),
                statusTone(obl.getStatus(), obl, today)));
        }

        return new TasksPageData(allRows, buildCriticalClients(obligations, clientsById, today),
            pendingCount, inProgressCount, completedCount, overdueCount);
    }

    public static ReportsPageData buildReportsPageData(
            List<TaxObligation> obligations, LocalDate today) {
        List<DashboardSummary.WorkloadBar> workload = buildWeeklyWorkload(obligations, today);
        List<DashboardSummary.MonthlyPoint> compliance = buildMonthlyCompliance(obligations, today);
        String linePath = buildLinePath(compliance);
        List<DashboardSummary.DistributionSlice> slices = buildDistributionSlices(obligations);
        String distStyle = buildDistributionStyle(slices);

        List<DistributionSummary> typeBreakdown = buildTypeBreakdown(obligations);

        long total = obligations.stream()
            .filter(o -> o != null && o.getStatus() != TaxObligationStatus.CANCELLED)
            .count();
        long completed = obligations.stream()
            .filter(o -> o != null && o.getStatus() == TaxObligationStatus.COMPLETED)
            .count();
        double complPct = total == 0 ? 100.0 : (completed * 100.0 / total);

        return new ReportsPageData(workload, compliance, linePath, slices, distStyle,
            typeBreakdown, total, completed, complPct);
    }

    // --- helpers ---

    private static String resolveClientName(TaxObligation obligation, Map<String, TaxPayer> clientsById) {
        if (obligation == null || obligation.getTaxPayerId() == null) return "Contribuyente";
        TaxPayer tp = clientsById.get(obligation.getTaxPayerId());
        if (tp == null || tp.getBusinessName() == null || tp.getBusinessName().isBlank()) return "Contribuyente";
        return tp.getBusinessName();
    }

    private static String describeType(TaxObligationType type) {
        if (type == null) return "Obligación";
        return switch (type) {
            case INCOME_TAX -> "Impuesto de renta";
            case VAT -> "IVA";
            case WITHHOLDING -> "Retención";
            case INDUSTRY_COMMERCE -> "Industria y comercio";
            case PATRIMONY -> "Patrimonio";
        };
    }

    private static String describeStatus(TaxObligationStatus status, TaxObligation obligation, LocalDate today) {
        if (isOverdue(obligation, today)) return "Vencido";
        if (status == null) return "Pendiente";
        return switch (status) {
            case PENDING -> "Pendiente";
            case IN_PROGRESS -> "En proceso";
            case COMPLETED -> "Completado";
            case OVERDUE -> "Vencido";
            case CANCELLED -> "Cancelado";
        };
    }

    private static String statusTone(TaxObligationStatus status, TaxObligation obligation, LocalDate today) {
        if (isOverdue(obligation, today)) return "danger";
        if (status == null) return "neutral";
        return switch (status) {
            case PENDING -> "warning";
            case IN_PROGRESS -> "primary";
            case COMPLETED -> "success";
            case OVERDUE -> "danger";
            case CANCELLED -> "neutral";
        };
    }

    private static boolean isOverdue(TaxObligation obligation, LocalDate today) {
        if (obligation == null) return false;
        TaxObligationStatus s = obligation.getStatus();
        if (s == TaxObligationStatus.COMPLETED || s == TaxObligationStatus.CANCELLED) return false;
        if (s == TaxObligationStatus.OVERDUE) return true;
        LocalDate due = obligation.getDueDate();
        return due != null && due.isBefore(today);
    }

    private static boolean isCompleted(TaxObligation obligation) {
        return obligation != null && obligation.getStatus() == TaxObligationStatus.COMPLETED;
    }

    private static boolean isCancelled(TaxObligation obligation) {
        return obligation != null && obligation.getStatus() == TaxObligationStatus.CANCELLED;
    }

    private static String calendarTone(List<TaxObligation> dayObligations, LocalDate day, LocalDate today) {
        if (dayObligations.isEmpty()) return "neutral";
        boolean hasOverdue = false, hasPending = false, hasInProgress = false, hasCompleted = false;
        for (TaxObligation o : dayObligations) {
            if (isOverdue(o, today)) hasOverdue = true;
            else if (o.getStatus() == TaxObligationStatus.PENDING) hasPending = true;
            else if (o.getStatus() == TaxObligationStatus.IN_PROGRESS) hasInProgress = true;
            else if (o.getStatus() == TaxObligationStatus.COMPLETED) hasCompleted = true;
        }
        if (hasOverdue || day.isBefore(today)) return "danger";
        if (day.isEqual(today) || hasPending) return "warning";
        if (hasInProgress) return "primary";
        if (hasCompleted) return "success";
        return "info";
    }

    private static List<DashboardSummary.WorkloadBar> buildWeeklyWorkload(List<TaxObligation> obligations, LocalDate today) {
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        String[] labels = { "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom" };
        int[] counts = new int[7];
        int max = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            int count = 0;
            for (TaxObligation o : obligations) {
                if (o != null && o.getDueDate() != null && o.getDueDate().isEqual(day)) count++;
            }
            counts[i] = count;
            max = Math.max(max, count);
        }
        List<DashboardSummary.WorkloadBar> bars = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            double pct = max == 0 ? 0.0 : (counts[i] * 100.0 / max);
            bars.add(new DashboardSummary.WorkloadBar(labels[i], counts[i], pct, "primary"));
        }
        return bars;
    }

    private static List<DashboardSummary.MonthlyPoint> buildMonthlyCompliance(List<TaxObligation> obligations, LocalDate today) {
        Locale locale = Locale.forLanguageTag("es-CO");
        YearMonth currentMonth = YearMonth.from(today);
        List<YearMonth> months = new ArrayList<>();
        for (int offset = 5; offset >= 0; offset--) months.add(currentMonth.minusMonths(offset));

        double width = 100.0, height = 40.0, padding = 4.0;
        double step = months.size() == 1 ? 0.0 : (width - (padding * 2.0)) / (months.size() - 1);

        List<DashboardSummary.MonthlyPoint> points = new ArrayList<>();
        for (int idx = 0; idx < months.size(); idx++) {
            YearMonth ym = months.get(idx);
            int total = 0, completed = 0;
            for (TaxObligation o : obligations) {
                if (o == null || o.getDueDate() == null || isCancelled(o)) continue;
                if (YearMonth.from(o.getDueDate()).equals(ym)) {
                    total++;
                    if (isCompleted(o)) completed++;
                }
            }
            double comp = total == 0 ? 0.0 : (completed * 100.0 / total);
            int x = months.size() == 1 ? 50 : (int) Math.round(padding + (step * idx));
            int y = (int) Math.round(height - padding - (comp / 100.0) * (height - (padding * 2.0)));
            points.add(new DashboardSummary.MonthlyPoint(
                capitalize(ym.getMonth().getDisplayName(TextStyle.SHORT, locale)),
                formatPercent(comp), x, y));
        }
        return points;
    }

    private static String buildLinePath(List<DashboardSummary.MonthlyPoint> points) {
        if (points.isEmpty()) return "M 4 36 L 96 36";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < points.size(); i++) {
            DashboardSummary.MonthlyPoint p = points.get(i);
            sb.append(i == 0 ? "M " : " L ").append(p.x()).append(' ').append(p.y());
        }
        return sb.toString();
    }

    private static List<DashboardSummary.DistributionSlice> buildDistributionSlices(List<TaxObligation> obligations) {
        Map<TaxObligationType, Integer> counts = new EnumMap<>(TaxObligationType.class);
        int total = 0;
        for (TaxObligation o : obligations) {
            if (o == null || o.getType() == null || isCancelled(o)) continue;
            counts.merge(o.getType(), 1, Integer::sum);
            total++;
        }
        List<TaxObligationType> order = List.of(
            TaxObligationType.INCOME_TAX, TaxObligationType.VAT,
            TaxObligationType.WITHHOLDING, TaxObligationType.INDUSTRY_COMMERCE,
            TaxObligationType.PATRIMONY);
        List<String> tones = List.of("primary", "warning", "success", "info", "danger");
        List<DashboardSummary.DistributionSlice> slices = new ArrayList<>();
        for (int i = 0; i < order.size(); i++) {
            int c = counts.getOrDefault(order.get(i), 0);
            double pct = total == 0 ? 0.0 : (c * 100.0 / total);
            slices.add(new DashboardSummary.DistributionSlice(
                describeType(order.get(i)),
                formatCount(c) + " obligaciones · " + formatPercent(pct), pct, tones.get(i)));
        }
        return slices;
    }

    private static String buildDistributionStyle(List<DashboardSummary.DistributionSlice> slices) {
        if (slices.isEmpty()) return "conic-gradient(var(--border) 0deg 360deg)";
        StringBuilder g = new StringBuilder("conic-gradient(");
        double start = 0.0;
        for (int i = 0; i < slices.size(); i++) {
            double end = start + (slices.get(i).percent() * 3.6);
            if (i > 0) g.append(", ");
            g.append(colorForTone(slices.get(i).tone()))
             .append(' ').append(formatAngle(start)).append("deg ")
             .append(formatAngle(end)).append("deg");
            start = end;
        }
        return g.append(')').toString();
    }

    private static List<DistributionSummary> buildTypeBreakdown(List<TaxObligation> obligations) {
        Map<TaxObligationType, Long> counts = obligations.stream()
            .filter(o -> o != null && o.getType() != null && !isCancelled(o))
            .collect(Collectors.groupingBy(TaxObligation::getType, () -> new EnumMap<>(TaxObligationType.class), Collectors.counting()));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        List<TaxObligationType> order = List.of(
            TaxObligationType.INCOME_TAX, TaxObligationType.VAT,
            TaxObligationType.WITHHOLDING, TaxObligationType.INDUSTRY_COMMERCE,
            TaxObligationType.PATRIMONY);
        List<String> tones = List.of("primary", "warning", "success", "info", "danger");
        List<DistributionSummary> result = new ArrayList<>();
        for (int i = 0; i < order.size(); i++) {
            long c = counts.getOrDefault(order.get(i), 0L);
            double pct = total == 0 ? 0.0 : (c * 100.0 / total);
            result.add(new DistributionSummary(describeType(order.get(i)), c, pct, tones.get(i)));
        }
        return result;
    }

    public static List<DashboardSummary.CriticalClient> buildCriticalClients(
            List<TaxObligation> obligations, Map<String, TaxPayer> clientsById, LocalDate today) {
        Map<String, ClientStats> stats = new LinkedHashMap<>();
        for (TaxObligation o : obligations) {
            if (o == null || o.getTaxPayerId() == null || isCancelled(o)) continue;
            ClientStats cs = stats.computeIfAbsent(o.getTaxPayerId(),
                id -> new ClientStats(id, resolveClientName(id, clientsById)));
            if (!isCompleted(o)) {
                cs.pendingCount++;
                if (isOverdue(o, today)) cs.overdueCount++;
                if (o.getDueDate() != null && (cs.nextDueDate == null || o.getDueDate().isBefore(cs.nextDueDate)))
                    cs.nextDueDate = o.getDueDate();
            }
        }
        return stats.values().stream()
            .filter(s -> s.pendingCount > 0)
            .sorted(Comparator.<ClientStats, Integer>comparing(s -> s.overdueCount * 10 + s.pendingCount).reversed()
                .thenComparing(s -> s.nextDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(s -> s.name))
            .limit(4)
            .map(s -> {
                String risk = riskLabel(s, today);
                return new DashboardSummary.CriticalClient(s.clientId, s.name, s.pendingCount,
                    s.nextDueDate != null ? formatDate(s.nextDueDate) : "Sin fecha", risk, riskTone(risk));
            })
            .toList();
    }

    private static String resolveClientName(String clientId, Map<String, TaxPayer> clientsById) {
        TaxPayer tp = clientsById.get(clientId);
        if (tp == null || tp.getBusinessName() == null || tp.getBusinessName().isBlank()) return "Contribuyente";
        return tp.getBusinessName();
    }

    private static int taskOrder(TaxObligation obligation, LocalDate today) {
        if (isOverdue(obligation, today)) return 0;
        TaxObligationStatus s = obligation != null ? obligation.getStatus() : null;
        if (s == TaxObligationStatus.PENDING) return 1;
        if (s == TaxObligationStatus.IN_PROGRESS) return 2;
        if (s == TaxObligationStatus.COMPLETED) return 3;
        return 4;
    }

    private static String priorityLabel(TaxObligation obligation, LocalDate today) {
        if (obligation == null || isOverdue(obligation, today)) return "Alta";
        if (obligation.getDueDate() == null) return "Media";
        long d = ChronoUnit.DAYS.between(today, obligation.getDueDate());
        if (d <= 3) return "Alta";
        if (d <= 7) return "Media";
        return "Baja";
    }

    private static String priorityTone(TaxObligation obligation, LocalDate today) {
        return switch (priorityLabel(obligation, today)) {
            case "Alta" -> "danger";
            case "Media" -> "warning";
            default -> "success";
        };
    }

    private static String responsibleFor(TaxObligationType type) {
        if (type == null) return "Equipo fiscal";
        return switch (type) {
            case INCOME_TAX, WITHHOLDING -> "Contador";
            case VAT -> "Auxiliar contable";
            case INDUSTRY_COMMERCE, PATRIMONY -> "Gerencia";
        };
    }

    private static String riskLabel(ClientStats stats, LocalDate today) {
        if (stats.overdueCount >= 2 || stats.pendingCount >= 4
            || (stats.nextDueDate != null && !stats.nextDueDate.isAfter(today.plusDays(3))))
            return "Alto";
        if (stats.pendingCount >= 2
            || (stats.nextDueDate != null && !stats.nextDueDate.isAfter(today.plusDays(10))))
            return "Medio";
        return "Bajo";
    }

    private static String riskTone(String risk) {
        return switch (risk) {
            case "Alto" -> "danger";
            case "Medio" -> "warning";
            default -> "success";
        };
    }

    private static String formatDate(LocalDate date) {
        if (date == null) return "Sin fecha";
        return capitalize(date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-CO"))));
    }

    private static String formatPercent(double v) {
        return String.format(Locale.US, "%.1f%%", v);
    }

    private static String formatCount(long v) {
        return java.text.NumberFormat.getIntegerInstance(Locale.US).format(v);
    }

    private static String formatAngle(double v) {
        return String.format(Locale.US, "%.1f", v);
    }

    private static String capitalize(String v) {
        if (v == null || v.isBlank()) return v;
        return Character.toUpperCase(v.charAt(0)) + v.substring(1);
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

    private static final class ClientStats {
        final String clientId;
        final String name;
        int pendingCount;
        int overdueCount;
        LocalDate nextDueDate;
        ClientStats(String clientId, String name) { this.clientId = clientId; this.name = name; }
    }
}
