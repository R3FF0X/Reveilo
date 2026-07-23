import type { MouseEvent } from "react";
import type { Alarm } from "./types";
import { DAY_LABELS_2, formatTime, sortDaysWeekOrder } from "./types";

type Props = {
  alarm: Alarm;
  onEdit: (id: string) => void;
  onDelete: (id: string) => void;
  onToggleEnabled: (id: string) => void;
};

function ToggleSwitch({
  checked,
  onClick,
}: {
  checked: boolean;
  onClick: (e: MouseEvent) => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`relative w-10 h-6 rounded-full flex-shrink-0 transition-colors duration-200 ${
        checked ? "bg-orange-600" : "bg-neutral-700"
      }`}
    >
      <span
        className={`absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-white transition-transform duration-200 ${
          checked ? "translate-x-4" : "translate-x-0"
        }`}
      />
    </button>
  );
}

function AlarmCard({ alarm, onEdit, onDelete, onToggleEnabled }: Props) {
  const daysSummary =
    alarm.repeatDays.length === 0
      ? "Ponctuelle"
      : alarm.repeatDays.length === 7
        ? "Tous les jours"
        : sortDaysWeekOrder(alarm.repeatDays)
            .map((d) => DAY_LABELS_2[d])
            .join(" ");

  return (
    <div
      onClick={() => onEdit(alarm.id)}
      className={`relative flex flex-col justify-between rounded-2xl p-3 min-h-[140px] shadow-lg cursor-pointer active:scale-[0.98] transition-transform duration-100 ${
        alarm.enabled ? "bg-neutral-800" : "bg-neutral-900/60"
      }`}
    >
      <div className="flex items-center justify-between gap-2">
        <span
          className={`text-[10px] leading-none ${alarm.enabled ? "text-neutral-400" : "text-neutral-600"}`}
        >
          {daysSummary}
        </span>
        <div className="flex items-center gap-1.5 flex-shrink-0">
          {alarm.weekParity !== "all" && (
            <span className="text-[10px] font-bold leading-none text-orange-500">
              {alarm.weekParity === "even" ? "P" : "I"}
            </span>
          )}
          <button
            onClick={(e) => {
              e.stopPropagation();
              onDelete(alarm.id);
            }}
            className="text-neutral-500 hover:text-white text-sm leading-none active:scale-90 transition-transform duration-100"
          >
            ✕
          </button>
        </div>
      </div>

      <div className="flex flex-col items-center text-center px-1">
        <p
          className={`text-2xl font-semibold ${alarm.enabled ? "text-white" : "text-neutral-500"}`}
        >
          {formatTime(alarm.time)}
        </p>
      </div>

      <div className="flex items-center justify-between mt-2 gap-2">
        <ToggleSwitch
          checked={alarm.enabled}
          onClick={(e) => {
            e.stopPropagation();
            onToggleEnabled(alarm.id);
          }}
        />
        <span
          className={`text-[10px] truncate ${alarm.enabled ? "text-neutral-400" : "text-neutral-600"}`}
        >
          {alarm.label}
        </span>
      </div>
    </div>
  );
}

export default AlarmCard;
