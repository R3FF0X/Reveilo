import { useState, useEffect } from "react";
import type { Alarm } from "./types";
import { getUpcomingOccurrences } from "./types";
import ClockLogo from "./ClockLogo";
import AlarmCard from "./AlarmCard";
import AlarmForm from "./AlarmForm";
import UpcomingAlarmsList from "./UpcomingAlarmsList";
import BottomSheetModal from "./BottomSheetModal";
import ConfirmDeleteModal from "./ConfirmDeleteModal";
import {
  syncNativeAlarms,
  ensureNotificationPermission,
  ensureExactAlarmPermission,
} from "./nativeAlarms";

const STORAGE_KEY = "reveilo-alarms";
const UPCOMING_WINDOW_DAYS = 21;
const MAX_ALARMS = 20;

function loadAlarms(): Alarm[] {
  const raw = localStorage.getItem(STORAGE_KEY);
  return raw ? JSON.parse(raw) : [];
}

function App() {
  const [alarms, setAlarms] = useState<Alarm[]>(loadAlarms);
  const [isAdding, setIsAdding] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(alarms));
  }, [alarms]);

  useEffect(() => {
    const id = window.setInterval(() => setNow(new Date()), 30000);
    return () => window.clearInterval(id);
  }, []);

  useEffect(() => {
    syncNativeAlarms(alarms);
  }, [alarms]);

  useEffect(() => {
    ensureNotificationPermission();
    ensureExactAlarmPermission();
  }, []);

  function addAlarm(newAlarm: Omit<Alarm, "id">) {
    if (alarms.length >= MAX_ALARMS) return;
    const alarm: Alarm = { ...newAlarm, id: crypto.randomUUID() };
    setAlarms((prev) => [...prev, alarm]);
    setIsAdding(false);
  }

  function updateAlarm(id: string, updated: Omit<Alarm, "id">) {
    setAlarms((prev) =>
      prev.map((alarm) => (alarm.id === id ? { ...updated, id } : alarm)),
    );
    setEditingId(null);
  }

  function deleteAlarm(id: string) {
    setAlarms((prev) => prev.filter((alarm) => alarm.id !== id));
  }

  function toggleEnabled(id: string) {
    setAlarms((prev) =>
      prev.map((alarm) =>
        alarm.id === id ? { ...alarm, enabled: !alarm.enabled } : alarm,
      ),
    );
  }

  function closeForm() {
    setIsAdding(false);
    setEditingId(null);
  }

  const editingAlarm = alarms.find((alarm) => alarm.id === editingId);
  const pendingDeleteAlarm = alarms.find(
    (alarm) => alarm.id === pendingDeleteId,
  );
  const isFormOpen = isAdding || editingId !== null;
  const upcoming = getUpcomingOccurrences(alarms, now, UPCOMING_WINDOW_DAYS);
  const canAddMore = alarms.length < MAX_ALARMS;

  return (
    <div className="h-screen bg-[#1b1d21] flex flex-col overflow-hidden">
      <header className="h-14 flex-shrink-0 flex items-center justify-center gap-2 bg-[#1b1d21]/95 backdrop-blur border-b border-neutral-800">
        <ClockLogo className="w-8 h-8" />
        <span className="text-white font-semibold">Réveilo</span>
      </header>

      {isFormOpen && (
        <BottomSheetModal onClose={closeForm}>
          {(close) => (
            <>
              <h2 className="text-white text-base font-semibold mb-3">
                {editingAlarm ? "Modifier l'alarme" : "Ajouter une alarme"}
              </h2>
              <AlarmForm
                initialAlarm={editingAlarm}
                onSubmit={(data) => {
                  if (editingId) {
                    updateAlarm(editingId, data);
                  } else {
                    addAlarm(data);
                  }
                  close();
                }}
                onCancel={close}
              />
            </>
          )}
        </BottomSheetModal>
      )}

      {pendingDeleteAlarm && (
        <ConfirmDeleteModal
          label={pendingDeleteAlarm.label}
          onConfirm={() => deleteAlarm(pendingDeleteAlarm.id)}
          onCancel={() => setPendingDeleteId(null)}
        />
      )}

      <div className="flex-1 flex min-h-0">
        <aside className="thin-scrollbar w-[76px] flex-shrink-0 overflow-y-auto overflow-x-hidden border-r border-neutral-800 px-1.5 py-4">
          <p className="text-neutral-300 text-xs font-semibold mb-3 text-center uppercase tracking-wide">
            À venir
          </p>
          <UpcomingAlarmsList occurrences={upcoming} />
        </aside>

        <main className="flex-1 overflow-y-auto p-4">
          <div className="grid grid-cols-2 landscape:grid-cols-4 gap-3">
            {alarms.map((alarm) => (
              <AlarmCard
                key={alarm.id}
                alarm={alarm}
                onEdit={setEditingId}
                onDelete={setPendingDeleteId}
                onToggleEnabled={toggleEnabled}
              />
            ))}
          </div>
        </main>
      </div>

      <button
        onClick={() => canAddMore && setIsAdding(true)}
        disabled={!canAddMore}
        aria-label="Ajouter une alarme"
        className={`fixed bottom-6 right-6 z-40 w-14 h-14 rounded-full text-white text-3xl leading-none shadow-lg flex items-center justify-center transition-transform duration-100 ${
          canAddMore
            ? "bg-orange-600 hover:bg-orange-500 active:scale-90"
            : "bg-neutral-700 opacity-60 cursor-not-allowed"
        }`}
      >
        +
      </button>
    </div>
  );
}

export default App;
