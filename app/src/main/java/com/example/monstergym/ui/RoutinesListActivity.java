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
import com.example.monstergym.adapter.RoutineAdapter;
import com.example.monstergym.data.WorkoutDataManager;
import com.example.monstergym.databinding.ActivityRoutinesListBinding;
import com.example.monstergym.databinding.DialogCreateRoutineBinding;
import com.example.monstergym.model.Routine;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RoutinesListActivity extends BaseActivity implements RoutineAdapter.OnRoutineClickListener {

    private ActivityRoutinesListBinding binding;
    private RoutineAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRoutinesListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupRecyclerView();

        binding.fabAddRoutine.setOnClickListener(v -> showCreateRoutineDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRoutines();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.routines_title);
        }
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupRecyclerView() {
        binding.rvRoutines.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RoutineAdapter(this, new ArrayList<>(), this);
        binding.rvRoutines.setAdapter(adapter);

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
                WorkoutDataManager.getInstance().reorderRoutines(adapter.getRoutines());
            }
        });
        itemTouchHelper.attachToRecyclerView(binding.rvRoutines);
        adapter.setOnStartDragListener(itemTouchHelper::startDrag);
    }

    private void loadRoutines() {
        List<Routine> routines = WorkoutDataManager.getInstance().getRoutines();
        adapter.setRoutines(routines);

        if (routines.isEmpty()) {
            binding.tvEmptyRoutines.setVisibility(View.VISIBLE);
            binding.rvRoutines.setVisibility(View.GONE);
        } else {
            binding.tvEmptyRoutines.setVisibility(View.GONE);
            binding.rvRoutines.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onOpenRoutine(Routine routine) {
        Intent intent = new Intent(this, RoutineDetailActivity.class);
        intent.putExtra("ROUTINE_ID", routine.getId());
        startActivity(intent);
    }

    @Override
    public void onEditRoutine(Routine routine) {
        DialogCreateRoutineBinding dialogBinding = DialogCreateRoutineBinding.inflate(LayoutInflater.from(this));

        final int[] selectedImage = {routine.getImageResource() != 0 ? routine.getImageResource() : R.drawable.devil_mad_red};
        dialogBinding.ivRoutineIconSelect.setImageResource(selectedImage[0]);

        dialogBinding.llSelectRoutineImage.setOnClickListener(v -> {
            com.example.monstergym.utils.DevilImagePickerHelper.showPicker(this, drawableResId -> {
                selectedImage[0] = drawableResId;
                dialogBinding.ivRoutineIconSelect.setImageResource(drawableResId);
            });
        });

        dialogBinding.etRoutineName.setText(routine.getDisplayName(this));
        dialogBinding.etRoutineDescription.setText(routine.getDisplayDescription(this));
        dialogBinding.btnDeleteRoutine.setVisibility(View.VISIBLE);
        dialogBinding.btnDuplicateRoutine.setVisibility(View.VISIBLE);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_edit_routine_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.btn_save, null)
                .setNegativeButton(R.string.btn_cancel, (d, which) -> d.dismiss())
                .create();

        dialog.show();

        dialogBinding.btnDeleteRoutine.setOnClickListener(v -> {
            dialog.dismiss();
            onDeleteRoutine(routine);
        });

        dialogBinding.btnDuplicateRoutine.setOnClickListener(v -> {
            dialog.dismiss();
            WorkoutDataManager.getInstance().duplicateRoutine(routine);
            loadRoutines();
            android.widget.Toast.makeText(this, R.string.msg_routine_duplicated, android.widget.Toast.LENGTH_SHORT).show();
        });

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = dialogBinding.etRoutineName.getText() != null ? dialogBinding.etRoutineName.getText().toString().trim() : "";
            String desc = dialogBinding.etRoutineDescription.getText() != null ? dialogBinding.etRoutineDescription.getText().toString().trim() : "";

            if (TextUtils.isEmpty(name)) {
                dialogBinding.tilRoutineName.setError(getString(R.string.error_empty_routine_name));
                return;
            } else {
                dialogBinding.tilRoutineName.setError(null);
            }

            routine.setName(name);
            routine.setNameResId(0);
            routine.setDescription(desc);
            routine.setDescriptionResId(0);
            routine.setImageResource(selectedImage[0]);

            WorkoutDataManager.getInstance().updateRoutine(routine);
            loadRoutines();
            dialog.dismiss();
        });
    }

    @Override
    public void onDeleteRoutine(Routine routine) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_routine_title)
                .setMessage(getString(R.string.dialog_delete_routine_msg, routine.getDisplayName(this)))
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    WorkoutDataManager.getInstance().deleteRoutine(routine.getId());
                    loadRoutines();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void showCreateRoutineDialog() {
        DialogCreateRoutineBinding dialogBinding = DialogCreateRoutineBinding.inflate(LayoutInflater.from(this));

        final int[] selectedImage = {R.drawable.devil_mad_red};
        dialogBinding.ivRoutineIconSelect.setImageResource(selectedImage[0]);

        dialogBinding.llSelectRoutineImage.setOnClickListener(v -> {
            com.example.monstergym.utils.DevilImagePickerHelper.showPicker(this, drawableResId -> {
                selectedImage[0] = drawableResId;
                dialogBinding.ivRoutineIconSelect.setImageResource(drawableResId);
            });
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_create_routine_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.btn_create, null)
                .setNegativeButton(R.string.btn_cancel, (d, which) -> d.dismiss())
                .create();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE));
        }

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
                    selectedImage[0],
                    abbrev,
                    new ArrayList<>()
            );

            WorkoutDataManager.getInstance().addRoutine(newRoutine);
            loadRoutines();
            dialog.dismiss();

            Intent intent = new Intent(RoutinesListActivity.this, RoutineDetailActivity.class);
            intent.putExtra("ROUTINE_ID", newRoutine.getId());
            startActivity(intent);
        });
    }
}