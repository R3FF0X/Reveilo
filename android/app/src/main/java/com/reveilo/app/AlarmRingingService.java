package com.reveilo.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * Service au premier plan qui joue le son d'alarme et fait vibrer le
 * téléphone, indépendamment de l'écran de sonnerie (AlarmActivity). Il porte
 * aussi la notification plein écran avec un bouton "Arrêter" utilisable
 * directement depuis le volet de notifications, sans déverrouiller l'écran.
 */
public class AlarmRingingService extends Service {
    public static final String ACTION_STOP = "com.reveilo.app.ACTION_STOP_ALARM";
    private static final String CHANNEL_ID = "reveilo_alarms_v2";
    private static final int NOTIFICATION_ID = 1;

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    private String id;
    private String time;
    private String label;
    private String weekParity;
    private int[] repeatDays;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopRinging(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null) {
            id = intent.getStringExtra("id");
            time = intent.getStringExtra("time");
            label = intent.getStringExtra("label");
            weekParity = intent.getStringExtra("weekParity");
            repeatDays = intent.getIntArrayExtra("repeatDays");
        }

        createChannelIfNeeded();
        startForeground(NOTIFICATION_ID, buildNotification());
        startRinging();
        return START_NOT_STICKY;
    }

    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null && notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Alarmes Réveilo",
                    NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Utilisé pour déclencher l'écran de sonnerie des alarmes");
                channel.setBypassDnd(true);
                channel.enableVibration(false);
                channel.setSound(null, null);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private android.app.Notification buildNotification() {
        int requestCode = id != null ? id.hashCode() : 0;

        Intent activityIntent = new Intent(this, AlarmActivity.class);
        activityIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );
        activityIntent.putExtra("id", id);
        activityIntent.putExtra("label", label);
        activityIntent.putExtra("time", time);
        activityIntent.putExtra("repeatDays", repeatDays);
        activityIntent.putExtra("weekParity", weekParity);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(this, AlarmRingingService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
            this,
            requestCode,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle((label == null || label.isEmpty()) ? "Réveilo" : label)
            .setContentText("Alarme en cours")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(0, "Arrêter", stopPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build();
    }

    private void startRinging() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound);
            if (mediaPlayer != null) {
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
                mediaPlayer.start();
            }
        } catch (Exception e) {
            android.util.Log.e("Reveilo", "Impossible de démarrer le son d'alarme", e);
        }

        vibrator = getSystemService(Vibrator.class);
        if (vibrator != null) {
            long[] pattern = {0, 500, 500};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    private void stopRinging(boolean rescheduleIfRepeating) {
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
            vibrator = null;
        }

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }

        if (rescheduleIfRepeating && id != null && time != null && repeatDays != null && repeatDays.length > 0) {
            AlarmScheduler.scheduleNext(getApplicationContext(), id, time, repeatDays, weekParity, label);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRinging(false);
    }
}
