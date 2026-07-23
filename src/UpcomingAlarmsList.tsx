import type { Occurrence } from "./types";
import { formatDayHeader } from "./types";

type Props = {
  occurrences: Occurrence[];
};

function pad(n: number): string {
  return n.toString().padStart(2, "0");
}

function UpcomingAlarmsList({ occurrences }: Props) {
  if (occurrences.length === 0) {
    return null;
  }

  const groups: { header: string; items: Occurrence[] }[] = [];
  for (const occ of occurrences) {
    const header = formatDayHeader(occ.date);
    const lastGroup = groups[groups.length - 1];
    if (lastGroup && lastGroup.header === header) {
      lastGroup.items.push(occ);
    } else {
      groups.push({ header, items: [occ] });
    }
  }

  return (
    <div className="flex flex-col">
      {groups.map((group, i) => (
        <div key={`${group.header}-${i}`}>
          {i > 0 && <div className="border-t border-neutral-800 my-2.5" />}
          <p className="text-neutral-300 text-xs font-semibold whitespace-nowrap text-center">
            {group.header}
          </p>
          <div className="flex flex-col items-center gap-1 mt-1">
            {group.items.map((occ) => (
              <span key={occ.alarm.id} className="text-white text-xs">
                {pad(occ.date.getHours())}:{pad(occ.date.getMinutes())}
              </span>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

export default UpcomingAlarmsList;
