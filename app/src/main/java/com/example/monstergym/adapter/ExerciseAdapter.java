package com.example.monstergym.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.monstergym.R;
import com.example.monstergym.databinding.ItemExerciseBinding;
import com.example.monstergym.model.Exercise;
import com.example.monstergym.model.ExerciseVariation;

import java.util.Collections;
import java.util.List;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder> {

    public interface OnExerciseClickListener {
        void onOpenExercise(Exercise exercise);
        void onEditExercise(Exercise exercise);
        void onDeleteExercise(Exercise exercise);
    }

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    private final Context context;
    private List<Exercise> exercises;
    private final OnExerciseClickListener listener;
    private OnStartDragListener dragListener;

    public ExerciseAdapter(Context context, List<Exercise> exercises, OnExerciseClickListener listener) {
        this(context, exercises, listener, null);
    }

    public ExerciseAdapter(Context context, List<Exercise> exercises, OnExerciseClickListener listener, OnStartDragListener dragListener) {
        this.context = context;
        this.exercises = exercises;
        this.listener = listener;
        this.dragListener = dragListener;
    }

    public void setOnStartDragListener(OnStartDragListener dragListener) {
        this.dragListener = dragListener;
    }

    public void setExercises(List<Exercise> exercises) {
        this.exercises = exercises;
        notifyDataSetChanged();
    }

    public List<Exercise> getExercises() {
        return exercises;
    }

    public boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(exercises, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(exercises, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemExerciseBinding binding = ItemExerciseBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ExerciseViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        holder.bind(exercises.get(position), context, listener, dragListener);
    }

    @Override
    public int getItemCount() {
        return exercises != null ? exercises.size() : 0;
    }

    public static class ExerciseViewHolder extends RecyclerView.ViewHolder {
        private final ItemExerciseBinding binding;

        public ExerciseViewHolder(ItemExerciseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Exercise exercise, Context context, OnExerciseClickListener listener, OnStartDragListener dragListener) {
            binding.tvExerciseName.setText(exercise.getDisplayName(context));

            if (exercise.isExtra()) {
                binding.tvExtraBadge.setVisibility(View.VISIBLE);
            } else {
                binding.tvExtraBadge.setVisibility(View.GONE);
            }

            int varCount = exercise.getVariations() != null ? exercise.getVariations().size() : 0;
            binding.tvVariationsCount.setText(context.getString(R.string.variations_count_format, varCount));

            if (exercise.getVariations() != null && !exercise.getVariations().isEmpty()) {
                ExerciseVariation firstVar = exercise.getVariations().get(0);
                String summary = context.getString(R.string.sets_reps_format, firstVar.getSets(), firstVar.getRepetitions())
                        + " (" + firstVar.getDisplayWeights() + " " + firstVar.getWeightUnit() + ")";
                binding.tvSetsRepsSummary.setText(summary);
                binding.tvSetsRepsSummary.setVisibility(View.VISIBLE);
            } else {
                binding.tvSetsRepsSummary.setVisibility(View.GONE);
            }

            if (exercise.getImageResource() != 0 && exercise.getImageResource() != R.drawable.image_placeholder) {
                binding.ivExerciseImage.setImageResource(exercise.getImageResource());
            } else {
                binding.ivExerciseImage.setImageResource(R.drawable.devil_mad_red);
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onOpenExercise(exercise);
            });

            binding.btnEditExercise.setOnClickListener(v -> {
                if (listener != null) listener.onEditExercise(exercise);
            });

            binding.ivDragHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN && dragListener != null) {
                    dragListener.onStartDrag(this);
                }
                return false;
            });
        }
    }
}
