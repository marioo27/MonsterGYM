package com.example.monstergym.data;

import android.content.Context;
import android.text.TextUtils;
import com.example.monstergym.R;
import com.example.monstergym.model.Exercise;
import com.example.monstergym.model.ExerciseVariation;
import com.example.monstergym.model.Routine;
import com.example.monstergym.model.WorkoutLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorkoutDataManager {

    private static WorkoutDataManager instance;
    private final List<Routine> routines;
    private final GymDatabaseHelper dbHelper;
    private final Context appContext;

    private WorkoutDataManager(Context context) {
        this.appContext = context != null ? context.getApplicationContext() : null;
        this.routines = new ArrayList<>();
        this.dbHelper = appContext != null ? new GymDatabaseHelper(appContext) : null;
        loadData(appContext);
    }

    public static synchronized WorkoutDataManager getInstance(Context context) {
        if (instance == null) {
            instance = new WorkoutDataManager(context != null ? context.getApplicationContext() : null);
        }
        return instance;
    }

    public static synchronized WorkoutDataManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("WorkoutDataManager must be initialized with context first.");
        }
        return instance;
    }

    public synchronized void resetRoutinesFromAssetJson() {
        if (appContext == null) return;
        if (dbHelper != null) {
            dbHelper.saveAllRoutines(new ArrayList<>());
        }
        routines.clear();
        loadDataFromJson(appContext);
        if (dbHelper != null && !routines.isEmpty()) {
            dbHelper.saveAllRoutines(routines);
            exportJsonFile();
        }
    }

    private void loadData(Context context) {
        if (context == null) return;

        if (dbHelper != null && !dbHelper.isDatabaseEmpty()) {
            routines.clear();
            routines.addAll(dbHelper.getAllRoutines());
            return;
        }

        // Initialize from assets if database is empty
        loadDataFromJson(context);
        if (dbHelper != null && !routines.isEmpty()) {
            dbHelper.saveAllRoutines(routines);
            exportJsonFile();
        }
    }

    private void loadDataFromJson(Context context) {
        if (context == null) return;
        try {
            InputStream is = context.getAssets().open("gymRoutine.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            int bytesRead = is.read(buffer);
            is.close();
            if (bytesRead <= 0) return;
            String jsonString = new String(buffer, StandardCharsets.UTF_8);

            JSONObject root = new JSONObject(jsonString);
            JSONArray rutinasArray = root.optJSONArray("routines") != null ? root.optJSONArray("routines") : root.optJSONArray("rutinas");
            if (rutinasArray == null) return;

            for (int i = 0; i < rutinasArray.length(); i++) {
                JSONObject rutObj = rutinasArray.getJSONObject(i);
                String id = rutObj.optString("id", UUID.randomUUID().toString());
                String nombre = rutObj.has("name") ? rutObj.optString("name", "") : rutObj.optString("nombre", "");
                String descripcion = rutObj.has("description") ? rutObj.optString("description", "") : rutObj.optString("descripcion", "");

                List<Exercise> exercises = new ArrayList<>();

                // Parse regular exercises
                JSONArray exArray = rutObj.optJSONArray("exercises") != null ? rutObj.optJSONArray("exercises") : rutObj.optJSONArray("ejercicios");
                if (exArray != null) {
                    for (int j = 0; j < exArray.length(); j++) {
                        Exercise ex = parseExercise(exArray.optJSONObject(j), false);
                        if (ex != null) exercises.add(ex);
                    }
                }

                // Parse extra exercises
                JSONArray extraExArray = rutObj.optJSONArray("extra_exercises") != null ? rutObj.optJSONArray("extra_exercises") : rutObj.optJSONArray("ejercicios_extra");
                if (extraExArray != null) {
                    for (int j = 0; j < extraExArray.length(); j++) {
                        Exercise ex = parseExercise(extraExArray.optJSONObject(j), true);
                        if (ex != null) exercises.add(ex);
                    }
                }

                String abbreviation = rutObj.has("abbreviation") ? rutObj.optString("abbreviation", "") : rutObj.optString("abreviatura", "");

                int nameResId = 0;
                int descResId = 0;
                if ("rutina_tren_superior".equals(id)) {
                    nameResId = R.string.routine_upper_body;
                    descResId = R.string.routine_desc_upper_body;
                } else if ("rutina_tren_inferior".equals(id)) {
                    nameResId = R.string.routine_lower_body;
                    descResId = R.string.routine_desc_lower_body;
                }

                String imageName = rutObj.has("image") ? rutObj.optString("image", "") : rutObj.optString("imagen", "");
                int imageRes = getDrawableResId(context, imageName, R.drawable.devil_mad_red);

                Routine routine = new Routine(
                        id,
                        nombre, nameResId,
                        descripcion, descResId,
                        nombre, 0,
                        imageRes,
                        abbreviation,
                        exercises
                );
                routines.add(routine);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Exercise parseExercise(JSONObject exObj, boolean isExtra) {
        if (exObj == null) return null;
        String id = exObj.optString("id", UUID.randomUUID().toString());
        String nombre = exObj.has("name") ? exObj.optString("name", "") : exObj.optString("nombre", "");

        List<ExerciseVariation> variations = new ArrayList<>();
        JSONArray varArray = exObj.optJSONArray("variations") != null ? exObj.optJSONArray("variations") : exObj.optJSONArray("variaciones");
        if (varArray != null) {
            for (int i = 0; i < varArray.length(); i++) {
                ExerciseVariation var = parseVariation(varArray.optJSONObject(i));
                if (var != null) variations.add(var);
            }
        }

        String imageName = exObj.has("image") ? exObj.optString("image", "") : exObj.optString("imagen", "");
        int imageRes = getDrawableResId(appContext, imageName, R.drawable.devil_mad_red);

        return new Exercise(
                id,
                nombre, 0,
                "", 0,
                imageRes,
                variations,
                isExtra
        );
    }

    private ExerciseVariation parseVariation(JSONObject varObj) {
        if (varObj == null) return null;
        String id = varObj.optString("id", UUID.randomUUID().toString());
        String nombre = varObj.has("name") ? varObj.optString("name", "") : varObj.optString("nombre", "");
        String notas = varObj.has("notes") ? varObj.optString("notes", "") : varObj.optString("notas", "");

        List<Double> setWeights = new ArrayList<>();
        int repetitions = 10;
        int restSeconds = 90;

        JSONArray seriesArray = varObj.optJSONArray("sets") != null ? varObj.optJSONArray("sets") : varObj.optJSONArray("series");
        if (seriesArray != null && seriesArray.length() > 0) {
            for (int i = 0; i < seriesArray.length(); i++) {
                JSONObject setObj = seriesArray.optJSONObject(i);
                if (setObj != null) {
                    double pesoKg = setObj.has("weight_kg") ? setObj.optDouble("weight_kg", 0.0) : setObj.optDouble("peso_kg", 0.0);
                    int reps = setObj.has("reps") ? setObj.optInt("reps", 10) : (setObj.has("repetitions") ? setObj.optInt("repetitions", 10) : setObj.optInt("repeticiones", 10));
                    if (i == 0) {
                        repetitions = reps;
                    }
                    setWeights.add(pesoKg);
                }
            }
        } else {
            setWeights.add(0.0);
            setWeights.add(0.0);
            setWeights.add(0.0);
        }

        int sets = setWeights.size();
        double primaryWeight = !setWeights.isEmpty() ? setWeights.get(0) : 0.0;

        String imageName = varObj.has("image") ? varObj.optString("image", "") : varObj.optString("imagen", "");
        int imageRes = getDrawableResId(appContext, imageName, R.drawable.devil_mad_red);

        ExerciseVariation variation = new ExerciseVariation(
                id,
                nombre, 0,
                sets,
                repetitions,
                primaryWeight,
                "kg",
                restSeconds,
                notas,
                0,
                imageRes
        );
        variation.setSetWeights(setWeights);
        return variation;
    }

    public synchronized List<Routine> getRoutines() {
        return new ArrayList<>(routines);
    }

    public synchronized Routine getRoutineById(String id) {
        if (id == null) return null;
        for (Routine routine : routines) {
            if (id.equals(routine.getId())) {
                return routine;
            }
        }
        return null;
    }

    public synchronized void addRoutine(Routine routine) {
        if (routine != null) {
            if (routine.getId() == null) {
                routine.setId(UUID.randomUUID().toString());
            }
            routines.add(routine);
            if (dbHelper != null) {
                dbHelper.insertRoutine(routine, routines.size() - 1);
            }
            exportJsonFile();
        }
    }

    public synchronized void updateRoutine(Routine updated) {
        if (updated == null || updated.getId() == null) return;
        for (int i = 0; i < routines.size(); i++) {
            if (updated.getId().equals(routines.get(i).getId())) {
                routines.set(i, updated);
                if (dbHelper != null) {
                    dbHelper.updateRoutine(updated);
                }
                exportJsonFile();
                return;
            }
        }
    }

    public synchronized void deleteRoutine(String routineId) {
        if (routineId == null) return;
        for (int i = 0; i < routines.size(); i++) {
            if (routineId.equals(routines.get(i).getId())) {
                routines.remove(i);
                if (dbHelper != null) {
                    dbHelper.deleteRoutine(routineId);
                }
                exportJsonFile();
                return;
            }
        }
    }

    public synchronized Routine duplicateRoutine(Routine source) {
        if (source == null) return null;
        List<Exercise> clonedExercises = new ArrayList<>();
        if (source.getExercises() != null) {
            for (Exercise ex : source.getExercises()) {
                List<ExerciseVariation> clonedVars = new ArrayList<>();
                if (ex.getVariations() != null) {
                    for (ExerciseVariation var : ex.getVariations()) {
                        List<Double> clonedWeights = new ArrayList<>(var.getSetWeights());
                        ExerciseVariation clonedVar = new ExerciseVariation(
                                UUID.randomUUID().toString(),
                                var.getName(), var.getNameResId(),
                                var.getSets(), var.getRepetitions(),
                                var.getWeight(), var.getWeightUnit(),
                                var.getRestSeconds(), var.getNotes(),
                                var.getNotesResId(), var.getImageResource()
                        );
                        clonedVar.setSetWeights(clonedWeights);
                        clonedVars.add(clonedVar);
                    }
                }
                Exercise clonedEx = new Exercise(
                        UUID.randomUUID().toString(),
                        ex.getName(), ex.getNameResId(),
                        ex.getMuscleGroup(), ex.getMuscleGroupResId(),
                        ex.getImageResource(), clonedVars, ex.isExtra()
                );
                clonedExercises.add(clonedEx);
            }
        }
        Routine duplicated = new Routine(
                UUID.randomUUID().toString(),
                source.getName() + " (Copia)", 0,
                source.getDescription(), 0,
                source.getMuscleGroup(), 0,
                source.getImageResource(),
                clonedExercises
        );
        addRoutine(duplicated);
        return duplicated;
    }

    // Exercise operations
    public synchronized Exercise getExerciseById(String routineId, String exerciseId) {
        Routine routine = getRoutineById(routineId);
        if (routine == null || exerciseId == null) return null;
        for (Exercise exercise : routine.getExercises()) {
            if (exerciseId.equals(exercise.getId())) {
                return exercise;
            }
        }
        return null;
    }

    public synchronized void addExercise(String routineId, Exercise exercise) {
        Routine routine = getRoutineById(routineId);
        if (routine != null && exercise != null) {
            if (exercise.getId() == null) {
                exercise.setId(UUID.randomUUID().toString());
            }
            routine.getExercises().add(exercise);
            if (dbHelper != null) {
                dbHelper.insertExercise(routineId, exercise, routine.getExercises().size() - 1);
            }
            exportJsonFile();
        }
    }

    public synchronized void updateExercise(String routineId, Exercise updatedExercise) {
        Routine routine = getRoutineById(routineId);
        if (routine != null && updatedExercise != null && updatedExercise.getId() != null) {
            List<Exercise> exercises = routine.getExercises();
            for (int i = 0; i < exercises.size(); i++) {
                if (updatedExercise.getId().equals(exercises.get(i).getId())) {
                    exercises.set(i, updatedExercise);
                    if (dbHelper != null) {
                        dbHelper.updateExercise(updatedExercise);
                    }
                    exportJsonFile();
                    return;
                }
            }
        }
    }

    public synchronized void deleteExercise(String routineId, String exerciseId) {
        Routine routine = getRoutineById(routineId);
        if (routine != null && exerciseId != null) {
            List<Exercise> exercises = routine.getExercises();
            for (int i = 0; i < exercises.size(); i++) {
                if (exerciseId.equals(exercises.get(i).getId())) {
                    exercises.remove(i);
                    if (dbHelper != null) {
                        dbHelper.deleteExercise(exerciseId);
                    }
                    exportJsonFile();
                    return;
                }
            }
        }
    }

    // Variation operations
    public synchronized ExerciseVariation getVariationById(String routineId, String exerciseId, String variationId) {
        Exercise exercise = getExerciseById(routineId, exerciseId);
        if (exercise == null || variationId == null) return null;
        for (ExerciseVariation var : exercise.getVariations()) {
            if (variationId.equals(var.getId())) {
                return var;
            }
        }
        return null;
    }

    public synchronized void addVariation(String routineId, String exerciseId, ExerciseVariation variation) {
        Exercise exercise = getExerciseById(routineId, exerciseId);
        if (exercise != null && variation != null) {
            if (variation.getId() == null) {
                variation.setId(UUID.randomUUID().toString());
            }
            exercise.getVariations().add(variation);
            if (dbHelper != null) {
                dbHelper.insertVariation(exerciseId, variation, exercise.getVariations().size() - 1);
            }
            exportJsonFile();
        }
    }

    public synchronized void updateVariation(String routineId, String exerciseId, ExerciseVariation updatedVariation) {
        Exercise exercise = getExerciseById(routineId, exerciseId);
        if (exercise != null && updatedVariation != null && updatedVariation.getId() != null) {
            List<ExerciseVariation> variations = exercise.getVariations();
            for (int i = 0; i < variations.size(); i++) {
                if (updatedVariation.getId().equals(variations.get(i).getId())) {
                    variations.set(i, updatedVariation);
                    if (dbHelper != null) {
                        dbHelper.updateVariation(updatedVariation);
                    }
                    exportJsonFile();
                    return;
                }
            }
        }
    }

    public synchronized void deleteVariation(String routineId, String exerciseId, String variationId) {
        Exercise exercise = getExerciseById(routineId, exerciseId);
        if (exercise != null && variationId != null) {
            List<ExerciseVariation> variations = exercise.getVariations();
            for (int i = 0; i < variations.size(); i++) {
                if (variationId.equals(variations.get(i).getId())) {
                    variations.remove(i);
                    if (dbHelper != null) {
                        dbHelper.deleteVariation(variationId);
                    }
                    exportJsonFile();
                    return;
                }
            }
        }
    }

    public synchronized void reorderRoutines(List<Routine> reorderedList) {
        if (reorderedList == null) return;
        routines.clear();
        routines.addAll(reorderedList);
        if (dbHelper != null) {
            dbHelper.updateRoutinePositions(routines);
        }
        exportJsonFile();
    }

    public synchronized void reorderExercises(String routineId, List<Exercise> reorderedList) {
        Routine routine = getRoutineById(routineId);
        if (routine != null && reorderedList != null) {
            routine.setExercises(reorderedList);
            if (dbHelper != null) {
                dbHelper.updateExercisePositions(routineId, reorderedList);
            }
            exportJsonFile();
        }
    }

    public synchronized void reorderVariations(String routineId, String exerciseId, List<ExerciseVariation> reorderedList) {
        Exercise exercise = getExerciseById(routineId, exerciseId);
        if (exercise != null && reorderedList != null) {
            exercise.setVariations(reorderedList);
            if (dbHelper != null) {
                dbHelper.updateVariationPositions(exerciseId, reorderedList);
            }
            exportJsonFile();
        }
    }

    public synchronized void saveWorkoutLog(WorkoutLog log) {
        if (dbHelper != null) {
            dbHelper.saveWorkoutLog(log);
        }
    }

    public synchronized void deleteWorkoutLog(String date) {
        if (dbHelper != null) {
            dbHelper.deleteWorkoutLog(date);
        }
    }

    public synchronized WorkoutLog getWorkoutLog(String date) {
        if (dbHelper != null) {
            return dbHelper.getWorkoutLog(date);
        }
        return null;
    }

    public synchronized java.util.Map<String, WorkoutLog> getAllWorkoutLogsMap() {
        if (dbHelper != null) {
            return dbHelper.getAllWorkoutLogsMap();
        }
        return new java.util.HashMap<>();
    }

    public synchronized boolean importJsonData(String jsonString) {
        if (TextUtils.isEmpty(jsonString)) return false;
        try {
            JSONObject rootObj = new JSONObject(jsonString);
            JSONArray rutinasArray = rootObj.optJSONArray("routines") != null ? rootObj.optJSONArray("routines") : rootObj.optJSONArray("rutinas");
            if (rutinasArray == null) return false;

            if (dbHelper != null) {
                dbHelper.saveAllRoutines(new ArrayList<>());
            }
            routines.clear();

            for (int i = 0; i < rutinasArray.length(); i++) {
                JSONObject rutObj = rutinasArray.getJSONObject(i);
                String id = rutObj.optString("id", UUID.randomUUID().toString());
                String nombre = rutObj.has("name") ? rutObj.optString("name", "") : rutObj.optString("nombre", "");
                String descripcion = rutObj.has("description") ? rutObj.optString("description", "") : rutObj.optString("descripcion", "");
                String abbrev = rutObj.has("abbreviation") ? rutObj.optString("abbreviation", "") : rutObj.optString("abreviatura", "");

                List<Exercise> exercises = new ArrayList<>();
                JSONArray exArray = rutObj.optJSONArray("exercises") != null ? rutObj.optJSONArray("exercises") : rutObj.optJSONArray("ejercicios");
                if (exArray != null) {
                    for (int j = 0; j < exArray.length(); j++) {
                        Exercise ex = parseExercise(exArray.optJSONObject(j), false);
                        if (ex != null) exercises.add(ex);
                    }
                }
                JSONArray extraExArray = rutObj.optJSONArray("extra_exercises") != null ? rutObj.optJSONArray("extra_exercises") : rutObj.optJSONArray("ejercicios_extra");
                if (extraExArray != null) {
                    for (int j = 0; j < extraExArray.length(); j++) {
                        Exercise ex = parseExercise(extraExArray.optJSONObject(j), true);
                        if (ex != null) exercises.add(ex);
                    }
                }

                String imageName = rutObj.has("image") ? rutObj.optString("image", "") : rutObj.optString("imagen", "");
                int imageRes = getDrawableResId(appContext, imageName, R.drawable.devil_mad_red);

                Routine routine = new Routine(
                        id,
                        nombre, 0,
                        descripcion, 0,
                        nombre, 0,
                        imageRes,
                        abbrev,
                        exercises
                );
                routines.add(routine);
                if (dbHelper != null) {
                    dbHelper.insertRoutine(routine, i);
                }
            }

            // Parse workout_logs if present
            JSONArray logsArray = rootObj.optJSONArray("workout_logs") != null ? rootObj.optJSONArray("workout_logs") : rootObj.optJSONArray("logs");
            if (logsArray != null) {
                for (int i = 0; i < logsArray.length(); i++) {
                    JSONObject logObj = logsArray.optJSONObject(i);
                    if (logObj != null) {
                        String date = logObj.optString("date", "");
                        if (!TextUtils.isEmpty(date)) {
                            String rId = logObj.has("routine_id") ? logObj.optString("routine_id", "") : logObj.optString("routineId", "");
                            String rName = logObj.has("routine_name") ? logObj.optString("routine_name", "") : logObj.optString("routineName", "");
                            String notes = logObj.optString("notes", "");
                            long timestamp = logObj.optLong("timestamp", System.currentTimeMillis());
                            com.example.monstergym.model.WorkoutLog log = new com.example.monstergym.model.WorkoutLog(date, rId, rName, notes, timestamp);
                            saveWorkoutLog(log);
                        }
                    }
                }
            }

            exportJsonFile();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public synchronized String generateJsonString() {
        try {
            JSONObject root = new JSONObject();
            JSONArray rutinasArray = new JSONArray();

            for (Routine r : routines) {
                JSONObject rutObj = new JSONObject();
                rutObj.put("id", r.getId());
                rutObj.put("name", r.getName() != null ? r.getName() : "");
                rutObj.put("description", r.getDescription() != null ? r.getDescription() : "");
                rutObj.put("abbreviation", r.getAbbreviation() != null ? r.getAbbreviation() : "");
                rutObj.put("image", getImageName(appContext, r.getImageResource()));

                JSONArray ejerciciosArray = new JSONArray();
                JSONArray ejerciciosExtraArray = new JSONArray();

                if (r.getExercises() != null) {
                    for (Exercise ex : r.getExercises()) {
                        JSONObject exObj = new JSONObject();
                        exObj.put("id", ex.getId());
                        exObj.put("name", ex.getName() != null ? ex.getName() : "");
                        exObj.put("image", getImageName(appContext, ex.getImageResource()));

                        JSONArray variacionesArray = new JSONArray();
                        if (ex.getVariations() != null) {
                            for (ExerciseVariation var : ex.getVariations()) {
                                JSONObject varObj = new JSONObject();
                                varObj.put("id", var.getId());
                                varObj.put("name", var.getName() != null ? var.getName() : "");
                                varObj.put("notes", var.getNotes() != null ? var.getNotes() : "");
                                varObj.put("image", getImageName(appContext, var.getImageResource()));

                                JSONArray seriesArray = new JSONArray();
                                List<Double> weights = var.getSetWeights();
                                if (weights != null) {
                                    for (int sIdx = 0; sIdx < weights.size(); sIdx++) {
                                        JSONObject setObj = new JSONObject();
                                        setObj.put("number", sIdx + 1);
                                        setObj.put("weight_kg", weights.get(sIdx));
                                        setObj.put("reps", var.getRepetitions());
                                        seriesArray.put(setObj);
                                    }
                                }
                                varObj.put("sets", seriesArray);
                                variacionesArray.put(varObj);
                            }
                        }
                        exObj.put("variations", variacionesArray);

                        if (ex.isExtra()) {
                            ejerciciosExtraArray.put(exObj);
                        } else {
                            ejerciciosArray.put(exObj);
                        }
                    }
                }
                rutObj.put("exercises", ejerciciosArray);
                if (ejerciciosExtraArray.length() > 0) {
                    rutObj.put("extra_exercises", ejerciciosExtraArray);
                }
                rutinasArray.put(rutObj);
            }
            root.put("routines", rutinasArray);

            java.util.Map<String, com.example.monstergym.model.WorkoutLog> logsMap = getAllWorkoutLogsMap();
            if (logsMap != null && !logsMap.isEmpty()) {
                JSONArray logsArray = new JSONArray();
                for (com.example.monstergym.model.WorkoutLog log : logsMap.values()) {
                    JSONObject logObj = new JSONObject();
                    logObj.put("date", log.getDate() != null ? log.getDate() : "");
                    logObj.put("routine_id", log.getRoutineId() != null ? log.getRoutineId() : "");
                    logObj.put("routine_name", log.getRoutineName() != null ? log.getRoutineName() : "");
                    logObj.put("notes", log.getNotes() != null ? log.getNotes() : "");
                    logObj.put("timestamp", log.getTimestamp());
                    logsArray.put(logObj);
                }
                root.put("workout_logs", logsArray);
            }

            return root.toString(2);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void exportJsonFile() {
        if (appContext == null) return;
        new Thread(() -> {
            try {
                String jsonStr = generateJsonString();
                File outFile = new File(appContext.getFilesDir(), "gymRoutine.json");
                FileOutputStream fos = new FileOutputStream(outFile);
                fos.write(jsonStr.getBytes(StandardCharsets.UTF_8));
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String getImageName(Context context, int resId) {
        if (context == null || resId == 0) return "";
        try {
            return context.getResources().getResourceEntryName(resId);
        } catch (Exception e) {
            return "";
        }
    }

    private int getDrawableResId(Context context, String imageName, int defaultResId) {
        if (context == null || TextUtils.isEmpty(imageName)) return defaultResId;
        try {
            int resId = context.getResources().getIdentifier(imageName, "drawable", context.getPackageName());
            return resId != 0 ? resId : defaultResId;
        } catch (Exception e) {
            return defaultResId;
        }
    }
}