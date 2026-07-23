package com.reveilo.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Android efface les alarmes programmées via AlarmManager après un
 * redémarrage : on les reprogramme toutes depuis la liste sauvegardée.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            AlarmScheduler.rescheduleFromStorage(context);
        }
    }
}
