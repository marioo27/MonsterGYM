package com.example.monstergym.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.monstergym.R;
import com.example.monstergym.databinding.ItemRoutineBinding;
import com.example.monstergym.model.Routine;

import java.util.Collections;
import java.util.List;

public class RoutineAdapter extends RecyclerView.Adapter<RoutineAdapter.RoutineViewHolder> {

    public interface OnRoutineClickListener {
        void onOpenRoutine(Routine routine);
        void onEditRoutine(Routine routine);
        void onDeleteRoutine(Routine routine);
    }

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    private final Context context;
    private List<Routine> routines;
    private final OnRoutineClickListener listener;
    private OnStartDragListener dragListener;

    public RoutineAdapter(Context context, List<Routine> routines, OnRoutineClickListener listener) {
        this(context, routines, listener, null);
    }

    public RoutineAdapter(Context context, List<Routine> routines, OnRoutineClickListener listener, OnStartDragListener dragListener) {
        this.context = context;
        this.routines = routines;
        this.listener = listener;
        this.dragListener = dragListener;
    }

    public void setOnStartDragListener(OnStartDragListener dragListener) {
        this.dragListener = dragListener;
    }

    public void setRoutines(List<Routine> routines) {
        this.routines = routines;
        notifyDataSetChanged();
    }

    public List<Routine> getRoutines() {
        return routines;
    }

    public boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(routines, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(routines, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @NonNull
    @Override
    public RoutineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRoutineBinding binding = ItemRoutineBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RoutineViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RoutineViewHolder holder, int position) {
        holder.bind(routines.get(position), context, listener, dragListener);
    }

    @Override
    public int getItemCount() {
        return routines != null ? routines.size() : 0;
    }

    public static class RoutineViewHolder extends RecyclerView.ViewHolder {
        private final ItemRoutineBinding binding;

        public RoutineViewHolder(ItemRoutineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Routine routine, Context context, OnRoutineClickListener listener, OnStartDragListener dragListener) {
            binding.tvRoutineName.setText(routine.getDisplayName(context));

            String desc = routine.getDisplayDescription(context);
            String abbrev = routine.getAbbreviation();
            String fullDesc = "";
            if (!TextUtils.isEmpty(abbrev)) {
                fullDesc = "[" + abbrev + "] ";
            }
            if (!TextUtils.isEmpty(desc)) {
                fullDesc += desc;
            }

            if (TextUtils.isEmpty(fullDesc)) {
                binding.tvRoutineDescription.setVisibility(View.GONE);
            } else {
                binding.tvRoutineDescription.setVisibility(View.VISIBLE);
                binding.tvRoutineDescription.setText(fullDesc);
            }

            int exCount = routine.getExercises() != null ? routine.getExercises().size() : 0;
            binding.tvExercisesCount.setText(context.getString(R.string.exercises_count_format, exCount));

            if (routine.getImageResource() != 0 && routine.getImageResource() != R.drawable.ic_dumbbell) {
                binding.ivRoutineIcon.setImageResource(routine.getImageResource());
            } else {
                binding.ivRoutineIcon.setImageResource(R.drawable.devil_mad_red);
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onOpenRoutine(routine);
            });

            binding.btnEditRoutine.setOnClickListener(v -> {
                if (listener != null) listener.onEditRoutine(routine);
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
