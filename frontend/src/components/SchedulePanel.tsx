import { useState } from 'react';
import { Clock, Trash2, Plus } from 'lucide-react';
import { useSchedules, useCreateSchedule, useDeleteSchedule } from '@/hooks/api';
import type { Frequency, ScanSchedule } from '@/types/api';

// Quartz day-of-week is 1 (Sunday) to 7 (Saturday).
const DAY_NAMES = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

function pad(n: number): string {
  return n.toString().padStart(2, '0');
}

/** A human sentence for a schedule, so users never see a cron expression. */
function describe(s: ScanSchedule): string {
  switch (s.frequency) {
    case 'HOURLY':
      return `Every hour at ${pad(s.minute)} minutes past`;
    case 'DAILY':
      return `Every day at ${pad(s.hour)}:${pad(s.minute)}`;
    case 'WEEKLY':
      return `Every ${DAY_NAMES[s.dayOfWeek - 1] ?? 'week'} at ${pad(s.hour)}:${pad(s.minute)}`;
    default:
      return s.cron;
  }
}

/**
 * Manage recurring scan schedules for one registered path: list what exists, add a new one from
 * a friendly preset, and remove one. The cron expression is built on the server.
 */
export function SchedulePanel({ pathId, algorithm }: { pathId: number; algorithm: string }) {
  const { data: allSchedules, isLoading } = useSchedules();
  const createSchedule = useCreateSchedule();
  const deleteSchedule = useDeleteSchedule();

  const schedules = (allSchedules ?? []).filter((s) => s.pathId === pathId);

  const [frequency, setFrequency] = useState<Frequency>('DAILY');
  const [hour, setHour] = useState(2);
  const [minute, setMinute] = useState(0);
  const [dayOfWeek, setDayOfWeek] = useState(2); // Monday

  const submit = () => {
    createSchedule.mutate({ pathId, frequency, hour, minute, dayOfWeek, algorithm });
  };

  return (
    <div className="mb-6">
      <h2 className="text-lg font-semibold mb-2 flex items-center gap-2">
        <Clock className="w-5 h-5" /> Scheduled scans
      </h2>

      <div className="bg-card border border-foreground/10 rounded-lg p-4">
        {/* Existing schedules */}
        {isLoading ? (
          <p className="text-foreground/60 text-sm">Loading schedules…</p>
        ) : schedules.length === 0 ? (
          <p className="text-foreground/60 text-sm mb-4">
            No automatic scans set up for this path yet.
          </p>
        ) : (
          <ul className="mb-4 divide-y divide-foreground/10">
            {schedules.map((s) => (
              <li key={s.id} className="flex items-center justify-between py-2">
                <span className="text-sm">
                  {describe(s)} <span className="text-foreground/50">· {s.algorithm}</span>
                </span>
                <button
                  onClick={() => deleteSchedule.mutate(s.id)}
                  disabled={deleteSchedule.isPending}
                  className="text-foreground/60 hover:text-red-500 p-1"
                  title="Remove schedule"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </li>
            ))}
          </ul>
        )}

        {/* Create form */}
        <div className="flex flex-wrap items-end gap-3 border-t border-foreground/10 pt-4">
          <label className="text-sm">
            <span className="block text-foreground/60 mb-1">Frequency</span>
            <select
              value={frequency}
              onChange={(e) => setFrequency(e.target.value as Frequency)}
              className="bg-background border border-foreground/20 rounded px-2 py-1"
            >
              <option value="HOURLY">Hourly</option>
              <option value="DAILY">Daily</option>
              <option value="WEEKLY">Weekly</option>
            </select>
          </label>

          {frequency === 'WEEKLY' && (
            <label className="text-sm">
              <span className="block text-foreground/60 mb-1">Day</span>
              <select
                value={dayOfWeek}
                onChange={(e) => setDayOfWeek(Number(e.target.value))}
                className="bg-background border border-foreground/20 rounded px-2 py-1"
              >
                {DAY_NAMES.map((name, i) => (
                  <option key={name} value={i + 1}>{name}</option>
                ))}
              </select>
            </label>
          )}

          {frequency !== 'HOURLY' && (
            <label className="text-sm">
              <span className="block text-foreground/60 mb-1">Hour</span>
              <input
                type="number"
                min={0}
                max={23}
                value={hour}
                onChange={(e) => setHour(Math.max(0, Math.min(23, Number(e.target.value))))}
                className="bg-background border border-foreground/20 rounded px-2 py-1 w-16"
              />
            </label>
          )}

          <label className="text-sm">
            <span className="block text-foreground/60 mb-1">Minute</span>
            <input
              type="number"
              min={0}
              max={59}
              value={minute}
              onChange={(e) => setMinute(Math.max(0, Math.min(59, Number(e.target.value))))}
              className="bg-background border border-foreground/20 rounded px-2 py-1 w-16"
            />
          </label>

          <button
            onClick={submit}
            disabled={createSchedule.isPending}
            className="bg-primary text-primary-foreground rounded px-3 py-1.5 text-sm flex items-center gap-1 disabled:opacity-50"
          >
            <Plus className="w-4 h-4" /> Add schedule
          </button>
        </div>
      </div>
    </div>
  );
}
