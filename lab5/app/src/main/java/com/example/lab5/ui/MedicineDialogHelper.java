package com.example.lab5.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.lab5.R;
import com.example.lab5.data.entity.ActiveSubstance;
import com.example.lab5.data.entity.Medicine;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

// Вынес диалог создания и редактирования препарата в отдельный класс, чтобы использовать его на обоих экранах.
public final class MedicineDialogHelper {

    public interface OnMedicineSavedListener {
        void onSaved(@NonNull Medicine medicine, boolean isEdit);
    }

    private MedicineDialogHelper() {
    }

    public static void show(
            @NonNull Context context,
            @NonNull List<ActiveSubstance> substances,
            @Nullable Medicine existing,
            @NonNull OnMedicineSavedListener listener
    ) {
        if (substances.isEmpty()) {
            return;
        }

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_medicine, null);
        TextInputLayout tilName = dialogView.findViewById(R.id.tilMedicineName);
        TextInputLayout tilDosage = dialogView.findViewById(R.id.tilDosage);
        TextInputLayout tilSubstance = dialogView.findViewById(R.id.tilSubstance);

        TextInputEditText etName = dialogView.findViewById(R.id.etMedicineName);
        TextInputEditText etDosage = dialogView.findViewById(R.id.etDosage);
        AutoCompleteTextView actvSubstance = dialogView.findViewById(R.id.actvSubstance);
        TextInputEditText etSchedule = dialogView.findViewById(R.id.etSchedule);
        TextInputEditText etNotes = dialogView.findViewById(R.id.etNotes);

        ArrayAdapter<ActiveSubstance> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_dropdown_item_1line,
                substances
        );
        actvSubstance.setAdapter(adapter);

        final int[] selectedSubstanceId = {substances.get(0).id};
        if (existing != null) {
            etName.setText(existing.name);
            etDosage.setText(existing.dosage);
            etSchedule.setText(existing.scheduleTime);
            etNotes.setText(existing.notes);
            selectedSubstanceId[0] = existing.substanceId;
            for (ActiveSubstance sub : substances) {
                if (sub.id == existing.substanceId) {
                    actvSubstance.setText(sub.name, false);
                    break;
                }
            }
        } else {
            actvSubstance.setText(substances.get(0).name, false);
        }

        actvSubstance.setOnItemClickListener((parent, view, position, id) -> {
            ActiveSubstance chosen = substances.get(position);
            selectedSubstanceId[0] = chosen.id;
            tilSubstance.setError(null);
        });

        boolean isEdit = existing != null;
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(isEdit ? R.string.dialog_edit_title : R.string.dialog_add_title)
                .setView(dialogView)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Проверяю обязательные поля до закрытия диалога, чтобы пустой ввод никогда не ломал приложение.
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String dosage = etDosage.getText() != null ? etDosage.getText().toString().trim() : "";
            String schedule = etSchedule.getText() != null ? etSchedule.getText().toString().trim() : "";
            String notes = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";

            boolean valid = true;
            if (name.isEmpty()) {
                tilName.setError(context.getString(R.string.error_name_required));
                valid = false;
            } else {
                tilName.setError(null);
            }

            if (dosage.isEmpty()) {
                tilDosage.setError(context.getString(R.string.error_dosage_required));
                valid = false;
            } else {
                tilDosage.setError(null);
            }

            if (selectedSubstanceId[0] <= 0) {
                tilSubstance.setError(context.getString(R.string.error_substance_required));
                valid = false;
            } else {
                tilSubstance.setError(null);
            }

            if (!valid) {
                return;
            }

            Medicine result;
            if (isEdit) {
                result = new Medicine(existing.id, name, dosage, selectedSubstanceId[0], schedule, notes);
            } else {
                result = new Medicine(name, dosage, selectedSubstanceId[0], schedule, notes);
            }
            listener.onSaved(result, isEdit);
            dialog.dismiss();
        }));

        dialog.show();
    }
}
