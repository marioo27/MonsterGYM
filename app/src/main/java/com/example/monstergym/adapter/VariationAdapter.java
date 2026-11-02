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
import com.example.monstergym.databinding.ItemVariationBinding;
import com.example.monstergym.model.ExerciseVariation;

import java.util.Collections;
import java.util.List;

public class VariationAdapter extends RecyclerView.Adapter<VariationAdapter.VariationViewHolder> {

    public interface OnVariationClickListener {
        void onOpenVariation(ExerciseVariation variation);
        void onEditVariation(ExerciseVariation variation);
        void onDeleteVariation(ExerciseVariation variation);
    }

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    private final Context context;
    private List<ExerciseVariation> variations;
    private final OnVariationClickListener listener;
    private OnStartDragListener dragListener;

    public VariationAdapter(Context context, List<ExerciseVariation> variations, OnVariationClickListener listener) {
        this(context, variations, listener, null);
    }

    public VariationAdapter(Context context, List<ExerciseVariation> variations, OnVariationClickListener listener, OnStartDragListener dragListener) {
        this.context = context;
        this.variations = variations;
        this.listener = listener;
        this.dragListener = dragListener;
    }

    public void setOnStartDragListener(OnStartDragListener dragListener) {
        this.dragListener = dragListener;
    }

    public void setVariations(List<ExerciseVariation> variations) {
        this.variations = variations;
        notifyDataSetChanged();
    }

    public List<ExerciseVariation> getVariations() {
        return variations;
    }

    public boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(variations, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(variations, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @NonNull
    @Override
    public VariationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVariationBinding binding = ItemVariationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VariationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VariationViewHolder holder, int position) {
        holder.bind(variations.get(position), context, listener, dragListener);
    }

    @Override
    public int getItemCount() {
        return variations != null ? variations.size() : 0;
    }

    public static class VariationViewHolder extends RecyclerView.ViewHolder {
        private final ItemVariationBinding binding;

        public VariationViewHolder(ItemVariationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ExerciseVariation variation, Context context, OnVariationClickListener listener, OnStartDragListener dragListener) {
            binding.tvVariationName.setText(variation.getDisplayName(context));

            String setsRepsWeight = context.getString(R.string.sets_reps_format, variation.getSets(), variation.getRepetitions())
                    + " • " + variation.getDisplayWeights() + " " + variation.getWeightUnit();
            binding.tvSetsRepsWeight.setText(setsRepsWeight);

            String notes = variation.getDisplayNotes(context);
            if (TextUtils.isEmpty(notes)) {
                binding.tvVariationNotes.setVisibility(View.GONE);
            } else {
                binding.tvVariationNotes.setVisibility(View.VISIBLE);
                binding.tvVariationNotes.setText(notes);
            }

            if (variation.getImageResource() != 0 && variation.getImageResource() != R.drawable.image_placeholder) {
                binding.ivVariationImage.setImageResource(variation.getImageResource());
            } else {
                binding.ivVariationImage.setImageResource(R.drawable.devil_mad_red);
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onOpenVariation(variation);
            });

            binding.btnEditVariation.setOnClickListener(v -> {
                if (listener != null) listener.onEditVariation(variation);
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
