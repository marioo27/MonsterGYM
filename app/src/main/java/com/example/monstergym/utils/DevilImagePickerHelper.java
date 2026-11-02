package com.example.monstergym.utils;

import android.content.Context;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ScrollView;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;

import com.example.monstergym.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DevilImagePickerHelper {

    public interface OnImageSelectedListener {
        void onImageSelected(int drawableResId);
    }

    private static class DevilPose {
        String baseName;
        List<Variant> variants = new ArrayList<>();
    }

    private static class Variant {
        int resId;
        String color;
        String displayName;
    }

    private static List<DevilPose> getGroupedPoses(Context context) {
        Map<String, DevilPose> poseMap = new LinkedHashMap<>();
        List<String> knownColors = Arrays.asList("red", "blue", "purple", "yellow", "green", "orange", "pink");

        try {
            Field[] fields = R.drawable.class.getDeclaredFields();
            for (Field field : fields) {
                String name = field.getName().toLowerCase();
                if (name.contains("devil") || name.contains("diablillo")) {
                    int resId = field.getInt(null);
                    if (resId == 0) continue;

                    String baseName = name;
                    String color = "";

                    for (String c : knownColors) {
                        if (name.endsWith("_" + c)) {
                            baseName = name.substring(0, name.length() - c.length() - 1);
                            color = c;
                            break;
                        }
                    }

                    DevilPose pose = poseMap.get(baseName);
                    if (pose == null) {
                        pose = new DevilPose();
                        pose.baseName = baseName;
                        poseMap.put(baseName, pose);
                    }

                    boolean exists = false;
                    for (Variant v : pose.variants) {
                        if (v.resId == resId) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        Variant v = new Variant();
                        v.resId = resId;
                        v.color = color;
                        v.displayName = color.isEmpty() ? "Default" : color.substring(0, 1).toUpperCase() + color.substring(1);
                        pose.variants.add(v);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (DevilPose pose : poseMap.values()) {
            pose.variants.sort((v1, v2) -> {
                int idx1 = knownColors.indexOf(v1.color);
                int idx2 = knownColors.indexOf(v2.color);
                if (idx1 != -1 && idx2 != -1) {
                    return Integer.compare(idx1, idx2);
                }
                return v1.color.compareTo(v2.color);
            });
        }

        // Fallback in case reflection yields no results
        if (poseMap.isEmpty()) {
            DevilPose pose = new DevilPose();
            pose.baseName = "devil_mad";
            Variant v = new Variant();
            v.resId = R.drawable.devil_mad_red;
            v.color = "red";
            v.displayName = "Red";
            pose.variants.add(v);
            poseMap.put(pose.baseName, pose);
        }

        return new ArrayList<>(poseMap.values());
    }

    public static void showPicker(Context context, OnImageSelectedListener listener) {
        if (context == null || listener == null) return;

        List<DevilPose> poses = getGroupedPoses(context);
        if (poses.isEmpty()) return;

        int dp8 = dpToPx(context, 8);
        int dp16 = dpToPx(context, 16);
        int dp64 = dpToPx(context, 64);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setPadding(dp16, dp16, dp16, dp16);

        GridLayout gridLayout = new GridLayout(context);
        gridLayout.setColumnCount(3);
        gridLayout.setAlignmentMode(GridLayout.ALIGN_BOUNDS);

        AlertDialog[] dialogHolder = new AlertDialog[1];

        for (DevilPose pose : poses) {
            Variant repVariant = pose.variants.get(0);

            CardView card = new CardView(context);
            card.setRadius(dpToPx(context, 16));
            card.setCardElevation(dpToPx(context, 4));
            card.setCardBackgroundColor(Color.parseColor("#1E293B"));
            card.setUseCompatPadding(true);

            ImageView iv = new ImageView(context);
            iv.setLayoutParams(new ViewGroup.LayoutParams(dp64, dp64));
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setImageResource(repVariant.resId);
            iv.setPadding(dp8, dp8, dp8, dp8);

            card.addView(iv);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
            params.setMargins(dp8, dp8, dp8, dp8);
            card.setLayoutParams(params);

            card.setOnClickListener(v -> {
                if (dialogHolder[0] != null) {
                    dialogHolder[0].dismiss();
                }

                if (pose.variants.size() == 1) {
                    listener.onImageSelected(pose.variants.get(0).resId);
                } else {
                    showColorPicker(context, pose, listener);
                }
            });

            gridLayout.addView(card);
        }

        scrollView.addView(gridLayout);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.select_devil_image_title)
                .setView(scrollView)
                .setNegativeButton(R.string.btn_cancel, (d, which) -> d.dismiss())
                .create();

        dialogHolder[0] = dialog;
        dialog.show();
    }

    private static void showColorPicker(Context context, DevilPose pose, OnImageSelectedListener listener) {
        int dp8 = dpToPx(context, 8);
        int dp16 = dpToPx(context, 16);
        int dp64 = dpToPx(context, 64);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setPadding(dp16, dp16, dp16, dp16);

        GridLayout gridLayout = new GridLayout(context);
        gridLayout.setColumnCount(3);
        gridLayout.setAlignmentMode(GridLayout.ALIGN_BOUNDS);

        AlertDialog[] colorDialogHolder = new AlertDialog[1];

        for (Variant variant : pose.variants) {
            CardView card = new CardView(context);
            card.setRadius(dpToPx(context, 16));
            card.setCardElevation(dpToPx(context, 4));
            card.setCardBackgroundColor(Color.parseColor("#1E293B"));
            card.setUseCompatPadding(true);

            ImageView iv = new ImageView(context);
            iv.setLayoutParams(new ViewGroup.LayoutParams(dp64, dp64));
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setImageResource(variant.resId);
            iv.setPadding(dp8, dp8, dp8, dp8);

            card.addView(iv);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
            params.setMargins(dp8, dp8, dp8, dp8);
            card.setLayoutParams(params);

            card.setOnClickListener(v -> {
                if (colorDialogHolder[0] != null) {
                    colorDialogHolder[0].dismiss();
                }
                listener.onImageSelected(variant.resId);
            });

            gridLayout.addView(card);
        }

        scrollView.addView(gridLayout);

        AlertDialog colorDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.select_devil_color_title)
                .setView(scrollView)
                .setNegativeButton(R.string.btn_cancel, (d, which) -> {
                    d.dismiss();
                    showPicker(context, listener);
                })
                .create();

        colorDialogHolder[0] = colorDialog;
        colorDialog.show();
    }

    private static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
