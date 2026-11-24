package com.example.monstergym.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.monstergym.databinding.ItemWorkoutLogBinding;
import com.example.monstergym.model.WorkoutLog;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class WorkoutLogAdapter extends RecyclerView.Adapter<WorkoutLogAdapter.LogViewHolder> {

    public interface OnLogClickListener {
        void onEditLog(WorkoutLog log);
        void onDeleteLog(WorkoutLog log);
    }

    private final Context context;
    private List<WorkoutLog> logs;
    private final OnLogClickListener listener;
    private final SimpleDateFormat parseSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat displaySdf = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));

    public WorkoutLogAdapter(Context context, List<WorkoutLog> logs, OnLogClickListener listener) {
        this.context = context;
        this.logs = logs;
        this.listener = listener;
    }

    public void setLogs(List<WorkoutLog> logs) {
        this.logs = logs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWorkoutLogBinding binding = ItemWorkoutLogBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new LogViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        holder.bind(logs.get(position), context, listener, parseSdf, displaySdf);
    }

    @Override
    public int getItemCount() {
        return logs != null ? logs.size() : 0;
    }

    public static class LogViewHolder extends RecyclerView.ViewHolder {
        private final ItemWorkoutLogBinding binding;

        public LogViewHolder(ItemWorkoutLogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(WorkoutLog log, Context context, OnLogClickListener listener,
                         SimpleDateFormat parseSdf, SimpleDateFormat displaySdf) {
            String formattedDate = log.getDate();
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(parseSdf.parse(log.getDate()));
                String text = displaySdf.format(cal.getTime());
                if (!TextUtils.isEmpty(text)) {
                    formattedDate = text.substring(0, 1).toUpperCase() + text.substring(1);
                }
            } catch (Exception ignored) {}

            binding.tvLogDate.setText(formattedDate);
            binding.tvLogRoutine.setText(log.getRoutineName());

            String fullSummary = formatLogNotesAndSummary(context, log);
            if (!TextUtils.isEmpty(fullSummary)) {
                binding.tvLogNotes.setVisibility(View.VISIBLE);
                binding.tvLogNotes.setText(fullSummary);
            } else {
                binding.tvLogNotes.setVisibility(View.GONE);
            }

            binding.btnEditLog.setOnClickListener(v -> {
                if (listener != null) listener.onEditLog(log);
            });

            binding.btnDeleteLogItem.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteLog(log);
            });
        }

        public static String formatLogNotesAndSummary(Context context, WorkoutLog log) {
            if (log == null) return "";
            StringBuilder sb = new StringBuilder();
            String completedVarsStr = log.getCompletedVariations();
            if (!TextUtils.isEmpty(completedVarsStr)) {
                java.util.Set<String> savedSet = new java.util.HashSet<>();
                for (String s : completedVarsStr.split(",")) {
                    if (!s.trim().isEmpty()) savedSet.add(s.trim());
                }

                com.example.monstergym.model.Routine routine = null;
                List<com.example.monstergym.model.Routine> routines = com.example.monstergym.data.WorkoutDataManager.getInstance().getRoutines();
                if (routines != null) {
                    for (com.example.monstergym.model.Routine r : routines) {
                        if (r.getId().equals(log.getRoutineId()) || r.getDisplayName(context).equalsIgnoreCase(log.getRoutineName())) {
                            routine = r;
                            break;
                        }
                    }
                }

                if (routine != null && routine.getExercises() != null) {
                    for (com.example.monstergym.model.Exercise ex : routine.getExercises()) {
                        List<String> matchedVars = new java.util.ArrayList<>();
                        if (ex.getVariations() != null && !ex.getVariations().isEmpty()) {
                            for (com.example.monstergym.model.ExerciseVariation v : ex.getVariations()) {
                                String key = ex.getId() + "::" + v.getId();
                                if (savedSet.contains(key) || savedSet.contains(v.getId())) {
                                    matchedVars.add(v.getDisplayName(context));
                                }
                            }
                        } else {
                            if (savedSet.contains(ex.getId())) {
                                matchedVars.add(ex.getDisplayName(context));
                            }
                        }

                        if (!matchedVars.isEmpty()) {
                            if (sb.length() > 0) sb.append("\n");
                            sb.append("✓ ").append(ex.getDisplayName(context));
                            if (ex.isExtra()) sb.append(" [EXTRA]");
                            sb.append(": ").append(TextUtils.join(", ", matchedVars));
                        }
                    }
                }
            }

            if (!TextUtils.isEmpty(log.getNotes())) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append("📝 ").append(log.getNotes());
            }

            return sb.toString();
        }
    }
}