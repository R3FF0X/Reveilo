import { useState } from "react";
import type { FormEvent } from "react";
import type { Alarm, WeekParity } from "./types";
import { DAY_LABELS, WEEK_ORDER } from "./types";

type Props = {
  initialAlarm?: Alarm;
  onSubmit: (alarm: Omit<Alarm, "id">) => void;
  onCancel: () => void;
};

const fieldClass =
  "w-full bg-neutral-800 border-none rounded-xl px-4 py-3 text-white placeholder:text-neutral-500 focus:outline-none focus:ring-2 focus:ring-white/30";

function AlarmForm({ initialAlarm, onSubmit, onCancel }: Props) {
  const [label, setLabel] = useState(initialAlarm?.label ?? "");
  const [time, setTime] = useState(initialAlarm?.time ?? "07:00");
  const [repeatDays, setRepeatDays] = useState<number[]>(
    initialAlarm?.repeatDays ?? [],
  );
  const [weekParity, setWeekParity] = useState<WeekParity>(
    initialAlarm?.weekParity ?? "all",
  );
  const [enabled, setEnabled] = useState(initialAlarm?.enabled ?? true);

  function toggleDay(day: number) {
    setRepeatDays((prev) =>
      prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day],
    );
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!label.trim()) return;
    onSubmit({ label, time, repeatDays, weekParity, enabled });
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-3 w-full">
      <input
        type="time"
        value={time}
        onChange={(e) => setTime(e.target.value)}
        className={`${fieldClass} text-center text-2xl`}
      />

      <input
        type="text"
        placeholder="Nom de l'alarme"
        value={label}
        onChange={(e) => setLabel(e.target.value)}
        autoFocus
        className={fieldClass}
      />

      <div className="bg-neutral-800 rounded-xl px-4 py-3">
        <span className="text-neutral-300 text-sm">Répétition</span>
        <div className="flex justify-between gap-1 mt-2">
          {WEEK_ORDER.map((day) => (
            <button
              key={day}
              type="button"
              onClick={() => toggleDay(day)}
              className={`w-8 h-8 rounded-full text-xs font-semibold flex items-center justify-center transition-colors active:scale-90 ${
                repeatDays.includes(day)
                  ? "bg-orange-600 text-white"
                  : "bg-neutral-900 text-neutral-500"
              }`}
            >
              {DAY_LABELS[day]}
            </button>
          ))}
        </div>
        {repeatDays.length === 0 && (
          <p className="text-neutral-500 text-xs mt-2">
            Aucun jour sélectionné : alarme ponctuelle (prochaine occurrence).
          </p>
        )}
      </div>

      {repeatDays.length > 0 && (
        <div className="bg-neutral-800 rounded-xl px-4 py-3">
          <label className="text-neutral-300 text-sm block mb-2">
            Fréquence
          </label>
          <select
            value={weekParity}
            onChange={(e) => setWeekParity(e.target.value as WeekParity)}
            className="w-full bg-neutral-900 border-none rounded-lg px-3 py-2 text-white focus:outline-none focus:ring-2 focus:ring-white/30"
          >
            <option value="all">Toutes les semaines</option>
            <option value="even">Semaines paires</option>
            <option value="odd">Semaines impaires</option>
          </select>
        </div>
      )}

      <label className="flex items-center justify-between bg-neutral-800 rounded-xl px-4 py-3 cursor-pointer">
        <span className="text-neutral-300 text-sm">Activée</span>
        <input
          type="checkbox"
          checked={enabled}
          onChange={(e) => setEnabled(e.target.checked)}
          className="w-4 h-4 accent-orange-600"
        />
      </label>

      <div className="flex gap-2 pt-1">
        <button
          type="button"
          onClick={onCancel}
          className="flex-1 py-3 rounded-xl border border-neutral-700 text-neutral-300 font-medium active:scale-95 transition-transform duration-100"
        >
          Annuler
        </button>
        <button
          type="submit"
          className="flex-1 py-3 rounded-xl bg-white text-black font-semibold active:scale-95 transition-transform duration-100"
        >
          {initialAlarm ? "Enregistrer" : "Ajouter"}
        </button>
      </div>
    </form>
  );
}

export default AlarmForm;
