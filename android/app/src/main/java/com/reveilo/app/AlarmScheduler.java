package com.reveilo.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * Logique de programmation des alarmes côté natif : calcule la prochaine
 * occurrence (jours de répétition + semaines paires/impaires) et programme
 * un vrai réveil via AlarmManager.setAlarmClock, indépendamment de la partie
 * JS/WebView. La liste complète est aussi persistée en SharedPreferences pour
 * pouvoir tout reprogrammer après un redémarrage du téléphone.
 */
public class AlarmScheduler {

    private static final String PREFS_NAME = "reveilo_alarms";
    private static final String KEY_ALARMS = "alarms_json";

    public static void syncAll(Context context, JSONArray alarmsJson) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String previousJson = prefs.getString(KEY_ALARMS, "[]");
        try {
            JSONArray previous = new JSONArray(previousJson);
            for (int i = 0; i < previous.length(); i++) {
                JSONObject a = previous.getJSONObject(i);
                cancel(context, a.getString("id"));
            }
        } catch (Exception e) {
            // ignore
        }

        prefs.edit().putString(KEY_ALARMS, alarmsJson.toString()).apply();
        scheduleAllFrom(context, alarmsJson);
    }

    public static void rescheduleFromStorage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_ALARMS, "[]");
        try {
            scheduleAllFrom(context, new JSONArray(json));
        } catch (Exception e) {
            // ignore
        }
    }

    private static void scheduleAllFrom(Context context, JSONArray alarmsJson) {
        for (int i = 0; i < alarmsJson.length(); i++) {
            try {
                JSONObject a = alarmsJson.getJSONObject(i);
                boolean enabled = a.optBoolean("enabled", true);
                if (!enabled) continue;

                String id = a.getString("id");
                String time = a.getString("time");
                String label = a.optString("label", "");
                String weekParity = a.optString("weekParity", "all");

                JSONArray daysJson = a.optJSONArray("repeatDays");
                int[] repeatDays = new int[0];
                if (daysJson != null) {
                    repeatDays = new int[daysJson.length()];
                    for (int j = 0; j < daysJson.length(); j++) {
                        repeatDays[j] = daysJson.getInt(j);
                    }
                }

                scheduleNext(context, id, time, repeatDays, weekParity, label);
            } catch (Exception e) {
                // on ignore cette entrée et on continue les autres
            }
        }
    }

    public static void scheduleNext(
        Context context,
        String id,
        String time,
        int[] repeatDays,
        String weekParity,
        String label
    ) {
        long next = computeNextOccurrence(time, repeatDays, weekParity, System.currentTimeMillis());
        if (next < 0) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("id", id);
        intent.putExtra("label", label);
        intent.putExtra("time", time);
        intent.putExtra("repeatDays", repeatDays);
        intent.putExtra("weekParity", weekParity);

        int requestCode = id.hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent showIntent = new Intent(context, AlarmActivity.class);
        showIntent.putExtra("id", id);
        showIntent.putExtra("label", label);
        showIntent.putExtra("time", time);
        showIntent.putExtra("repeatDays", repeatDays);
        showIntent.putExtra("weekParity", weekParity);
        PendingIntent showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(next, showPendingIntent);
        alarmManager.setAlarmClock(info, pendingIntent);
    }

    public static void cancel(Context context, String id) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        int requestCode = id.hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }

    public static long computeNextOccurrence(String time, int[] repeatDays, String weekParity, long fromMillis) {
        String[] parts = time.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        Calendar from = Calendar.getInstance();
        from.setTimeInMillis(fromMillis);

        if (repeatDays == null || repeatDays.length == 0) {
            Calendar candidate = (Calendar) from.clone();
            candidate.set(Calendar.HOUR_OF_DAY, hour);
            candidate.set(Calendar.MINUTE, minute);
            candidate.set(Calendar.SECOND, 0);
            candidate.set(Calendar.MILLISECOND, 0);
            if (candidate.getTimeInMillis() <= fromMillis) {
                candidate.add(Calendar.DAY_OF_MONTH, 1);
            }
            return candidate.getTimeInMillis();
        }

        for (int offset = 0; offset < 60; offset++) {
            Calendar candidate = (Calendar) from.clone();
            candidate.add(Calendar.DAY_OF_MONTH, offset);
            candidate.set(Calendar.HOUR_OF_DAY, hour);
            candidate.set(Calendar.MINUTE, minute);
            candidate.set(Calendar.SECOND, 0);
            candidate.set(Calendar.MILLISECOND, 0);

            if (candidate.getTimeInMillis() <= fromMillis) continue;

            int javaDayOfWeek = candidate.get(Calendar.DAY_OF_WEEK); // 1=dimanche...7=samedi
            int jsDay = javaDayOfWeek - 1; // 0=dimanche...6=samedi, comme Date.getDay() en JS

            boolean dayMatches = false;
            for (int d : repeatDays) {
                if (d == jsDay) {
                    dayMatches = true;
                    break;
                }
            }
            if (!dayMatches) continue;

            if (!isWeekParityMatch(candidate, weekParity)) continue;

            return candidate.getTimeInMillis();
        }

        return -1;
    }

    private static boolean isWeekParityMatch(Calendar date, String parity) {
        if (parity == null || (!parity.equals("even") && !parity.equals("odd"))) return true;
        int week = getISOWeek(date);
        if (parity.equals("even")) return week % 2 == 0;
        return week % 2 == 1;
    }

    private static int getISOWeek(Calendar date) {
        Calendar cal = (Calendar) date.clone();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.setMinimalDaysInFirstWeek(4);
        return cal.get(Calendar.WEEK_OF_YEAR);
    }
}
