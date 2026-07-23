export type WeekParity = "all" | "even" | "odd";

export type Alarm = {
  id: string;
  time: string; // "HH:MM" (24h)
  label: string;
  enabled: boolean;
  repeatDays: number[]; // 0 = dimanche ... 6 = samedi ; vide = ponctuelle
  weekParity: WeekParity;
};

export const DAY_LABELS = ["D", "L", "M", "M", "J", "V", "S"];
export const DAY_LABELS_2 = ["Di", "Lu", "Ma", "Me", "Je", "Ve", "Sa"];
export const DAY_NAMES = [
  "Dimanche",
  "Lundi",
  "Mardi",
  "Mercredi",
  "Jeudi",
  "Vendredi",
  "Samedi",
];
export const DAY_ABBR = ["Dim", "Lun", "Mar", "Mer", "Jeu", "Ven", "Sam"];

// Ordre d'affichage des jours, semaine commençant le lundi (les index restent
// ceux de Date.getDay(), 0 = dimanche, uniquement l'ORDRE d'affichage change).
export const WEEK_ORDER = [1, 2, 3, 4, 5, 6, 0];

export function sortDaysWeekOrder(days: number[]): number[] {
  return [...days].sort(
    (a, b) => WEEK_ORDER.indexOf(a) - WEEK_ORDER.indexOf(b),
  );
}

export function formatTime(time: string): string {
  const [h, m] = time.split(":");
  return `${h}H${m}`;
}

export function timeToMinutes(time: string): number {
  const [h, m] = time.split(":").map(Number);
  return h * 60 + m;
}

// Numéro de semaine ISO 8601 (lundi = premier jour, semaine 1 = celle
// contenant le premier jeudi de l'année).
export function getISOWeek(date: Date): number {
  const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
  const dayNum = d.getUTCDay() || 7;
  d.setUTCDate(d.getUTCDate() + 4 - dayNum);
  const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
  return Math.ceil(((d.getTime() - yearStart.getTime()) / 86400000 + 1) / 7);
}

// Les anciennes alarmes stockées avant l'ajout de ce champ n'ont pas de
// weekParity : on traite toute valeur autre que "even"/"odd" comme "all".
export function isWeekParityMatch(
  date: Date,
  parity: WeekParity | undefined,
): boolean {
  if (parity !== "even" && parity !== "odd") return true;
  const week = getISOWeek(date);
  return parity === "even" ? week % 2 === 0 : week % 2 === 1;
}

export type Occurrence = {
  alarm: Alarm;
  date: Date;
};

// Retourne toutes les occurrences (toutes alarmes confondues) dans la fenêtre
// [from, from + windowDays], triées chronologiquement — une alarme qui se
// répète apparaît une fois par jour concerné (en tenant compte des semaines
// paires/impaires si applicable), pas une seule fois au total.
export function getUpcomingOccurrences(
  alarms: Alarm[],
  from: Date,
  windowDays: number,
): Occurrence[] {
  const windowEnd = new Date(from);
  windowEnd.setDate(windowEnd.getDate() + windowDays);

  const occurrences: Occurrence[] = [];

  for (const alarm of alarms) {
    if (!alarm.enabled) continue;
    const [h, m] = alarm.time.split(":").map(Number);

    if (alarm.repeatDays.length === 0) {
      const candidate = new Date(from);
      candidate.setHours(h, m, 0, 0);
      if (candidate <= from) candidate.setDate(candidate.getDate() + 1);
      if (candidate <= windowEnd) occurrences.push({ alarm, date: candidate });
      continue;
    }

    for (let offset = 0; offset <= windowDays; offset++) {
      const candidate = new Date(from);
      candidate.setDate(candidate.getDate() + offset);
      candidate.setHours(h, m, 0, 0);
      if (candidate <= from) continue;
      if (candidate > windowEnd) break;
      if (
        alarm.repeatDays.includes(candidate.getDay()) &&
        isWeekParityMatch(candidate, alarm.weekParity)
      ) {
        occurrences.push({ alarm, date: candidate });
      }
    }
  }

  return occurrences.sort((a, b) => a.date.getTime() - b.date.getTime());
}

// Prochaine occurrence unique d'une alarme (pour l'affichage du compte à
// rebours sur sa cellule).
export function getNextOccurrence(alarm: Alarm, from: Date): Date | null {
  if (!alarm.enabled) return null;
  const [h, m] = alarm.time.split(":").map(Number);

  if (alarm.repeatDays.length === 0) {
    const candidate = new Date(from);
    candidate.setHours(h, m, 0, 0);
    if (candidate <= from) candidate.setDate(candidate.getDate() + 1);
    return candidate;
  }

  for (let offset = 0; offset < 60; offset++) {
    const candidate = new Date(from);
    candidate.setDate(candidate.getDate() + offset);
    candidate.setHours(h, m, 0, 0);
    if (candidate <= from) continue;
    if (
      alarm.repeatDays.includes(candidate.getDay()) &&
      isWeekParityMatch(candidate, alarm.weekParity)
    ) {
      return candidate;
    }
  }
  return null;
}

// Formate un délai jusqu'à une date cible en "H:MM".
export function formatCountdown(target: Date, from: Date): string {
  const diffMs = target.getTime() - from.getTime();
  const totalMinutes = Math.max(0, Math.round(diffMs / 60000));
  const h = Math.floor(totalMinutes / 60);
  const m = totalMinutes % 60;
  return `${h}:${m.toString().padStart(2, "0")}`;
}

export function formatDayHeader(date: Date): string {
  const dayAbbr = DAY_ABBR[date.getDay()];
  const day = date.getDate().toString().padStart(2, "0");
  const month = (date.getMonth() + 1).toString().padStart(2, "0");
  return `${dayAbbr} ${day}/${month}`;
}
