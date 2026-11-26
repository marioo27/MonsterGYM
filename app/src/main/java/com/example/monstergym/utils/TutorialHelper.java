package com.example.monstergym.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.monstergym.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

public class TutorialHelper {

    private static final String PREF_NAME = "monstergym_tutorial_hints";

    public static void showFirstTimeHint(Activity activity, String hintKey, String title, String message) {
        if (activity == null || activity.isFinishing()) return;
        SharedPreferences prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        if (prefs.getBoolean(hintKey, false)) {
            return; // Already shown
        }

        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_tutorial_hint, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvHintTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvHintMessage);
        MaterialCheckBox cbDontShowAgain = dialogView.findViewById(R.id.cbDontShowAgain);
        MaterialButton btnGotIt = dialogView.findViewById(R.id.btnGotIt);

        tvTitle.setText("💡 " + title);
        tvMessage.setText(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnGotIt.setOnClickListener(v -> {
            if (cbDontShowAgain.isChecked()) {
                prefs.edit().putBoolean(hintKey, true).apply();
            }
            dialog.dismiss();
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    public static void showFullAppTutorial(Context context) {
        if (context == null) return;

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_full_tutorial, null);
        TextView tvStepTitle = dialogView.findViewById(R.id.tvStepTitle);
        TextView tvStepContent = dialogView.findViewById(R.id.tvStepContent);
        MaterialButton btnPrev = dialogView.findViewById(R.id.btnTutorialPrev);
        MaterialButton btnNext = dialogView.findViewById(R.id.btnTutorialNext);

        final String[] titles = {
                "1️⃣ Gestión de Rutinas",
                "2️⃣ Ejercicios y Variaciones",
                "3️⃣ Registro y Recaps en Calendario",
                "4️⃣ Rachas y Copias de Seguridad"
        };

        final String[] contents = {
                "• <b>Crear Rutinas</b>: Puedes añadir rutinas desde el panel principal.<br/>" +
                        "• <b>Reordenar</b>: Arrastra el icono de las barras para organizar tus rutinas.<br/>" +
                        "• <b>Personajes Diablillos</b>: Elige la imagen de tu personaje para cada rutina y ejercicio.",

                "• <b>Series y Pesos</b>: Configura el número de series, repeticiones y pesos por serie.<br/>" +
                        "• <b>Mismo Peso</b>: Marca la casilla de <i>'Mismo peso en todas las series'</i> para escribir el peso una sola vez.<br/>" +
                        "• <b>Calentamiento</b>: Incluye una serie de calentamiento con su peso específico.",

                "• <b>Regitrar Entrenamiento</b>: Toca cualquier día del calendario y marca la rutina y los ejercicios o variaciones que realizaste.<br/>" +
                        "• <b>Abreviaturas</b>: En el calendario verás las iniciales (ej. SUP, INF) sobre un círculo naranja.<br/>" +
                        "• <b>Recap por Día</b>: Toca cualquier día registrado para ver el detalle de los ejercicios completados.",

                "• <b>Recap Semanal y de Curso</b>: Pulsa sobre las tarjetas naranjas de <b>'🔥Esta semana🔥'</b> o <b>'🔥Este Curso🔥'</b> para ver el desglose con contador (<b>xN</b>) de tus entrenamientos.<br/>" +
                        "• <b>Respaldos JSON</b>: Desde Ajustes puedes exportar o cargar tus datos en JSON sin perder nada."
        };

        final int[] currentStep = {0};

        Runnable updateUI = () -> {
            tvStepTitle.setText(titles[currentStep[0]]);
            tvStepContent.setText(Html.fromHtml(contents[currentStep[0]], Html.FROM_HTML_MODE_LEGACY));
            btnPrev.setVisibility(currentStep[0] == 0 ? View.INVISIBLE : View.VISIBLE);
            btnNext.setText(currentStep[0] == titles.length - 1 ? "Entendido" : "Siguiente");
        };

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        updateUI.run();

        btnPrev.setOnClickListener(v -> {
            if (currentStep[0] > 0) {
                currentStep[0]--;
                updateUI.run();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentStep[0] < titles.length - 1) {
                currentStep[0]++;
                updateUI.run();
            } else {
                dialog.dismiss();
            }
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    public static void resetAllHints(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        Toast.makeText(context, "Se han restablecido los consejos del tutorial.", Toast.LENGTH_SHORT).show();
    }
}
