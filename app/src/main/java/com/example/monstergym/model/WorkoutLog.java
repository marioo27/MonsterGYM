package com.example.monstergym.model;

import java.io.Serializable;

public class WorkoutLog implements Serializable {
    private String date; // Format YYYY-MM-DD
    private String routineId;
    private String routineName;
    private String notes;
    private long timestamp;
    private String completedVariations; // Comma-separated or JSON list of completed variation IDs / names

    public WorkoutLog() {}

    public WorkoutLog(String date, String routineId, String routineName, String notes, long timestamp) {
        this(date, routineId, routineName, notes, timestamp, "");
    }

    public WorkoutLog(String date, String routineId, String routineName, String notes, long timestamp, String completedVariations) {
        this.date = date;
        this.routineId = routineId;
        this.routineName = routineName;
        this.notes = notes;
        this.timestamp = timestamp;
        this.completedVariations = completedVariations;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getRoutineId() {
        return routineId;
    }

    public void setRoutineId(String routineId) {
        this.routineId = routineId;
    }

    public String getRoutineName() {
        return routineName;
    }

    public void setRoutineName(String routineName) {
        this.routineName = routineName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getCompletedVariations() {
        return completedVariations != null ? completedVariations : "";
    }

    public void setCompletedVariations(String completedVariations) {
        this.completedVariations = completedVariations;
    }
}