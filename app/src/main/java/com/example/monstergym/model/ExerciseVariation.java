package com.example.monstergym.model;

import android.content.Context;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExerciseVariation implements Serializable {
    private String id;
    private String name;
    private int nameResId;
    private int sets;
    private int repetitions;
    private double weight;
    private List<Double> setWeights;
    private String weightUnit; // "kg" or "lb"
    private int restSeconds;
    private String notes;
    private int notesResId;
    private int imageResource;
    private boolean hasWarmupSet;
    private double warmupWeight;
    private int warmupReps;

    public ExerciseVariation() {
        this.weightUnit = "kg";
        this.sets = 3;
        this.repetitions = 10;
        this.restSeconds = 90;
        this.setWeights = new ArrayList<>();
        for (int i = 0; i < sets; i++) {
            this.setWeights.add(0.0);
        }
    }

    public ExerciseVariation(String id, String name, int nameResId, int sets, int repetitions,
                             double weight, String weightUnit, int restSeconds, String notes,
                             int notesResId, int imageResource) {
        this.id = id;
        this.name = name;
        this.nameResId = nameResId;
        this.sets = sets;
        this.repetitions = repetitions;
        this.weight = weight;
        this.weightUnit = weightUnit != null ? weightUnit : "kg";
        this.restSeconds = restSeconds;
        this.notes = notes;
        this.notesResId = notesResId;
        this.imageResource = imageResource;
        this.setWeights = new ArrayList<>();
        for (int i = 0; i < sets; i++) {
            this.setWeights.add(weight);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNameResId() {
        return nameResId;
    }

    public void setNameResId(int nameResId) {
        this.nameResId = nameResId;
    }

    public String getDisplayName(Context context) {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        if (nameResId != 0 && context != null) {
            try {
                return context.getString(nameResId);
            } catch (Exception e) {
                // fallback to name
            }
        }
        return "";
    }

    public int getSets() {
        return sets;
    }

    public void setSets(int sets) {
        this.sets = sets;
    }

    public int getRepetitions() {
        return repetitions;
    }

    public void setRepetitions(int repetitions) {
        this.repetitions = repetitions;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public String getWeightUnit() {
        return weightUnit;
    }

    public void setWeightUnit(String weightUnit) {
        this.weightUnit = weightUnit;
    }

    public int getRestSeconds() {
        return restSeconds;
    }

    public void setRestSeconds(int restSeconds) {
        this.restSeconds = restSeconds;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getNotesResId() {
        return notesResId;
    }

    public void setNotesResId(int notesResId) {
        this.notesResId = notesResId;
    }

    public String getDisplayNotes(Context context) {
        if (notesResId != 0 && context != null) {
            try {
                return context.getString(notesResId);
            } catch (Exception e) {
                // fallback to notes
            }
        }
        return notes != null ? notes : "";
    }

    public int getImageResource() {
        return imageResource;
    }

    public void setImageResource(int imageResource) {
        this.imageResource = imageResource;
    }

    public List<Double> getSetWeights() {
        if (setWeights == null) {
            setWeights = new ArrayList<>();
        }
        while (setWeights.size() < sets) {
            setWeights.add(0.0);
        }
        while (setWeights.size() > sets && sets > 0) {
            setWeights.remove(setWeights.size() - 1);
        }
        return setWeights;
    }

    public void setSetWeights(List<Double> setWeights) {
        this.setWeights = setWeights;
        if (setWeights != null && !setWeights.isEmpty()) {
            this.weight = setWeights.get(0);
        }
    }

    public String getDisplayWeights() {
        List<Double> weights = getSetWeights();
        if (weights == null || weights.isEmpty()) return "0";
        boolean allSame = true;
        double first = weights.get(0);
        for (double w : weights) {
            if (Double.compare(w, first) != 0) {
                allSame = false;
                break;
            }
        }
        if (allSame) {
            return formatDouble(first);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < weights.size(); i++) {
                if (i > 0) sb.append(" / ");
                sb.append(formatDouble(weights.get(i)));
            }
            return sb.toString();
        }
    }

    public boolean hasWarmupSet() {
        return hasWarmupSet;
    }

    public void setHasWarmupSet(boolean hasWarmupSet) {
        this.hasWarmupSet = hasWarmupSet;
    }

    public double getWarmupWeight() {
        return warmupWeight;
    }

    public void setWarmupWeight(double warmupWeight) {
        this.warmupWeight = warmupWeight;
    }

    public int getWarmupReps() {
        return warmupReps <= 0 ? 10 : warmupReps;
    }

    public void setWarmupReps(int warmupReps) {
        this.warmupReps = warmupReps;
    }

    public String formatDouble(double val) {
        if (val == (long) val) {
            return String.format(Locale.US, "%d", (long) val);
        } else {
            return String.format(Locale.US, "%.1f", val);
        }
    }
}