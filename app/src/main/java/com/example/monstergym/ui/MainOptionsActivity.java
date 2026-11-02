package com.example.monstergym.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.monstergym.R;
import com.example.monstergym.adapter.CalendarGridAdapter;
import com.example.monstergym.data.WorkoutDataManager;
import com.example.monstergym.databinding.ActivityMainOptionsBinding;
import com.example.monstergym.databinding.DialogCreditsBinding;
import com.example.monstergym.databinding.DialogCreateRoutineBinding;
import com.example.monstergym.databinding.DialogLogWorkoutBinding;
import com.example.monstergym.databinding.DialogSettingsBinding;
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
import java.util.UUID;

public class MainOptionsActivity extends BaseActivity implements CalendarGridAdapter.OnDayClickListener {

    private ActivityMainOptionsBinding binding;
    private CalendarGridAdapter calendarGridAdapter;
    private String selectedDateStr; // Format YYYY-MM-DD
    private int currentYear;
    private int currentMonth; // 0-indexed
    private Map<String, WorkoutLog> logsMap = new HashMap<>();
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
    private final SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", new Locale("es", "ES"));
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private final String[] roasts = {
        "¿Eso es todo lo que vas a levantar hoy? ¡Mi abuela calienta con más peso!",
        "¿Hoy tocaba entrenar o venías a hacer amigos al vestuario?",
        "¡Menuda técnica... de risa! Menos mirar el móvil y más sudar.",
        "¿Seguro que esas mancuernas no son de plástico?",
        "¿Otra vez con excusas? ¡A darle duro!"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainOptionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WorkoutDataManager.getInstance(this).resetRoutinesFromAssetJson();

        setupToolbar();

        Calendar today = Calendar.getInstance();
        currentYear = today.get(Calendar.YEAR);
        currentMonth = today.get(Calendar.MONTH);
        selectedDateStr = formatDateStr(currentYear, currentMonth, today.get(Calendar.DAY_OF_MONTH));

        setupCalendarRecyclerView();
        setupMonthNavigation();
        setupDiablilloCoach();

        binding.cardSeeRoutines.setOnClickListener(v -> {
            Intent intent = new Intent(MainOptionsActivity.this, RoutinesListActivity.class);
            startActivity(intent);
        });

        binding.cardStartNew.setOnClickListener(v -> showCreateRoutineDialog());
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            showSettingsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private final androidx.activity.result.ActivityResultLauncher<String> importJsonLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
                        java.util.Scanner s = new java.util.Scanner(inputStream).useDelimiter("\\A");
                        String jsonStr = s.hasNext() ? s.next() : "";
                        inputStream.close();

                        boolean success = WorkoutDataManager.getInstance().importJsonData(jsonStr);
                        if (success) {
                            Toast.makeText(this, "¡JSON importado con éxito!", Toast.LENGTH_SHORT).show();
                            refreshAllData();
                        } else {
                            Toast.makeText(this, "Error al importar el JSON", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error al leer el archivo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void exportJson() {
        try {
            String jsonStr = WorkoutDataManager.getInstance().generateJsonString();
            java.io.File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) downloadsDir.mkdirs();
            java.io.File dest = new java.io.File(downloadsDir, "gymRoutine_backup.json");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(dest);
            fos.write(jsonStr.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            fos.close();
            Toast.makeText(this, "JSON exportado a Descargas: gymRoutine_backup.json", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                java.io.File internalFile = new java.io.File(getFilesDir(), "gymRoutine_export.json");
                java.io.FileOutputStream fos = new java.io.FileOutputStream(internalFile);
                fos.write(WorkoutDataManager.getInstance().generateJsonString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                fos.close();
                Toast.makeText(this, "Guardado en archivos internos de la app", Toast.LENGTH_LONG).show();
            } catch (Exception ex) {
                ex.printStackTrace();
                Toast.makeText(this, "Error al exportar JSON", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showSettingsDialog() {
        DialogSettingsBinding settingsBinding = DialogSettingsBinding.inflate(LayoutInflater.from(this));

        android.content.SharedPreferences prefs = getSharedPreferences("monstergym_prefs", MODE_PRIVATE);
        boolean isLbs = prefs.getBoolean("use_lbs", false);

        if (isLbs) {
            settingsBinding.rbLbs.setChecked(true);
        } else {
            settingsBinding.rbKg.setChecked(true);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(settingsBinding.getRoot())
                .setNegativeButton("Cerrar", (d, w) -> d.dismiss())
                .create();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#ECEFF1")));
        }

        settingsBinding.rgWeightUnit.setOnCheckedChangeListener((group, checkedId) -> {
            boolean lbs = (checkedId == settingsBinding.rbLbs.getId());
            prefs.edit().putBoolean("use_lbs", lbs).apply();
            Toast.makeText(this, lbs ? "Unidad cambiada a libras (lbs)" : "Unidad cambiada a kilos (kg)", Toast.LENGTH_SHORT).show();
        });

        settingsBinding.btnExportJson.setOnClickListener(v -> {
            exportJson();
            dialog.dismiss();
        });

        settingsBinding.btnImportJson.setOnClickListener(v -> {
            importJsonLauncher.launch("application/json");
            dialog.dismiss();
        });

        settingsBinding.btnCredits.setOnClickListener(v -> {
            showCreditsDialog();
        });
    }

    private void showCreditsDialog() {
        DialogCreditsBinding creditsBinding = DialogCreditsBinding.inflate(LayoutInflater.from(this));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(creditsBinding.getRoot())
                .setPositiveButton("OK", (d, w) -> d.dismiss())
                .create();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#ECEFF1")));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAllData();
    }

    private int coachTurnIndex = 0;
    private final String[] yellowRoasts = {
        "¿Eso es todo lo que vas a levantar hoy? ¡Mi abuela calienta con más peso!",
        "¿Hoy tocaba entrenar o venías a hacer amigos al vestuario?",
        "¡Menuda técnica... de risa! Menos mirar el móvil y más sudar.",
        "¿Seguro que esas mancuernas no son de plástico?",
        "¿Otra vez con excusas? ¡A darle duro!"
    };
    private final String[] blueHydration = {
        "¡Bebe agua y mantente hidratado! El músculo necesita agua para crecer, no solo excusas.",
        "¿Ya te has bebido la botella de agua? ¡Hidratación al poder antes de seguir levantando hierro!"
    };

    private void setupDiablilloCoach() {
        binding.cardDiablilloCoach.setOnClickListener(v -> showNextCoachMessage());
    }

    private void showNextCoachMessage() {
        if (coachTurnIndex < 2) {
            binding.ivCoachImage.setImageResource(R.drawable.devil_muscle_yellow);
            binding.tvCoachTitle.setText("Coach Diablillo");
            int rIdx = new java.util.Random().nextInt(yellowRoasts.length);
            binding.tvDiablilloSpeech.setText(yellowRoasts[rIdx]);
        } else {
            binding.ivCoachImage.setImageResource(R.drawable.devil_water_blue);
            binding.tvCoachTitle.setText("Coach Hidratación 💧");
            int bIdx = new java.util.Random().nextInt(blueHydration.length);
            binding.tvDiablilloSpeech.setText(blueHydration[bIdx]);
        }
        coachTurnIndex = (coachTurnIndex + 1) % 3;
    }

    private String formatDateStr(int year, int monthZeroBased, int day) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, monthZeroBased + 1, day);
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.app_name);
        }
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupCalendarRecyclerView() {
        binding.rvCalendarGrid.setLayoutManager(new GridLayoutManager(this, 7));
        calendarGridAdapter = new CalendarGridAdapter(this, new ArrayList<>(), this);
        binding.rvCalendarGrid.setAdapter(calendarGridAdapter);
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
        updateCounters();
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

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int offset = (firstDayOfWeek + 5) % 7; // Monday = 0

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

        int maxDayCurrentMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int d = 1; d <= maxDayCurrentMonth; d++) {
            String dateStr = formatDateStr(currentYear, currentMonth, d);
            WorkoutLog log = logsMap.get(dateStr);
            boolean isTrained = (log != null);
            String abbrev = getAbbreviationForLog(log);
            boolean isSelected = dateStr.equals(selectedDateStr);
            dayList.add(new CalendarDay(currentYear, currentMonth, d, true, dateStr, isTrained, isSelected, abbrev));
        }

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
        showLogWorkoutDialog(selectedDateStr);
    }

    private void updateCounters() {
        // 1. Workouts this week (Monday to Sunday)
        int thisWeekDays = 0;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysSinceMonday = (dayOfWeek == Calendar.SUNDAY) ? 6 : (dayOfWeek - Calendar.MONDAY);
        cal.add(Calendar.DATE, -daysSinceMonday);
        long mondayMillis = cal.getTimeInMillis();

        cal.add(Calendar.DATE, 7);
        long nextMondayMillis = cal.getTimeInMillis();

        for (String dateStr : logsMap.keySet()) {
            try {
                java.util.Date d = dateFormat.parse(dateStr);
                if (d != null) {
                    long t = d.getTime();
                    if (t >= mondayMillis && t < nextMondayMillis) {
                        thisWeekDays++;
                    }
                }
            } catch (Exception ignored) {}
        }

        // 2. Workouts this course (since September 1st)
        int courseDays = 0;
        Calendar today = Calendar.getInstance();
        int y = today.get(Calendar.YEAR);
        int m = today.get(Calendar.MONTH);
        int startYear = m >= Calendar.SEPTEMBER ? y : y - 1;
        String startDateStr = String.format(Locale.US, "%04d-09-01", startYear);

        for (String date : logsMap.keySet()) {
            if (date.compareTo(startDateStr) >= 0) {
                courseDays++;
            }
        }

        binding.tvThisWeekCount.setText(thisWeekDays + (thisWeekDays == 1 ? " día" : " días"));
        binding.tvCourseCount.setText(courseDays + (courseDays == 1 ? " día" : " días"));
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

        WorkoutLog existingLog = WorkoutDataManager.getInstance().getWorkoutLog(targetDateStr);
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
                                routineName = routines.get(selectedIndex).getDisplayName(MainOptionsActivity.this);
                            } else if (!routineNames.isEmpty()) {
                                routineName = routineNames.get(0);
                            }

                            String notes = dialogBinding.etLogNotes.getText() != null ? dialogBinding.etLogNotes.getText().toString().trim() : "";

                            WorkoutLog log = new WorkoutLog(
                                    targetDateStr,
                                    routineId,
                                    routineName,
                                    notes,
                                    System.currentTimeMillis()
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
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE));
        }

        dialogBinding.btnDeleteLog.setOnClickListener(v -> {
            WorkoutDataManager.getInstance().deleteWorkoutLog(targetDateStr);
            refreshAllData();
            dialog.dismiss();
        });
    }

    private void showCreateRoutineDialog() {
        DialogCreateRoutineBinding dialogBinding = DialogCreateRoutineBinding.inflate(LayoutInflater.from(this));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_create_routine_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.btn_create, null) // Set null to handle validation override
                .setNegativeButton(R.string.btn_cancel, (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String name = dialogBinding.etRoutineName.getText() != null ? dialogBinding.etRoutineName.getText().toString().trim() : "";
                    String desc = dialogBinding.etRoutineDescription.getText() != null ? dialogBinding.etRoutineDescription.getText().toString().trim() : "";
                    String abbrev = dialogBinding.etRoutineAbbreviation.getText() != null ? dialogBinding.etRoutineAbbreviation.getText().toString().trim() : "";

                    if (TextUtils.isEmpty(name)) {
                        dialogBinding.tilRoutineName.setError(getString(R.string.error_empty_routine_name));
                        return;
                    } else {
                        dialogBinding.tilRoutineName.setError(null);
                    }

                    Routine newRoutine = new Routine(
                            UUID.randomUUID().toString(),
                            name, 0,
                            desc, 0,
                            "", 0,
                            R.drawable.ic_dumbbell,
                            abbrev,
                            new ArrayList<>()
                    );

                    WorkoutDataManager.getInstance().addRoutine(newRoutine);
                    dialog.dismiss();

                    Intent intent = new Intent(MainOptionsActivity.this, RoutineDetailActivity.class);
                    intent.putExtra("ROUTINE_ID", newRoutine.getId());
                    startActivity(intent);
                });
            }
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE));
        }
    }
}