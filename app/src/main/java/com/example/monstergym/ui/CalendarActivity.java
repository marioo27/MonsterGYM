package com.example.monstergym.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.monstergym.R;
import com.example.monstergym.adapter.CalendarGridAdapter;
import com.example.monstergym.adapter.WorkoutLogAdapter;
import com.example.monstergym.data.WorkoutDataManager;
import com.example.monstergym.databinding.ActivityCalendarBinding;
import com.example.monstergym.databinding.DialogLogWorkoutBinding;
import com.example.monstergym.model.CalendarDay;
import com.example.monstergym.model.Routine;
import com.example.monstergym.model.WorkoutLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarActivity extends BaseActivity implements WorkoutLogAdapter.OnLogClickListener, CalendarGridAdapter.OnDayClickListener {

    private ActivityCalendarBinding binding;
    private WorkoutLogAdapter logAdapter;
    private CalendarGridAdapter calendarGridAdapter;
    private String selectedDateStr; // Format YYYY-MM-DD
    private int currentYear;
    private int currentMonth; // 0-indexed
    private Map<String, WorkoutLog> logsMap = new HashMap<>();
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
    private final SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", new Locale("es", "ES"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCalendarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();

        Calendar today = Calendar.getInstance();
        currentYear = today.get(Calendar.YEAR);
        currentMonth = today.get(Calendar.MONTH);
        selectedDateStr = formatDateStr(currentYear, currentMonth, today.get(Calendar.DAY_OF_MONTH));

        setupRecyclerViews();
        setupMonthNavigation();
        refreshAllData();

        binding.btnLogDay.setOnClickListener(v -> showLogWorkoutDialog(selectedDateStr));

        com.example.monstergym.utils.TutorialHelper.showFirstTimeHint(this, "hint_calendar", "Mi Calendario", "Toca cualquier día para registrar o ver el resumen de los ejercicios completados en esa fecha.");
    }

    private String formatDateStr(int year, int monthZeroBased, int day) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, monthZeroBased + 1, day);
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.calendar_title);
        }
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupRecyclerViews() {
        // Calendar Grid (7 columns, starts on Monday)
        binding.rvCalendarGrid.setLayoutManager(new GridLayoutManager(this, 7));
        calendarGridAdapter = new CalendarGridAdapter(this, new ArrayList<>(), this);
        binding.rvCalendarGrid.setAdapter(calendarGridAdapter);

        // Workout Logs List
        binding.rvWorkoutLogs.setLayoutManager(new LinearLayoutManager(this));
        logAdapter = new WorkoutLogAdapter(this, new ArrayList<>(), this);
        binding.rvWorkoutLogs.setAdapter(logAdapter);
    }

    private void setupMonthNavigation() {
        binding.btnPrevMonth.setOnClickListener(v -> {
            currentMonth--;
            if (currentMonth < 0) {
                currentMonth = 11;
                currentYear--;
            }
            refreshAllData();
        });

        binding.btnNextMonth.setOnClickListener(v -> {
            currentMonth++;
            if (currentMonth > 11) {
                currentMonth = 0;
                currentYear++;
            }
            refreshAllData();
        });
    }

    private void refreshAllData() {
        logsMap = WorkoutDataManager.getInstance().getAllWorkoutLogsMap();
        updateMonthTitle();
        generateCalendarGridDays();
        updateSelectedDayInfo();
        updateMonthlyStats();
        loadAllWorkoutLogs();
    }

    private void updateMonthTitle() {
        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1);
        String title = monthYearFormat.format(cal.getTime());
        if (!TextUtils.isEmpty(title)) {
            title = title.substring(0, 1).toUpperCase() + title.substring(1);
        }
        binding.tvMonthYearTitle.setText(title);
    }

    private String getAbbreviationForLog(WorkoutLog log) {
        if (log == null) return "";
        String rId = log.getRoutineId();
        String rName = log.getRoutineName();

        if ("rutina_tren_superior".equals(rId) || (rName != null && rName.toLowerCase().contains("superior"))) {
            return "SUP";
        }
        if ("rutina_tren_inferior".equals(rId) || (rName != null && rName.toLowerCase().contains("inferior"))) {
            return "INF";
        }

        Routine r = WorkoutDataManager.getInstance().getRoutineById(rId);
        if (r != null && !TextUtils.isEmpty(r.getAbbreviation())) {
            return r.getAbbreviation();
        }

        if (rName != null && !rName.trim().isEmpty()) {
            String[] parts = rName.trim().split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String p : parts) {
                if (!p.isEmpty()) {
                    sb.append(Character.toUpperCase(p.charAt(0)));
                    if (sb.length() >= 3) break;
                }
            }
            return sb.length() > 0 ? sb.toString() : "EX";
        }

        return "EX";
    }

    private void generateCalendarGridDays() {
        List<CalendarDay> dayList = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1);

        // Day of week for 1st of month: Sunday=1, Monday=2 ... Saturday=7
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        // Offset from Monday (Monday = 0, Tuesday = 1, ... Sunday = 6)
        int offset = (firstDayOfWeek + 5) % 7;

        // Previous month trailing days
        Calendar prevCal = (Calendar) cal.clone();
        prevCal.add(Calendar.MONTH, -1);
        int maxDayPrevMonth = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int prevMonth = prevCal.get(Calendar.MONTH);
        int prevYear = prevCal.get(Calendar.YEAR);

        for (int i = offset - 1; i >= 0; i--) {
            int d = maxDayPrevMonth - i;
            String dateStr = formatDateStr(prevYear, prevMonth, d);
            WorkoutLog log = logsMap.get(dateStr);
            boolean isTrained = (log != null);
            String abbrev = getAbbreviationForLog(log);
            boolean isSelected = dateStr.equals(selectedDateStr);
            dayList.add(new CalendarDay(prevYear, prevMonth, d, false, dateStr, isTrained, isSelected, abbrev));
        }

        // Current month days
        int maxDayCurrentMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int d = 1; d <= maxDayCurrentMonth; d++) {
            String dateStr = formatDateStr(currentYear, currentMonth, d);
            WorkoutLog log = logsMap.get(dateStr);
            boolean isTrained = (log != null);
            String abbrev = getAbbreviationForLog(log);
            boolean isSelected = dateStr.equals(selectedDateStr);
            dayList.add(new CalendarDay(currentYear, currentMonth, d, true, dateStr, isTrained, isSelected, abbrev));
        }

        // Next month leading days to complete grid rows (multiple of 7)
        int remainder = dayList.size() % 7;
        if (remainder != 0) {
            int pad = 7 - remainder;
            Calendar nextCal = (Calendar) cal.clone();
            nextCal.add(Calendar.MONTH, 1);
            int nextMonth = nextCal.get(Calendar.MONTH);
            int nextYear = nextCal.get(Calendar.YEAR);
            for (int d = 1; d <= pad; d++) {
                String dateStr = formatDateStr(nextYear, nextMonth, d);
                WorkoutLog log = logsMap.get(dateStr);
                boolean isTrained = (log != null);
                String abbrev = getAbbreviationForLog(log);
                boolean isSelected = dateStr.equals(selectedDateStr);
                dayList.add(new CalendarDay(nextYear, nextMonth, d, false, dateStr, isTrained, isSelected, abbrev));
            }
        }

        calendarGridAdapter.setDays(dayList);
    }

    @Override
    public void onDayClick(CalendarDay day) {
        selectedDateStr = day.getDateStr();
        currentYear = day.getYear();
        currentMonth = day.getMonth();
        updateMonthTitle();
        generateCalendarGridDays();
        updateSelectedDayInfo();
    }

    private void updateSelectedDayInfo() {
        binding.tvSelectedDateText.setText(getDisplayDateText(selectedDateStr));

        WorkoutLog log = logsMap.get(selectedDateStr);
        if (log != null) {
            binding.llWorkoutInfo.setVisibility(View.VISIBLE);
            binding.tvNoWorkoutInfo.setVisibility(View.GONE);
            binding.tvLoggedRoutineName.setText(log.getRoutineName());

            if (!TextUtils.isEmpty(log.getNotes())) {
                binding.tvLoggedNotes.setVisibility(View.VISIBLE);
                binding.tvLoggedNotes.setText(log.getNotes());
            } else {
                binding.tvLoggedNotes.setVisibility(View.GONE);
            }
            binding.btnLogDay.setText(R.string.btn_edit_workout);
        } else {
            binding.llWorkoutInfo.setVisibility(View.GONE);
            binding.tvNoWorkoutInfo.setVisibility(View.VISIBLE);
            binding.btnLogDay.setText(R.string.btn_log_workout);
        }
    }

    private String getDisplayDateText(String dateStr) {
        try {
            String[] parts = dateStr.split("-");
            if (parts.length == 3) {
                int y = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]) - 1;
                int d = Integer.parseInt(parts[2]);
                Calendar cal = Calendar.getInstance();
                cal.set(y, m, d);
                String displayDate = displayDateFormat.format(cal.getTime());
                if (!TextUtils.isEmpty(displayDate)) {
                    return displayDate.substring(0, 1).toUpperCase() + displayDate.substring(1);
                }
            }
        } catch (Exception ignored) {}
        return dateStr;
    }

    private void updateMonthlyStats() {
        int monthlyCount = 0;
        int courseCount = 0;
        String monthPrefix = String.format(Locale.US, "%04d-%02d", currentYear, currentMonth + 1);

        int courseStartYear = currentMonth >= Calendar.SEPTEMBER ? currentYear : currentYear - 1;
        String courseStartDateStr = String.format(Locale.US, "%04d-09-01", courseStartYear);

        for (String date : logsMap.keySet()) {
            if (date.startsWith(monthPrefix)) {
                monthlyCount++;
            }
            if (date.compareTo(courseStartDateStr) >= 0) {
                courseCount++;
            }
        }

        String statsText = getString(R.string.text_monthly_stats, monthlyCount)
                + "\n" + getString(R.string.text_course_stats, courseCount);
        binding.tvMonthlyStats.setText(statsText);
    }

    private void loadAllWorkoutLogs() {
        List<WorkoutLog> logList = new ArrayList<>();
        if (logsMap != null) {
            for (WorkoutLog l : logsMap.values()) {
                if (l != null && l.getDate() != null) {
                    logList.add(l);
                }
            }
        }

        // Sort descending by date safely
        logList.sort((a, b) -> b.getDate().compareTo(a.getDate()));

        if (logList.isEmpty()) {
            binding.tvEmptyLogs.setVisibility(View.VISIBLE);
            binding.rvWorkoutLogs.setVisibility(View.GONE);
            logAdapter.setLogs(new ArrayList<>());
        } else {
            binding.tvEmptyLogs.setVisibility(View.GONE);
            binding.rvWorkoutLogs.setVisibility(View.VISIBLE);
            logAdapter.setLogs(logList);
        }
    }

    @Override
    public void onEditLog(WorkoutLog log) {
        if (log != null) {
            showLogWorkoutDialog(log.getDate());
        }
    }

    @Override
    public void onDeleteLog(WorkoutLog log) {
        if (log != null && log.getDate() != null) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.calendar_title)
                    .setMessage("¿Deseas eliminar el registro del " + getDisplayDateText(log.getDate()) + "?")
                    .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                        WorkoutDataManager.getInstance().deleteWorkoutLog(log.getDate());
                        refreshAllData();
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();
        }
    }

    private void showLogWorkoutDialog(String targetDateStr) {
        try {
            String[] parts = targetDateStr.split("-");
            if (parts.length == 3) {
                int y = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]) - 1;
                int d = Integer.parseInt(parts[2]);
                Calendar targetCal = Calendar.getInstance();
                targetCal.set(y, m, d, 0, 0, 0);
                targetCal.set(Calendar.MILLISECOND, 0);

                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);

                if (targetCal.after(today)) {
                    Toast.makeText(this, R.string.error_future_date, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        } catch (Exception ignored) {}

        DialogLogWorkoutBinding dialogBinding = DialogLogWorkoutBinding.inflate(LayoutInflater.from(this));

        dialogBinding.tvDialogDateTitle.setText(getDisplayDateText(targetDateStr));

        List<Routine> routines = WorkoutDataManager.getInstance().getRoutines();
        List<String> routineNames = new ArrayList<>();
        if (routines != null && !routines.isEmpty()) {
            for (Routine r : routines) {
                routineNames.add(r.getDisplayName(this));
            }
        } else {
            routineNames.add("Entrenamiento General");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, routineNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogBinding.spinnerRoutines.setAdapter(adapter);

        final WorkoutLog existingLog = WorkoutDataManager.getInstance().getWorkoutLog(targetDateStr);
        final String savedCompletedVars = existingLog != null ? existingLog.getCompletedVariations() : null;

        if (existingLog != null) {
            dialogBinding.switchAttended.setChecked(true);
            dialogBinding.llRoutineDetails.setVisibility(View.VISIBLE);
            dialogBinding.btnDeleteLog.setVisibility(View.VISIBLE);

            if (!TextUtils.isEmpty(existingLog.getNotes())) {
                dialogBinding.etLogNotes.setText(existingLog.getNotes());
            }

            int index = -1;
            if (routines != null) {
                for (int i = 0; i < routines.size(); i++) {
                    if (routines.get(i).getId().equals(existingLog.getRoutineId())
                            || routines.get(i).getDisplayName(this).equalsIgnoreCase(existingLog.getRoutineName())) {
                        index = i;
                        break;
                    }
                }
            }
            if (index >= 0) {
                dialogBinding.spinnerRoutines.setSelection(index);
            }
        } else {
            dialogBinding.switchAttended.setChecked(true);
            dialogBinding.llRoutineDetails.setVisibility(View.VISIBLE);
            dialogBinding.btnDeleteLog.setVisibility(View.GONE);
        }

        dialogBinding.spinnerRoutines.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (routines != null && position >= 0 && position < routines.size()) {
                    Routine r = routines.get(position);
                    boolean isSameRoutineAsSaved = existingLog != null && r.getId().equals(existingLog.getRoutineId());
                    populateExerciseCheckboxes(CalendarActivity.this, dialogBinding.llExerciseSelectionContainer, r, isSameRoutineAsSaved ? savedCompletedVars : null);
                } else {
                    dialogBinding.llExerciseSelectionContainer.removeAllViews();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                dialogBinding.llExerciseSelectionContainer.removeAllViews();
            }
        });

        int initialPos = dialogBinding.spinnerRoutines.getSelectedItemPosition();
        if (routines != null && initialPos >= 0 && initialPos < routines.size()) {
            Routine initialRoutine = routines.get(initialPos);
            boolean isSameRoutineAsSaved = existingLog != null && initialRoutine.getId().equals(existingLog.getRoutineId());
            populateExerciseCheckboxes(this, dialogBinding.llExerciseSelectionContainer, initialRoutine, isSameRoutineAsSaved ? savedCompletedVars : null);
        }

        dialogBinding.switchAttended.setOnCheckedChangeListener((buttonView, isChecked) -> {
            dialogBinding.llRoutineDetails.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.calendar_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.btn_save, (dialogInterface, which) -> {
                    try {
                        if (dialogBinding.switchAttended.isChecked()) {
                            int selectedIndex = dialogBinding.spinnerRoutines.getSelectedItemPosition();
                            String routineId = "";
                            String routineName = "";
                            if (routines != null && selectedIndex >= 0 && selectedIndex < routines.size()) {
                                routineId = routines.get(selectedIndex).getId();
                                routineName = routines.get(selectedIndex).getDisplayName(CalendarActivity.this);
                            } else if (!routineNames.isEmpty()) {
                                routineName = routineNames.get(0);
                            }

                            List<String> checkedKeys = new ArrayList<>();
                            for (int i = 0; i < dialogBinding.llExerciseSelectionContainer.getChildCount(); i++) {
                                View child = dialogBinding.llExerciseSelectionContainer.getChildAt(i);
                                if (child instanceof com.google.android.material.checkbox.MaterialCheckBox) {
                                    com.google.android.material.checkbox.MaterialCheckBox cb = (com.google.android.material.checkbox.MaterialCheckBox) child;
                                    if (cb.isChecked() && cb.getTag() != null) {
                                        checkedKeys.add(cb.getTag().toString());
                                    }
                                }
                            }

                            String notes = dialogBinding.etLogNotes.getText() != null ? dialogBinding.etLogNotes.getText().toString().trim() : "";
                            String completedVars = TextUtils.join(",", checkedKeys);

                            WorkoutLog log = new WorkoutLog(
                                    targetDateStr,
                                    routineId,
                                    routineName,
                                    notes,
                                    System.currentTimeMillis(),
                                    completedVars
                            );
                            WorkoutDataManager.getInstance().saveWorkoutLog(log);
                        } else {
                            WorkoutDataManager.getInstance().deleteWorkoutLog(targetDateStr);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    refreshAllData();
                })
                .setNegativeButton(R.string.btn_cancel, (d, which) -> d.dismiss())
                .create();

        dialog.show();

        dialogBinding.btnDeleteLog.setOnClickListener(v -> {
            WorkoutDataManager.getInstance().deleteWorkoutLog(targetDateStr);
            refreshAllData();
            dialog.dismiss();
        });
    }

    private void populateExerciseCheckboxes(android.content.Context context, android.widget.LinearLayout container, Routine routine, String savedCompletedVars) {
        container.removeAllViews();
        if (routine == null || routine.getExercises() == null || routine.getExercises().isEmpty()) {
            android.widget.TextView tvEmpty = new android.widget.TextView(context);
            tvEmpty.setText("No hay ejercicios en esta rutina.");
            tvEmpty.setTextColor(android.graphics.Color.GRAY);
            tvEmpty.setTextSize(13);
            container.addView(tvEmpty);
            return;
        }

        java.util.Set<String> savedSet = new java.util.HashSet<>();
        if (!TextUtils.isEmpty(savedCompletedVars)) {
            String[] parts = savedCompletedVars.split(",");
            for (String p : parts) {
                if (!p.trim().isEmpty()) savedSet.add(p.trim());
            }
        }
        boolean isNewLog = (savedCompletedVars == null);

        List<com.example.monstergym.model.Exercise> mainExercises = new ArrayList<>();
        List<com.example.monstergym.model.Exercise> extraExercises = new ArrayList<>();
        for (com.example.monstergym.model.Exercise ex : routine.getExercises()) {
            if (ex.isExtra()) {
                extraExercises.add(ex);
            } else {
                mainExercises.add(ex);
            }
        }

        renderExerciseGroup(context, container, "EJERCICIOS PRINCIPALES", mainExercises, savedSet, isNewLog);
        if (!extraExercises.isEmpty()) {
            renderExerciseGroup(context, container, "EJERCICIOS EXTRA", extraExercises, savedSet, isNewLog);
        }
    }

    private void renderExerciseGroup(android.content.Context context, android.widget.LinearLayout container, String headerTitle, List<com.example.monstergym.model.Exercise> exercises, java.util.Set<String> savedSet, boolean isNewLog) {
        if (exercises.isEmpty()) return;

        android.widget.TextView tvHeader = new android.widget.TextView(context);
        tvHeader.setText(headerTitle);
        tvHeader.setTextSize(12);
        tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        tvHeader.setTextColor(android.graphics.Color.parseColor("#1976D2"));
        tvHeader.setPadding(0, 8, 0, 4);
        container.addView(tvHeader);

        for (com.example.monstergym.model.Exercise ex : exercises) {
            android.widget.TextView tvExName = new android.widget.TextView(context);
            String title = ex.getDisplayName(context);
            if (ex.isExtra()) title += " [EXTRA]";
            tvExName.setText(title);
            tvExName.setTextSize(14);
            tvExName.setTypeface(null, android.graphics.Typeface.BOLD);
            tvExName.setTextColor(android.graphics.Color.parseColor("#212121"));
            tvExName.setPadding(8, 6, 0, 2);
            container.addView(tvExName);

            List<com.example.monstergym.model.ExerciseVariation> variations = ex.getVariations();
            if (variations != null && !variations.isEmpty()) {
                for (int vIdx = 0; vIdx < variations.size(); vIdx++) {
                    com.example.monstergym.model.ExerciseVariation var = variations.get(vIdx);
                    com.google.android.material.checkbox.MaterialCheckBox cb = new com.google.android.material.checkbox.MaterialCheckBox(context);
                    String itemKey = ex.getId() + "::" + var.getId();
                    cb.setTag(itemKey);

                    String varLabel = " • " + var.getDisplayName(context) + " (" + var.getSets() + "x" + var.getRepetitions() + ")";
                    cb.setText(varLabel);
                    cb.setTextSize(13);
                    cb.setTextColor(android.graphics.Color.parseColor("#424242"));
                    cb.setPadding(16, 0, 0, 0);

                    if (isNewLog) {
                        cb.setChecked(!ex.isExtra() && vIdx == 0);
                    } else {
                        cb.setChecked(savedSet.contains(itemKey) || savedSet.contains(var.getId()));
                    }
                    container.addView(cb);
                }
            } else {
                com.google.android.material.checkbox.MaterialCheckBox cb = new com.google.android.material.checkbox.MaterialCheckBox(context);
                String itemKey = ex.getId();
                cb.setTag(itemKey);
                cb.setText(" • " + ex.getDisplayName(context));
                cb.setTextSize(13);
                cb.setTextColor(android.graphics.Color.parseColor("#424242"));
                cb.setPadding(16, 0, 0, 0);

                if (isNewLog) {
                    cb.setChecked(!ex.isExtra());
                } else {
                    cb.setChecked(savedSet.contains(itemKey));
                }
                container.addView(cb);
            }
        }
    }
}