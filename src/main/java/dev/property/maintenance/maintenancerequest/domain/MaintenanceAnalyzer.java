package dev.property.maintenance.maintenancerequest.domain;

public interface MaintenanceAnalyzer {

    MaintenanceAnalysisResult analyze(String description, ReportedUrgency reportedUrgency);
}
