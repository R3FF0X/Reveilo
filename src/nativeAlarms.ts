import { registerPlugin } from "@capacitor/core";
import type { Alarm } from "./types";

interface AlarmRingerPlugin {
  syncAlarms(options: { alarms: Alarm[] }): Promise<void>;
  ensureNotificationPermission(): Promise<void>;
  canScheduleExactAlarms(): Promise<{ value: boolean }>;
  requestExactAlarmPermission(): Promise<void>;
}

const AlarmRinger = registerPlugin<AlarmRingerPlugin>("AlarmRinger");

// Envoie la liste complète des alarmes au natif : il annule tout, sauvegarde,
// puis reprogramme les occurrences suivantes (utile aussi après un reboot).
export async function syncNativeAlarms(alarms: Alarm[]): Promise<void> {
  try {
    await AlarmRinger.syncAlarms({ alarms });
  } catch {
    // pas sur une plateforme native (ex: navigateur en dev) — on ignore
  }
}

// Demande la permission de notification (obligatoire sur Android 13+ pour
// pouvoir afficher l'écran de sonnerie via un "full-screen intent").
export async function ensureNotificationPermission(): Promise<void> {
  try {
    await AlarmRinger.ensureNotificationPermission();
  } catch {
    // ignore hors plateforme native
  }
}

// Demande la permission "Alarmes et rappels" (obligatoire sur Android 12+
// pour programmer des alarmes exactes) si elle n'est pas déjà accordée.
export async function ensureExactAlarmPermission(): Promise<void> {
  try {
    const { value } = await AlarmRinger.canScheduleExactAlarms();
    if (!value) {
      await AlarmRinger.requestExactAlarmPermission();
    }
  } catch {
    // ignore hors plateforme native
  }
}
