package com.example.lab5.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.example.lab5.R;
import com.example.lab5.data.entity.ConflictRule;
import com.example.lab5.domain.DetectedConflict;
import com.example.lab5.domain.RiskSummary;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

// Вынес оформление уровней риска в общий помощник, чтобы цвет, значок и подпись совпадали на всех экранах.
public final class ConflictUiHelper {

    private ConflictUiHelper() {
    }

    @ColorInt
    public static int foregroundColor(@NonNull Context context, @NonNull String severity) {
        if (ConflictRule.SEVERITY_HIGH.equals(severity)) {
            return ContextCompat.getColor(context, R.color.risk_high_fg);
        }
        if (ConflictRule.SEVERITY_MEDIUM.equals(severity)) {
            return ContextCompat.getColor(context, R.color.risk_medium_fg);
        }
        if (ConflictRule.SEVERITY_LOW.equals(severity)) {
            return ContextCompat.getColor(context, R.color.risk_low_fg);
        }
        return ContextCompat.getColor(context, R.color.risk_safe_fg);
    }

    @ColorInt
    public static int backgroundColor(@NonNull Context context, @NonNull String severity) {
        if (ConflictRule.SEVERITY_HIGH.equals(severity)) {
            return ContextCompat.getColor(context, R.color.risk_high_bg);
        }
        if (ConflictRule.SEVERITY_MEDIUM.equals(severity)) {
            return ContextCompat.getColor(context, R.color.risk_medium_bg);
        }
        if (ConflictRule.SEVERITY_LOW.equals(severity)) {
            return ContextCompat.getColor(context, R.color.risk_low_bg);
        }
        return ContextCompat.getColor(context, R.color.risk_safe_bg);
    }

    @ColorInt
    public static int borderColor(@NonNull Context context, @NonNull String severity) {
        if (ConflictRule.SEVERITY_HIGH.equals(severity)) {
            return ContextCompat.getColor(context, R.color.risk_high_border);
        }
        if (ConflictRule.SEVERITY_MEDIUM.equals(severity)) {
            return ContextCompat.getColor(context, R.color.risk_medium_border);
        }
        if (ConflictRule.SEVERITY_LOW.equals(severity)) {
            return ContextCompat.getColor(context, R.color.risk_low_border);
        }
        return ContextCompat.getColor(context, R.color.risk_safe_border);
    }

    @DrawableRes
    public static int iconRes(@NonNull String severity) {
        if (ConflictRule.SEVERITY_HIGH.equals(severity)) {
            return R.drawable.ic_warning_high;
        }
        if (ConflictRule.SEVERITY_MEDIUM.equals(severity)) {
            return R.drawable.ic_warning_medium;
        }
        if (ConflictRule.SEVERITY_LOW.equals(severity)) {
            return R.drawable.ic_info_low;
        }
        return R.drawable.ic_check_safe;
    }

    @NonNull
    public static String severityLabel(@NonNull Context context, @NonNull String severity) {
        if (ConflictRule.SEVERITY_HIGH.equals(severity)) {
            return context.getString(R.string.severity_high_label);
        }
        if (ConflictRule.SEVERITY_MEDIUM.equals(severity)) {
            return context.getString(R.string.severity_medium_label);
        }
        if (ConflictRule.SEVERITY_LOW.equals(severity)) {
            return context.getString(R.string.severity_low_label);
        }
        return context.getString(R.string.severity_safe_label);
    }

    @NonNull
    public static String bannerTitle(@NonNull Context context, @NonNull String overallLevel) {
        if (RiskSummary.LEVEL_HIGH.equals(overallLevel)) {
            return context.getString(R.string.banner_high_title);
        }
        if (RiskSummary.LEVEL_MEDIUM.equals(overallLevel)) {
            return context.getString(R.string.banner_medium_title);
        }
        if (RiskSummary.LEVEL_LOW.equals(overallLevel)) {
            return context.getString(R.string.banner_low_title);
        }
        return context.getString(R.string.banner_safe_title);
    }

    // Заполняю контейнер карточками найденных конфликтов с цветом, значком и рекомендацией.
    public static void populateConflictRows(
            @NonNull Context context,
            @NonNull LinearLayout container,
            @NonNull List<DetectedConflict> conflicts
    ) {
        container.removeAllViews();
        if (conflicts.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }

        container.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(context);

        for (DetectedConflict conflict : conflicts) {
            View row = inflater.inflate(R.layout.item_conflict_row, container, false);
            MaterialCardView card = row.findViewById(R.id.cardConflictItem);
            ImageView icon = row.findViewById(R.id.ivConflictSeverityIcon);
            TextView badge = row.findViewById(R.id.tvConflictSeverityBadge);
            TextView pair = row.findViewById(R.id.tvConflictPair);
            TextView title = row.findViewById(R.id.tvConflictTitle);
            TextView recommendation = row.findViewById(R.id.tvConflictRecommendation);

            int fg = foregroundColor(context, conflict.severity);
            int bg = backgroundColor(context, conflict.severity);
            int border = borderColor(context, conflict.severity);

            card.setStrokeColor(border);
            icon.setImageResource(iconRes(conflict.severity));
            icon.setImageTintList(ColorStateList.valueOf(fg));

            badge.setText(severityLabel(context, conflict.severity));
            badge.setTextColor(fg);
            ViewCompat.setBackgroundTintList(badge, ColorStateList.valueOf(bg));

            pair.setText(conflict.medicineAName + " (" + conflict.substanceAName + ") + "
                    + conflict.medicineBName + " (" + conflict.substanceBName + ")");
            title.setText(conflict.rule.title);
            recommendation.setText(conflict.rule.recommendation);

            container.addView(row);
        }
    }
}
