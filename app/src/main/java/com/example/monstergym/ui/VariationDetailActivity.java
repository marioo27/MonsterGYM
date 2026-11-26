package com.example.monstergym.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.monstergym.R;
import com.example.monstergym.data.WorkoutDataManager;
import com.example.monstergym.databinding.ActivityVariationDetailBinding;
import com.example.monstergym.model.ExerciseVariation;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class VariationDetailActivity extends BaseActivity {

    private ActivityVariationDetailBinding binding;
    private String routineId;
    private String exerciseId;
    private String variationId;
    private ExerciseVariation currentVariation;
    private final List<TextInputEditText> setWeightInputFields = new ArrayList<>();
    private boolean isUpdatingSetsUI = false;
    private int selectedImageResId = R.drawable.devil_mad_red;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVariationDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        routineId = getIntent().getStringExtra("ROUTINE_ID");
        exerciseId = getIntent().getStringExtra("EXERCISE_ID");
        variationId = getIntent().getStringExtra("VARIATION_ID");
        if (variationId != null && variationId.trim().isEmpty()) {
            variationId = null;
        }

        setupToolbar();
        setupSetsWatcher();

        binding.cbSameWeightAllSets.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingSetsUI) return;
            updateSetWeightsEnableState();
            if (isChecked) {
                syncSetWeightsToFirstSet();
            }
        });

        binding.cbHasWarmupSet.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.llWarmupContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        loadVariationData();

        binding.llSelectVariationImage.setOnClickListener(v -> {
            com.example.monstergym.utils.DevilImagePickerHelper.showPicker(this, drawableResId -> {
                selectedImageResId = drawableResId;
                binding.ivVariationDetailImage.setImageResource(drawableResId);
            });
        });

        binding.btnSaveVariation.setOnClickListener(v -> saveVariation());
        binding.btnCancelVariation.setOnClickListener(v -> finish());
        binding.btnDeleteVariation.setOnClickListener(v -> confirmDeleteVariation());

        com.example.monstergym.utils.TutorialHelper.showFirstTimeHint(this, "hint_variation_detail", "Configurar Variación", "Puedes marcar <b>'Todas las series con el mismo peso'</b> para ahorrar tiempo o incluir una <b>'Serie de calentamiento'</b> con su peso específico.");
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(variationId != null ? R.string.edit_variation_title : R.string.add_variation_title);
        }
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupSetsWatcher() {
        binding.etSets.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdatingSetsUI) return;
                String setsStr = s != null ? s.toString().trim() : "";
                try {
                    int numSets = Integer.parseInt(setsStr);
                    if (numSets > 0 && numSets <= 30) {
                        List<Double> currentWeights = getCurrentSetWeightsFromUI();
                        renderSetWeightInputs(numSets, currentWeights);
                    }
                } catch (NumberFormatException ignored) {}
            }
        });
    }

    private List<Double> getCurrentSetWeightsFromUI() {
        List<Double> weights = new ArrayList<>();
        for (TextInputEditText et : setWeightInputFields) {
            String text = et.getText() != null ? et.getText().toString().trim() : "";
            try {
                weights.add(Double.parseDouble(text));
            } catch (NumberFormatException e) {
                weights.add(0.0);
            }
        }
        return weights;
    }

    private boolean areAllWeightsEqual(List<Double> weights) {
        if (weights == null || weights.size() <= 1) return true;
        double first = weights.get(0);
        for (int i = 1; i < weights.size(); i++) {
            if (Math.abs(weights.get(i) - first) > 0.0001) {
                return false;
            }
        }
        return true;
    }

    private void syncSetWeightsToFirstSet() {
        if (setWeightInputFields.isEmpty()) return;
        String firstText = setWeightInputFields.get(0).getText() != null ? setWeightInputFields.get(0).getText().toString() : "";
        isUpdatingSetsUI = true;
        for (int i = 1; i < setWeightInputFields.size(); i++) {
            setWeightInputFields.get(i).setText(firstText);
        }
        isUpdatingSetsUI = false;
    }

    private void updateSetWeightsEnableState() {
        boolean sameWeight = binding.cbSameWeightAllSets.isChecked();
        for (int i = 1; i < setWeightInputFields.size(); i++) {
            TextInputEditText et = setWeightInputFields.get(i);
            et.setEnabled(!sameWeight);
            et.setAlpha(sameWeight ? 0.6f : 1.0f);
        }
    }

    private void renderSetWeightInputs(int numSets, List<Double> initialWeights) {
        isUpdatingSetsUI = true;
        setWeightInputFields.clear();
        binding.llSetWeightsContainer.removeAllViews();

        for (int i = 0; i < numSets; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.bottomMargin = dpToPx(8);
            row.setLayoutParams(rowParams);

            // Set label
            TextView tvLabel = new TextView(this);
            tvLabel.setText(String.format(getString(R.string.label_set_number), i + 1));
            tvLabel.setTextSize(14);
            tvLabel.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    dpToPx(70),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            tvLabel.setLayoutParams(labelParams);

            // Weight input field
            TextInputLayout til = new TextInputLayout(this, null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox);
            LinearLayout.LayoutParams tilParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
            );
            tilParams.rightMargin = dpToPx(8);
            til.setLayoutParams(tilParams);
            til.setBoxBackgroundColor(Color.parseColor("#1E293B"));
            til.setBoxStrokeColor(Color.parseColor("#BBDEFB"));
            til.setHintTextColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#BBDEFB")));
            til.setDefaultHintTextColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#BBDEFB")));

            TextInputEditText et = new TextInputEditText(til.getContext());
            et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            et.setTextColor(Color.WHITE);
            et.setHintTextColor(Color.parseColor("#BBDEFB"));
            
            double val = 0.0;
            if (initialWeights != null && i < initialWeights.size()) {
                val = initialWeights.get(i);
            }
            et.setText(formatDouble(val));

            if (i == 0) {
                et.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (isUpdatingSetsUI) return;
                        if (binding.cbSameWeightAllSets.isChecked()) {
                            syncSetWeightsToFirstSet();
                        }
                    }
                });
            } else {
                et.setEnabled(!binding.cbSameWeightAllSets.isChecked());
                et.setAlpha(binding.cbSameWeightAllSets.isChecked() ? 0.6f : 1.0f);
            }

            til.addView(et);
            setWeightInputFields.add(et);

            // Unit label
            TextView tvUnit = new TextView(this);
            String unit = currentVariation != null && currentVariation.getWeightUnit() != null ? currentVariation.getWeightUnit() : "kg";
            tvUnit.setText(unit);
            tvUnit.setTextSize(14);
            tvUnit.setTextColor(Color.parseColor("#E0E0E0"));

            row.addView(tvLabel);
            row.addView(til);
            row.addView(tvUnit);

            binding.llSetWeightsContainer.addView(row);
        }
        isUpdatingSetsUI = false;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private String formatDouble(double val) {
        if (val == (long) val) {
            return String.format(Locale.US, "%d", (long) val);
        } else {
            return String.format(Locale.US, "%.1f", val);
        }
    }

    private void loadVariationData() {
        if (variationId != null) {
            currentVariation = WorkoutDataManager.getInstance().getVariationById(routineId, exerciseId, variationId);
        }

        boolean allSame = currentVariation == null || areAllWeightsEqual(currentVariation.getSetWeights());
        binding.cbSameWeightAllSets.setChecked(allSame);

        if (currentVariation != null) {
            binding.etVariationName.setText(currentVariation.getDisplayName(this));
            binding.etSets.setText(String.valueOf(currentVariation.getSets()));
            binding.etReps.setText(String.valueOf(currentVariation.getRepetitions()));
            binding.etNotes.setText(currentVariation.getDisplayNotes(this));

            binding.cbHasWarmupSet.setChecked(currentVariation.hasWarmupSet());
            binding.llWarmupContainer.setVisibility(currentVariation.hasWarmupSet() ? View.VISIBLE : View.GONE);
            binding.etWarmupWeight.setText(formatDouble(currentVariation.getWarmupWeight()));
            binding.etWarmupReps.setText(String.valueOf(currentVariation.getWarmupReps()));

            if (currentVariation.getImageResource() != 0) {
                selectedImageResId = currentVariation.getImageResource();
                binding.ivVariationDetailImage.setImageResource(selectedImageResId);
            }

            renderSetWeightInputs(currentVariation.getSets(), currentVariation.getSetWeights());
            binding.btnDeleteVariation.setVisibility(View.VISIBLE);
        } else {
            // New variation defaults (initialized to 0 weight for all sets)
            binding.etSets.setText("3");
            binding.etReps.setText("10");
            binding.cbHasWarmupSet.setChecked(false);
            binding.llWarmupContainer.setVisibility(View.GONE);
            binding.etWarmupWeight.setText("0");
            binding.etWarmupReps.setText("10");

            List<Double> initialZeroWeights = new ArrayList<>();
            initialZeroWeights.add(0.0);
            initialZeroWeights.add(0.0);
            initialZeroWeights.add(0.0);
            renderSetWeightInputs(3, initialZeroWeights);

            binding.btnDeleteVariation.setVisibility(View.GONE);
        }
    }

    private void saveVariation() {
        String name = binding.etVariationName.getText() != null ? binding.etVariationName.getText().toString().trim() : "";
        String setsStr = binding.etSets.getText() != null ? binding.etSets.getText().toString().trim() : "";
        String repsStr = binding.etReps.getText() != null ? binding.etReps.getText().toString().trim() : "";
        String notes = binding.etNotes.getText() != null ? binding.etNotes.getText().toString().trim() : "";

        boolean hasError = false;

        if (TextUtils.isEmpty(name)) {
            binding.tilVariationName.setError(getString(R.string.error_empty_variation_name));
            hasError = true;
        } else {
            binding.tilVariationName.setError(null);
        }

        int sets = 0;
        try {
            sets = Integer.parseInt(setsStr);
            if (sets <= 0) {
                binding.tilSets.setError(getString(R.string.error_invalid_sets));
                hasError = true;
            } else {
                binding.tilSets.setError(null);
            }
        } catch (NumberFormatException e) {
            binding.tilSets.setError(getString(R.string.error_invalid_sets));
            hasError = true;
        }

        int reps = 0;
        try {
            reps = Integer.parseInt(repsStr);
            if (reps <= 0) {
                binding.tilReps.setError(getString(R.string.error_invalid_reps));
                hasError = true;
            } else {
                binding.tilReps.setError(null);
            }
        } catch (NumberFormatException e) {
            binding.tilReps.setError(getString(R.string.error_invalid_reps));
            hasError = true;
        }

        if (hasError) return;

        List<Double> setWeights = getCurrentSetWeightsFromUI();
        String unit = "kg";

        double primaryWeight = !setWeights.isEmpty() ? setWeights.get(0) : 0.0;

        boolean hasWarmup = binding.cbHasWarmupSet.isChecked();
        double warmupWeight = 0.0;
        int warmupReps = 10;
        if (hasWarmup) {
            try {
                String wStr = binding.etWarmupWeight.getText() != null ? binding.etWarmupWeight.getText().toString().trim() : "";
                if (!TextUtils.isEmpty(wStr)) warmupWeight = Double.parseDouble(wStr);
            } catch (Exception ignored) {}
            try {
                String rStr = binding.etWarmupReps.getText() != null ? binding.etWarmupReps.getText().toString().trim() : "";
                if (!TextUtils.isEmpty(rStr)) warmupReps = Integer.parseInt(rStr);
            } catch (Exception ignored) {}
        }

        if (currentVariation != null) {
            currentVariation.setName(name);
            currentVariation.setNameResId(0);
            currentVariation.setSets(sets);
            currentVariation.setRepetitions(reps);
            currentVariation.setWeight(primaryWeight);
            currentVariation.setSetWeights(setWeights);
            currentVariation.setWeightUnit(unit);
            currentVariation.setRestSeconds(0);
            currentVariation.setNotes(notes);
            currentVariation.setNotesResId(0);
            currentVariation.setImageResource(selectedImageResId);
            currentVariation.setHasWarmupSet(hasWarmup);
            currentVariation.setWarmupWeight(warmupWeight);
            currentVariation.setWarmupReps(warmupReps);

            WorkoutDataManager.getInstance().updateVariation(routineId, exerciseId, currentVariation);
        } else {
            ExerciseVariation newVar = new ExerciseVariation(
                    UUID.randomUUID().toString(),
                    name, 0,
                    sets, reps, primaryWeight, unit, 0,
                    notes, 0,
                    selectedImageResId
            );
            newVar.setSetWeights(setWeights);
            newVar.setHasWarmupSet(hasWarmup);
            newVar.setWarmupWeight(warmupWeight);
            newVar.setWarmupReps(warmupReps);
            WorkoutDataManager.getInstance().addVariation(routineId, exerciseId, newVar);
        }

        Toast.makeText(this, R.string.msg_saved_successfully, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmDeleteVariation() {
        if (currentVariation == null) return;

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_variation_title)
                .setMessage(getString(R.string.dialog_delete_variation_msg, currentVariation.getDisplayName(this)))
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    WorkoutDataManager.getInstance().deleteVariation(routineId, exerciseId, currentVariation.getId());
                    Toast.makeText(VariationDetailActivity.this, R.string.msg_deleted_successfully, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }
}