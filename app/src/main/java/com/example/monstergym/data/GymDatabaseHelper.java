package com.example.monstergym.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.monstergym.R;
import com.example.monstergym.model.Exercise;
import com.example.monstergym.model.ExerciseVariation;
import com.example.monstergym.model.Routine;
import com.example.monstergym.model.WorkoutLog;

import java.util.ArrayList;
import java.util.List;

public class GymDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "monstergym.db";
    private static final int DATABASE_VERSION = 2;

    // Tables
    private static final String TABLE_ROUTINES = "routines";
    private static final String TABLE_EXERCISES = "exercises";
    private static final String TABLE_VARIATIONS = "variations";
    private static final String TABLE_SET_WEIGHTS = "set_weights";
    private static final String TABLE_WORKOUT_LOGS = "workout_logs";

    public GymDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Routines Table
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ROUTINES + " ("
                + "id TEXT PRIMARY KEY, "
                + "name TEXT, "
                + "name_res_id INTEGER, "
                + "description TEXT, "
                + "description_res_id INTEGER, "
                + "image_resource INTEGER, "
                + "position INTEGER"
                + ");");

        // Create Exercises Table
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EXERCISES + " ("
                + "id TEXT PRIMARY KEY, "
                + "routine_id TEXT, "
                + "name TEXT, "
                + "name_res_id INTEGER, "
                + "image_resource INTEGER, "
                + "is_extra INTEGER, "
                + "position INTEGER"
                + ");");

        // Create Variations Table
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_VARIATIONS + " ("
                + "id TEXT PRIMARY KEY, "
                + "exercise_id TEXT, "
                + "name TEXT, "
                + "name_res_id INTEGER, "
                + "sets INTEGER, "
                + "repetitions INTEGER, "
                + "weight REAL, "
                + "weight_unit TEXT, "
                + "rest_seconds INTEGER, "
                + "notes TEXT, "
                + "notes_res_id INTEGER, "
                + "image_resource INTEGER, "
                + "position INTEGER"
                + ");");

        // Create Set Weights Table
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SET_WEIGHTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "variation_id TEXT, "
                + "set_index INTEGER, "
                + "weight_kg REAL"
                + ");");

        // Create Workout Logs Table
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_WORKOUT_LOGS + " ("
                + "date TEXT PRIMARY KEY, "
                + "routine_id TEXT, "
                + "routine_name TEXT, "
                + "notes TEXT, "
                + "timestamp INTEGER"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_WORKOUT_LOGS + " ("
                    + "date TEXT PRIMARY KEY, "
                    + "routine_id TEXT, "
                    + "routine_name TEXT, "
                    + "notes TEXT, "
                    + "timestamp INTEGER"
                    + ");");
        }
    }

    public boolean isDatabaseEmpty() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_ROUTINES, null);
        boolean empty = true;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                empty = cursor.getInt(0) == 0;
            }
            cursor.close();
        }
        return empty;
    }

    public void saveAllRoutines(List<Routine> routines) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_SET_WEIGHTS, null, null);
            db.delete(TABLE_VARIATIONS, null, null);
            db.delete(TABLE_EXERCISES, null, null);
            db.delete(TABLE_ROUTINES, null, null);

            if (routines != null) {
                for (int rPos = 0; rPos < routines.size(); rPos++) {
                    Routine r = routines.get(rPos);
                    insertRoutineInternal(db, r, rPos);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void insertRoutine(Routine routine, int position) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            insertRoutineInternal(db, routine, position);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void insertRoutineInternal(SQLiteDatabase db, Routine routine, int position) {
        ContentValues cv = new ContentValues();
        cv.put("id", routine.getId());
        cv.put("name", routine.getName());
        cv.put("name_res_id", routine.getNameResId());
        cv.put("description", routine.getDescription());
        cv.put("description_res_id", routine.getDescriptionResId());
        cv.put("image_resource", routine.getImageResource());
        cv.put("position", position);
        db.insertWithOnConflict(TABLE_ROUTINES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);

        if (routine.getExercises() != null) {
            for (int ePos = 0; ePos < routine.getExercises().size(); ePos++) {
                Exercise ex = routine.getExercises().get(ePos);
                insertExerciseInternal(db, routine.getId(), ex, ePos);
            }
        }
    }

    public void updateRoutine(Routine routine) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", routine.getName());
        cv.put("name_res_id", routine.getNameResId());
        cv.put("description", routine.getDescription());
        cv.put("description_res_id", routine.getDescriptionResId());
        cv.put("image_resource", routine.getImageResource());
        db.update(TABLE_ROUTINES, cv, "id = ?", new String[]{routine.getId()});
    }

    public void deleteRoutine(String routineId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // Delete associated variations and set_weights
            Cursor exCursor = db.query(TABLE_EXERCISES, new String[]{"id"}, "routine_id = ?", new String[]{routineId}, null, null, null);
            if (exCursor != null) {
                while (exCursor.moveToNext()) {
                    String exerciseId = exCursor.getString(0);
                    deleteExerciseInternal(db, exerciseId);
                }
                exCursor.close();
            }
            db.delete(TABLE_EXERCISES, "routine_id = ?", new String[]{routineId});
            db.delete(TABLE_ROUTINES, "id = ?", new String[]{routineId});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void insertExercise(String routineId, Exercise exercise, int position) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            insertExerciseInternal(db, routineId, exercise, position);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void insertExerciseInternal(SQLiteDatabase db, String routineId, Exercise exercise, int position) {
        ContentValues cv = new ContentValues();
        cv.put("id", exercise.getId());
        cv.put("routine_id", routineId);
        cv.put("name", exercise.getName());
        cv.put("name_res_id", exercise.getNameResId());
        cv.put("image_resource", exercise.getImageResource());
        cv.put("is_extra", exercise.isExtra() ? 1 : 0);
        cv.put("position", position);
        db.insertWithOnConflict(TABLE_EXERCISES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);

        if (exercise.getVariations() != null) {
            for (int vPos = 0; vPos < exercise.getVariations().size(); vPos++) {
                ExerciseVariation var = exercise.getVariations().get(vPos);
                insertVariationInternal(db, exercise.getId(), var, vPos);
            }
        }
    }

    public void updateExercise(Exercise exercise) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", exercise.getName());
        cv.put("name_res_id", exercise.getNameResId());
        cv.put("image_resource", exercise.getImageResource());
        cv.put("is_extra", exercise.isExtra() ? 1 : 0);
        db.update(TABLE_EXERCISES, cv, "id = ?", new String[]{exercise.getId()});
    }

    public void deleteExercise(String exerciseId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            deleteExerciseInternal(db, exerciseId);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void deleteExerciseInternal(SQLiteDatabase db, String exerciseId) {
        Cursor varCursor = db.query(TABLE_VARIATIONS, new String[]{"id"}, "exercise_id = ?", new String[]{exerciseId}, null, null, null);
        if (varCursor != null) {
            while (varCursor.moveToNext()) {
                String varId = varCursor.getString(0);
                db.delete(TABLE_SET_WEIGHTS, "variation_id = ?", new String[]{varId});
            }
            varCursor.close();
        }
        db.delete(TABLE_VARIATIONS, "exercise_id = ?", new String[]{exerciseId});
        db.delete(TABLE_EXERCISES, "id = ?", new String[]{exerciseId});
    }

    public void insertVariation(String exerciseId, ExerciseVariation variation, int position) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            insertVariationInternal(db, exerciseId, variation, position);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void insertVariationInternal(SQLiteDatabase db, String exerciseId, ExerciseVariation variation, int position) {
        ContentValues cv = new ContentValues();
        cv.put("id", variation.getId());
        cv.put("exercise_id", exerciseId);
        cv.put("name", variation.getName());
        cv.put("name_res_id", variation.getNameResId());
        cv.put("sets", variation.getSets());
        cv.put("repetitions", variation.getRepetitions());
        cv.put("weight", variation.getWeight());
        cv.put("weight_unit", variation.getWeightUnit());
        cv.put("rest_seconds", variation.getRestSeconds());
        cv.put("notes", variation.getNotes());
        cv.put("notes_res_id", variation.getNotesResId());
        cv.put("image_resource", variation.getImageResource());
        cv.put("position", position);
        db.insertWithOnConflict(TABLE_VARIATIONS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);

        // Save Set Weights
        db.delete(TABLE_SET_WEIGHTS, "variation_id = ?", new String[]{variation.getId()});
        List<Double> weights = variation.getSetWeights();
        if (weights != null) {
            for (int i = 0; i < weights.size(); i++) {
                ContentValues wCv = new ContentValues();
                wCv.put("variation_id", variation.getId());
                wCv.put("set_index", i);
                wCv.put("weight_kg", weights.get(i));
                db.insert(TABLE_SET_WEIGHTS, null, wCv);
            }
        }
    }

    public void updateVariation(ExerciseVariation variation) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put("name", variation.getName());
            cv.put("name_res_id", variation.getNameResId());
            cv.put("sets", variation.getSets());
            cv.put("repetitions", variation.getRepetitions());
            cv.put("weight", variation.getWeight());
            cv.put("weight_unit", variation.getWeightUnit());
            cv.put("rest_seconds", variation.getRestSeconds());
            cv.put("notes", variation.getNotes());
            cv.put("notes_res_id", variation.getNotesResId());
            cv.put("image_resource", variation.getImageResource());
            db.update(TABLE_VARIATIONS, cv, "id = ?", new String[]{variation.getId()});

            db.delete(TABLE_SET_WEIGHTS, "variation_id = ?", new String[]{variation.getId()});
            List<Double> weights = variation.getSetWeights();
            if (weights != null) {
                for (int i = 0; i < weights.size(); i++) {
                    ContentValues wCv = new ContentValues();
                    wCv.put("variation_id", variation.getId());
                    wCv.put("set_index", i);
                    wCv.put("weight_kg", weights.get(i));
                    db.insert(TABLE_SET_WEIGHTS, null, wCv);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void deleteVariation(String variationId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_SET_WEIGHTS, "variation_id = ?", new String[]{variationId});
            db.delete(TABLE_VARIATIONS, "id = ?", new String[]{variationId});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Routine> getAllRoutines() {
        List<Routine> routines = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor rCursor = db.query(TABLE_ROUTINES, null, null, null, null, null, "position ASC");
        if (rCursor != null) {
            while (rCursor.moveToNext()) {
                String rId = rCursor.getString(rCursor.getColumnIndexOrThrow("id"));
                String rName = rCursor.getString(rCursor.getColumnIndexOrThrow("name"));
                int rNameRes = rCursor.getInt(rCursor.getColumnIndexOrThrow("name_res_id"));
                String rDesc = rCursor.getString(rCursor.getColumnIndexOrThrow("description"));
                int rDescRes = rCursor.getInt(rCursor.getColumnIndexOrThrow("description_res_id"));
                int rImage = rCursor.getInt(rCursor.getColumnIndexOrThrow("image_resource"));
                if (rImage == 0 || rImage == R.drawable.ic_dumbbell) {
                    rImage = R.drawable.devil_mad_red;
                }

                List<Exercise> exercises = getExercisesForRoutine(db, rId);

                Routine routine = new Routine(rId, rName, rNameRes, rDesc, rDescRes, rName, 0, rImage, exercises);
                routines.add(routine);
            }
            rCursor.close();
        }
        return routines;
    }

    private List<Exercise> getExercisesForRoutine(SQLiteDatabase db, String routineId) {
        List<Exercise> exercises = new ArrayList<>();
        Cursor exCursor = db.query(TABLE_EXERCISES, null, "routine_id = ?", new String[]{routineId}, null, null, "position ASC");
        if (exCursor != null) {
            while (exCursor.moveToNext()) {
                String exId = exCursor.getString(exCursor.getColumnIndexOrThrow("id"));
                String exName = exCursor.getString(exCursor.getColumnIndexOrThrow("name"));
                int exNameRes = exCursor.getInt(exCursor.getColumnIndexOrThrow("name_res_id"));
                int exImage = exCursor.getInt(exCursor.getColumnIndexOrThrow("image_resource"));
                if (exImage == 0 || exImage == R.drawable.image_placeholder) {
                    exImage = R.drawable.devil_mad_red;
                }
                boolean isExtra = exCursor.getInt(exCursor.getColumnIndexOrThrow("is_extra")) == 1;

                List<ExerciseVariation> variations = getVariationsForExercise(db, exId);

                Exercise exercise = new Exercise(exId, exName, exNameRes, "", 0, exImage, variations, isExtra);
                exercises.add(exercise);
            }
            exCursor.close();
        }
        return exercises;
    }

    private List<ExerciseVariation> getVariationsForExercise(SQLiteDatabase db, String exerciseId) {
        List<ExerciseVariation> variations = new ArrayList<>();
        Cursor varCursor = db.query(TABLE_VARIATIONS, null, "exercise_id = ?", new String[]{exerciseId}, null, null, "position ASC");
        if (varCursor != null) {
            while (varCursor.moveToNext()) {
                String varId = varCursor.getString(varCursor.getColumnIndexOrThrow("id"));
                String varName = varCursor.getString(varCursor.getColumnIndexOrThrow("name"));
                int varNameRes = varCursor.getInt(varCursor.getColumnIndexOrThrow("name_res_id"));
                int sets = varCursor.getInt(varCursor.getColumnIndexOrThrow("sets"));
                int reps = varCursor.getInt(varCursor.getColumnIndexOrThrow("repetitions"));
                double weight = varCursor.getDouble(varCursor.getColumnIndexOrThrow("weight"));
                String unit = varCursor.getString(varCursor.getColumnIndexOrThrow("weight_unit"));
                int rest = varCursor.getInt(varCursor.getColumnIndexOrThrow("rest_seconds"));
                String notes = varCursor.getString(varCursor.getColumnIndexOrThrow("notes"));
                int notesRes = varCursor.getInt(varCursor.getColumnIndexOrThrow("notes_res_id"));
                int image = varCursor.getInt(varCursor.getColumnIndexOrThrow("image_resource"));
                if (image == 0 || image == R.drawable.image_placeholder) {
                    image = R.drawable.devil_mad_red;
                }

                List<Double> setWeights = getSetWeightsForVariation(db, varId);

                ExerciseVariation variation = new ExerciseVariation(varId, varName, varNameRes, sets, reps, weight, unit, rest, notes, notesRes, image);
                if (setWeights != null && !setWeights.isEmpty()) {
                    variation.setSetWeights(setWeights);
                }
                variations.add(variation);
            }
            varCursor.close();
        }
        return variations;
    }

    private List<Double> getSetWeightsForVariation(SQLiteDatabase db, String variationId) {
        List<Double> setWeights = new ArrayList<>();
        Cursor wCursor = db.query(TABLE_SET_WEIGHTS, null, "variation_id = ?", new String[]{variationId}, null, null, "set_index ASC");
        if (wCursor != null) {
            while (wCursor.moveToNext()) {
                double wKg = wCursor.getDouble(wCursor.getColumnIndexOrThrow("weight_kg"));
                setWeights.add(wKg);
            }
            wCursor.close();
        }
        return setWeights;
    }

    public void saveWorkoutLog(WorkoutLog log) {
        if (log == null || log.getDate() == null) return;
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_WORKOUT_LOGS + " ("
                    + "date TEXT PRIMARY KEY, "
                    + "routine_id TEXT, "
                    + "routine_name TEXT, "
                    + "notes TEXT, "
                    + "timestamp INTEGER"
                    + ");");

            ContentValues cv = new ContentValues();
            cv.put("date", log.getDate());
            cv.put("routine_id", log.getRoutineId() != null ? log.getRoutineId() : "");
            cv.put("routine_name", log.getRoutineName() != null ? log.getRoutineName() : "");
            cv.put("notes", log.getNotes() != null ? log.getNotes() : "");
            cv.put("timestamp", log.getTimestamp());
            db.insertWithOnConflict(TABLE_WORKOUT_LOGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteWorkoutLog(String date) {
        if (date == null) return;
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_WORKOUT_LOGS + " ("
                    + "date TEXT PRIMARY KEY, "
                    + "routine_id TEXT, "
                    + "routine_name TEXT, "
                    + "notes TEXT, "
                    + "timestamp INTEGER"
                    + ");");
            db.delete(TABLE_WORKOUT_LOGS, "date = ?", new String[]{date});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public WorkoutLog getWorkoutLog(String date) {
        if (date == null) return null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.query(TABLE_WORKOUT_LOGS, null, "date = ?", new String[]{date}, null, null, null);
            WorkoutLog log = null;
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    String rId = cursor.getString(cursor.getColumnIndexOrThrow("routine_id"));
                    String rName = cursor.getString(cursor.getColumnIndexOrThrow("routine_name"));
                    String notes = cursor.getString(cursor.getColumnIndexOrThrow("notes"));
                    long ts = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
                    log = new WorkoutLog(date, rId, rName, notes, ts);
                }
                cursor.close();
            }
            return log;
        } catch (Exception e) {
            return null;
        }
    }

    public java.util.Map<String, WorkoutLog> getAllWorkoutLogsMap() {
        java.util.Map<String, WorkoutLog> map = new java.util.HashMap<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.query(TABLE_WORKOUT_LOGS, null, null, null, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                    String rId = cursor.getString(cursor.getColumnIndexOrThrow("routine_id"));
                    String rName = cursor.getString(cursor.getColumnIndexOrThrow("routine_name"));
                    String notes = cursor.getString(cursor.getColumnIndexOrThrow("notes"));
                    long ts = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
                    map.put(date, new WorkoutLog(date, rId, rName, notes, ts));
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public void updateRoutinePositions(List<Routine> routines) {
        if (routines == null) return;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (int i = 0; i < routines.size(); i++) {
                ContentValues cv = new ContentValues();
                cv.put("position", i);
                db.update(TABLE_ROUTINES, cv, "id = ?", new String[]{routines.get(i).getId()});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void updateExercisePositions(String routineId, List<Exercise> exercises) {
        if (exercises == null) return;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (int i = 0; i < exercises.size(); i++) {
                ContentValues cv = new ContentValues();
                cv.put("position", i);
                cv.put("routine_id", routineId);
                db.update(TABLE_EXERCISES, cv, "id = ?", new String[]{exercises.get(i).getId()});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void updateVariationPositions(String exerciseId, List<ExerciseVariation> variations) {
        if (variations == null) return;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (int i = 0; i < variations.size(); i++) {
                ContentValues cv = new ContentValues();
                cv.put("position", i);
                cv.put("exercise_id", exerciseId);
                db.update(TABLE_VARIATIONS, cv, "id = ?", new String[]{variations.get(i).getId()});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}