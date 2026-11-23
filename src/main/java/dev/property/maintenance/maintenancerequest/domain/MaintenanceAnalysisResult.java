package dev.property.maintenance.maintenancerequest.domain;

public record MaintenanceAnalysisResult(
        MaintenanceCategory category,
        SuggestedPriority suggestedPriority,
        Severity severity,
        String summary
) {
}
