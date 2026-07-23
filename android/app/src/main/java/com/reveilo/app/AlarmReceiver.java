package com.reveilo.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

/**
 * Déclenché par AlarmManager à l'heure exacte de l'alarme. Plutôt que de
 * lancer l'écran directement (souvent bloqué en arrière-plan par Android et
 * surtout par MIUI), on passe par une notification prioritaire avec un
 * "full-screen intent" : c'est le mécanisme officiellement autorisé pour
 * forcer l'affichage d'un écran même téléphone verrouillé, exactement comme
 * les appels entrants ou les vrais réveils.
 */
public class AlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "reveilo_alarms";

    @Override
    public void onReceive(Context context, Intent intent) {
        createChannelIfNeeded(context);

        Intent activityIntent = new Intent(context, AlarmActivity.class);
        activityIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );
        activityIntent.putExtras(intent);

        String id = intent.getStringExtra("id");
        int requestCode = id != null ? id.hashCode() : 0;

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String label = intent.getStringExtra("label");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle((label == null || label.isEmpty()) ? "Réveilo" : label)
            .setContentText("Alarme")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(true)
            .setOngoing(true);

        NotificationManager notificationManager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(requestCode, builder.build());
        }
    }

    private void createChannelIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null && notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Alarmes Réveilo",
                    NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Utilisé pour déclencher l'écran de sonnerie des alarmes");
                channel.setBypassDnd(true);
                channel.enableVibration(false);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
