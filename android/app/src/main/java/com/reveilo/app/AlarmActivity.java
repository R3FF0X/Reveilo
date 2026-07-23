package com.reveilo.app;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Écran de sonnerie plein écran : s'affiche par-dessus le verrouillage. Le
 * son et la vibration sont gérés par AlarmRingingService (qui continue même
 * si cet écran ne s'affiche pas) ; cette activité ne fait qu'afficher
 * l'heure/le titre et proposer le bouton "Arrêter".
 */
public class AlarmActivity extends Activity {
    private String id;
    private String time;
    private String label;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
        }
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        id = getIntent().getStringExtra("id");
        time = getIntent().getStringExtra("time");
        label = getIntent().getStringExtra("label");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#1b1d21"));
        layout.setPadding(48, 48, 48, 48);

        TextView clock = new TextView(this);
        clock.setText(time == null ? "" : time);
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(48);
        clock.setGravity(Gravity.CENTER);
        layout.addView(clock);

        TextView title = new TextView(this);
        title.setText((label == null || label.isEmpty()) ? "Réveilo" : label);
        title.setTextColor(Color.parseColor("#d4d4d8"));
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.topMargin = 16;
        title.setLayoutParams(titleParams);
        layout.addView(title);

        Button stopButton = new Button(this);
        stopButton.setText("Arrêter");
        stopButton.setTextColor(Color.WHITE);
        stopButton.setAllCaps(false);

        GradientDrawable buttonBackground = new GradientDrawable();
        buttonBackground.setColor(Color.parseColor("#ea580c"));
        buttonBackground.setCornerRadius(dp(24));
        stopButton.setBackground(buttonBackground);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (int) dp(56)
        );
        buttonParams.topMargin = (int) dp(48);
        stopButton.setLayoutParams(buttonParams);
        stopButton.setOnClickListener(v -> stopAlarm());
        layout.addView(stopButton);

        setContentView(layout);
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private void stopAlarm() {
        Intent stopIntent = new Intent(this, AlarmRingingService.class);
        stopIntent.setAction(AlarmRingingService.ACTION_STOP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(stopIntent);
        } else {
            startService(stopIntent);
        }
        finish();
    }
}
