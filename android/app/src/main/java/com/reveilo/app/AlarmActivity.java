package com.reveilo.app;

import android.app.Activity;
import android.app.KeyguardManager;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Écran de sonnerie plein écran : s'affiche par-dessus le verrouillage, joue
 * un son en boucle sur le flux "alarme" (qui passe outre le mode silencieux)
 * et fait vibrer le téléphone, jusqu'à ce que l'utilisateur appuie sur
 * "Arrêter". Si l'alarme est récurrente, reprogramme alors sa prochaine
 * occurrence.
 */
public class AlarmActivity extends Activity {
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    private String id;
    private String time;
    private String label;
    private String weekParity;
    private int[] repeatDays;

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
        weekParity = getIntent().getStringExtra("weekParity");
        repeatDays = getIntent().getIntArrayExtra("repeatDays");

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
        stopButton.setBackgroundColor(Color.parseColor("#ea580c"));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            300
        );
        buttonParams.topMargin = 96;
        stopButton.setLayoutParams(buttonParams);
        stopButton.setOnClickListener(v -> stopAlarm());
        layout.addView(stopButton);

        setContentView(layout);

        startRinging();
    }

    private void startRinging() {
        try {
            Uri alarmUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getValidRingtoneUri(this);
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, alarmUri);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                );
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            }
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            // le son n'a pas pu démarrer, la vibration prend le relais
        }

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) {
            long[] pattern = {0, 500, 500};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    private void stopAlarm() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
                // ignore
            }
            mediaPlayer = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
        }

        if (id != null) {
            android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancel(id.hashCode());
            }
        }

        if (id != null && time != null && repeatDays != null && repeatDays.length > 0) {
            AlarmScheduler.scheduleNext(getApplicationContext(), id, time, repeatDays, weekParity, label);
        }

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
                // ignore
            }
            mediaPlayer = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }
}
