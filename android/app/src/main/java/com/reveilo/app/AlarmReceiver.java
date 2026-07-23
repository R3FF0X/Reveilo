package com.reveilo.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Déclenché par AlarmManager à l'heure exacte de l'alarme. Délègue tout
 * (son, vibration, notification plein écran avec bouton "Arrêter") au
 * AlarmRingingService, qui reste actif indépendamment de l'écran affiché.
 */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, AlarmRingingService.class);
        serviceIntent.putExtras(intent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
