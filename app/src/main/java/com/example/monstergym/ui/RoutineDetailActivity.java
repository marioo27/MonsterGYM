package com.example.monstergym.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.monstergym.R;
import com.example.monstergym.adapter.ExerciseAdapter;
import com.example.monstergym.data.WorkoutDataManager;
import com.example.monstergym.databinding.ActivityRoutineDetailBinding;
import com.example.monstergym.databinding.DialogCreateExerciseBinding;
import com.example.monstergym.databinding.DialogCreateRoutineBinding;
import com.example.monstergym.model.Exercise;
import com.example.monstergym.model.Routine;

import java.util.ArrayList;
import java.util.UUID;

public class RoutineDetailActivity extends BaseActivity implements ExerciseAdapter.OnExerciseClickListener {

    private ActivityRoutineDetailBinding binding;
    private ExerciseAdapter adapter;
    private String routineId;
    private Routine currentRoutine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRoutineDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        routineId = getIntent().getStringExtra("ROUTINE_ID");

        setupToolbar();
        setupRecyclerView();

        binding.btnEditRoutineDetail.setOnClickListener(v -> showEditRoutineDialog());
        binding.fabAddExercise.setOnClickListener(v -> showCreateExerciseDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRoutineData();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.app_name);
        }
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupRecyclerView() {
        binding.rvExercises.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExerciseAdapter(this, new ArrayList<>(), this);
        binding.rvExercises.setAdapter(adapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();
                adapter.onItemMove(fromPos, toPos);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                WorkoutDataManager.getInstance().reorderExercises(routineId, adapter.getExercises());
            }
        });
        itemTouchHelper.attachToRecyclerView(binding.rvExercises);
        adapter.setOnStartDragListener(itemTouchHelper::startDrag);
    }

    private void loadRoutineData() {
        currentRoutine = WorkoutDataManager.getInstance().getRoutineById(routineId);
        if (currentRoutine == null) {
            finish();
            return;
        }

        binding.tvRoutineDetailName.setText(currentRoutine.getDisplayName(this));

        String desc = currentRoutine.getDisplayDescription(this);
        if (TextUtils.isEmpty(desc)) {
            binding.tvRoutineDetailDesc.setVisibility(View.GONE);
        } else {
            binding.tvRoutineDetailDesc.setVisibility(View.VISIBLE);
            binding.tvRoutineDetailDesc.setText(desc);
        }

        int exCount = currentRoutine.getExercises() != null ? currentRoutine.getExercises().size() : 0;
        binding.tvTotalExercises.setText(getString(R.string.exercises_count_format, exCount));

        if (currentRoutine.getExercises() == null || currentRoutine.getExercises().isEmpty()) {
            binding.tvEmptyExercises.setVisibility(View.VISIBLE);
            binding.rvExercises.setVisibility(View.GONE);
            adapter.setExercises(new ArrayList<>());
        } else {
            binding.tvEmptyExercises.setVisibility(View.GONE);
            binding.rvExercises.setVisibility(View.VISIBLE);
            adapter.setExercises(currentRoutine.getExercises());
        }
    }

    @Override
    public void onOpenExercise(Exercise exercise) {
        Intent intent = new Intent(this, VariationsListActivity.class);
        intent.putExtra("ROUTINE_ID", routineId);
        intent.putExtra("EXERCISE_ID", exercise.getId());
        startActivity(intent);
    }

    @Override
    public void onEditExercise(Exercise exercise) {
        DialogCreateExerciseBinding dialogBinding = DialogCreateExerciseBinding.inflate(LayoutInflater.from(this));

        final int[] selectedImage = {exercise.getImageResource() != 0 ? exercise.getImageResource() : R.drawable.devil_mad_red};
        dialogBinding.ivExerciseIconSelect.setImageResource(selectedImage[0]);

        dialogBinding.llSelectExerciseImage.setOnClickListener(v -> {
            com.example.monstergym.utils.DevilImagePickerHelper.showPicker(this, drawableResId -> {
                selectedImage[0] = drawableResId;
                dialogBinding.ivExerciseIconSelect.setImageResource(drawableResId);
            });
        });

        dialogBinding.etExerciseName.setText(exercise.getDisplayName(this));
        dialogBinding.switchIsExtra.setChecked(exercise.isExtra());
        dialogBinding.btnDeleteExercise.setVisibility(View.VISIBLE);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_edit_exercise_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.btn_save, null)
                .setNegativeButton(R.string.btn_cancel, (d, which) -> d.dismiss())
                .create();

        dialog.show();

        dialogBinding.btnDeleteExercise.setOnClickListener(v -> {
            dialog.dismiss();
            onDeleteExercise(exercise);
        });

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = dialogBinding.etExerciseName.getText() != null ? dialogBinding.etExerciseName.getText().toString().trim() : "";
            boolean isExtra = dialogBinding.switchIsExtra.isChecked();

            if (TextUtils.isEmpty(name)) {
                dialogBinding.tilExerciseName.setError(getString(R.string.error_empty_exercise_name));
                return;
            } else {
                dialogBinding.tilExerciseName.setError(null);
            }

            exercise.setName(name);
            exercise.setNameResId(0);
            exercise.setExtra(isExtra);
            exercise.setImageResource(selectedImage[0]);

            WorkoutDataManager.getInstance().updateExercise(routineId, exercise);
            loadRoutineData();
            dialog.dismiss();
        });
    }

    @Override
    public void onDeleteExercise(Exercise exercise) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_exercise_title)
                .setMessage(getString(R.string.dialog_delete_exercise_msg, exercise.getDisplayName(this)))
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    WorkoutDataManager.getInstance().deleteExercise(routineId, exercise.getId());
                    loadRoutineData();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void showEditRoutineDialog() {
        if (currentRoutine == null) return;
        DialogCreateRoutineBinding dialogBinding = DialogCreateRoutineBinding.inflate(LayoutInflater.from(this));

        final int[] selectedImage = {currentRoutine.getImageResource() != 0 ? currentRoutine.getImageResource() : R.drawable.devil_mad_red};
        dialogBinding.ivRoutineIconSelect.setImageResource(selectedImage[0]);

        dialogBinding.llSelectRoutineImage.setOnClickListener(v -> {
            com.example.monstergym.utils.DevilImagePickerHelper.showPicker(this, drawableResId -> {
                selectedImage[0] = drawableResId;
                dialogBinding.ivRoutineIconSelect.setImageResource(drawableResId);
            });
        });

        dialogBinding.etRoutineName.setText(currentRoutine.getDisplayName(this));
        dialogBinding.etRoutineDescription.setText(currentRoutine.getDisplayDescription(this));
        dialogBinding.etRoutineAbbreviation.setText(currentRoutine.getAbbreviation());
        dialogBinding.btnDeleteRoutine.setVisibility(View.VISIBLE);
        dialogBinding.btnDuplicateRoutine.setVisibility(View.VISIBLE);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_edit_routine_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.btn_save, null)
                .setNegativeButton(R.string.btn_cancel, (d, which) -> d.dismiss())
                .create();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE));
        }

        dialogBinding.btnDeleteRoutine.setOnClickListener(v -> {
            dialog.dismiss();
            onDeleteRoutine(currentRoutine);
        });

        dialogBinding.btnDuplicateRoutine.setOnClickListener(v -> {
            dialog.dismiss();
            WorkoutDataManager.getInstance().duplicateRoutine(currentRoutine);
            loadRoutineData();
            android.widget.Toast.makeText(this, R.string.msg_routine_duplicated, android.widget.Toast.LENGTH_SHORT).show();
        });

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

            currentRoutine.setName(name);
            currentRoutine.setNameResId(0);
            currentRoutine.setDescription(desc);
            currentRoutine.setDescriptionResId(0);
            currentRoutine.setAbbreviation(abbrev);
            currentRoutine.setImageResource(selectedImage[0]);

            WorkoutDataManager.getInstance().updateRoutine(currentRoutine);
            loadRoutineData();
            dialog.dismiss();
        });
    }

    private void onDeleteRoutine(Routine routine) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_routine_title)
                .setMessage(getString(R.string.dialog_delete_routine_msg, routine.getDisplayName(this)))
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    WorkoutDataManager.getInstance().deleteRoutine(routine.getId());
                    finish();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void showCreateExerciseDialog() {
        DialogCreateExerciseBinding dialogBinding = DialogCreateExerciseBinding.inflate(LayoutInflater.from(this));
        dialogBinding.btnDeleteExercise.setVisibility(View.GONE);

        final int[] selectedImage = {R.drawable.devil_mad_red};
        dialogBinding.ivExerciseIconSelect.setImageResource(selectedImage[0]);

        dialogBinding.llSelectExerciseImage.setOnClickListener(v -> {
            com.example.monstergym.utils.DevilImagePickerHelper.showPicker(this, drawableResId -> {
                selectedImage[0] = drawableResId;
                dialogBinding.ivExerciseIconSelect.setImageResource(drawableResId);
            });
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_create_exercise_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.btn_create, null)
                .setNegativeButton(R.string.btn_cancel, (d, which) -> d.dismiss())
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = dialogBinding.etExerciseName.getText() != null ? dialogBinding.etExerciseName.getText().toString().trim() : "";
            boolean isExtra = dialogBinding.switchIsExtra.isChecked();

            if (TextUtils.isEmpty(name)) {
                dialogBinding.tilExerciseName.setError(getString(R.string.error_empty_exercise_name));
                return;
            } else {
                dialogBinding.tilExerciseName.setError(null);
            }

            Exercise newExercise = new Exercise(
                    UUID.randomUUID().toString(),
                    name, 0,
                    "", 0,
                    selectedImage[0],
                    new ArrayList<>(),
                    isExtra
            );

            WorkoutDataManager.getInstance().addExercise(routineId, newExercise);
            loadRoutineData();
            dialog.dismiss();

            Intent intent = new Intent(RoutineDetailActivity.this, VariationsListActivity.class);
            intent.putExtra("ROUTINE_ID", routineId);
            intent.putExtra("EXERCISE_ID", newExercise.getId());
            startActivity(intent);
        });
    }
}