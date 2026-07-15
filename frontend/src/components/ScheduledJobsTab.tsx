import { useEffect, useState } from 'react';
import { Clock, Plus, Trash2, PlayCircle, PauseCircle, RefreshCw, AlertTriangle } from 'lucide-react';
import {
  usePaths,
  useAlgorithms,
  useSchedules,
  useCreateSchedule,
  useDeleteSchedule,
  useSchedulerStatus,
  usePauseScheduler,
  useResumeScheduler,
} from '@/hooks/api';
import { useCronSchedulingEnabled } from '@/lib/settings';
import { defaultAlgorithm } from '@/lib/algorithm';
import { formatJavaDateTime } from '@/types/api';
import type { Frequency, ScheduleRequest } from '@/types/api';

// Quartz day-of-week is 1 (Sunday) to 7 (Saturday).
const DAY_NAMES = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

const inputClass =
  'w-full px-3 py-2 bg-card border border-foreground/20 text-foreground rounded-lg text-sm';

/**
 * Manage recurring scan schedules against the Java backend (`/api/schedules`). Users pick a
 * friendly preset (frequency plus time); a raw Quartz cron interface is offered only when the
 * advanced cron setting is enabled (Settings, Advanced Settings). The server derives the cron,
 * persists the schedule, and re-registers it with Quartz on restart.
 */
export default function ScheduledJobsTab() {
  const { data: paths } = usePaths();
  const { data: algorithms } = useAlgorithms();
  const { data: schedules, isLoading: schedulesLoading } = useSchedules();
  const { data: schedulerStatus, isLoading: schedulerLoading } = useSchedulerStatus();

  const createSchedule = useCreateSchedule();
  const deleteSchedule = useDeleteSchedule();
  const pauseScheduler = usePauseScheduler();
  const resumeScheduler = useResumeScheduler();
  const cronEnabled = useCronSchedulingEnabled();

  const [pathId, setPathId] = useState<number>(0);
  const [algorithm, setAlgorithm] = useState(defaultAlgorithm());
  const [useCron, setUseCron] = useState(false);
  const [cron, setCron] = useState('0 0 2 * * ?');
  const [frequency, setFrequency] = useState<Frequency>('DAILY');
  const [hour, setHour] = useState(2);
  const [minute, setMinute] = useState(0);
  const [dayOfWeek, setDayOfWeek] = useState(2); // Monday

  useEffect(() => {
    if (!pathId && paths && paths.length > 0) {
      setPathId(paths[0].id);
    }
  }, [paths, pathId]);

  useEffect(() => {
    if (algorithms && algorithms.length > 0 && !algorithms.some((a) => a.id === algorithm)) {
      setAlgorithm(algorithms[0].id);
    }
  }, [algorithms, algorithm]);

  const isPaused = schedulerStatus?.paused ?? false;
  const running = schedulerStatus?.running ?? false;

  const clamp = (value: number, min: number, max: number) =>
    Math.max(min, Math.min(max, Number.isNaN(value) ? min : value));

  const submit = () => {
    if (!pathId) {
      return;
    }
    const request: ScheduleRequest =
      useCron && cronEnabled
        ? { pathId, cron: cron.trim(), algorithm }
        : { pathId, frequency, minute, hour, dayOfWeek, algorithm };
    createSchedule.mutate(request);
  };

  const toggleScheduler = () => {
    if (isPaused) {
      resumeScheduler.mutate();
    } else {
      pauseScheduler.mutate();
    }
  };

  const createError = createSchedule.error as Error | null;
  const cronInvalid = useCron && cronEnabled && cron.trim() === '';

  return (
    <div className="space-y-6">
      {/* Create schedule */}
      <div className="bg-card border border-foreground/10 rounded-lg p-6">
        <h2 className="text-xl font-semibold text-foreground mb-2">Create Scheduled Job</h2>
        <p className="text-sm text-foreground/60 mb-4">
          Schedule an automatic, recurring fixity scan for a registered path.
        </p>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3 mb-3">
          <div>
            <label className="block text-xs text-foreground/60 mb-1">Path</label>
            <select value={pathId} onChange={(e) => setPathId(Number(e.target.value))} className={inputClass}>
              {(paths ?? []).length === 0 && <option value={0}>No paths registered</option>}
              {(paths ?? []).map((path) => (
                <option key={path.id} value={path.id}>{path.name}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs text-foreground/60 mb-1">Algorithm</label>
            <select value={algorithm} onChange={(e) => setAlgorithm(e.target.value)} className={inputClass}>
              {(algorithms ?? []).map((a) => (
                <option key={a.id} value={a.id}>{a.displayName}</option>
              ))}
            </select>
          </div>

          {cronEnabled && (
            <div>
              <label className="block text-xs text-foreground/60 mb-1">Schedule mode</label>
              <select
                value={useCron ? 'cron' : 'preset'}
                onChange={(e) => setUseCron(e.target.value === 'cron')}
                className={inputClass}
              >
                <option value="preset">Preset (frequency)</option>
                <option value="cron">Cron expression (advanced)</option>
              </select>
            </div>
          )}
        </div>

        {useCron && cronEnabled ? (
          <div className="mb-4">
            <label className="block text-xs text-foreground/60 mb-1">Quartz cron expression</label>
            <input
              type="text"
              value={cron}
              onChange={(e) => setCron(e.target.value)}
              placeholder="0 0 2 * * ?"
              className={`${inputClass} font-mono`}
            />
            <p className="text-xs text-foreground/50 mt-1">
              Six fields: seconds minutes hours day-of-month month day-of-week. Example{' '}
              <code className="font-mono">0 0 2 * * ?</code> runs at 02:00 every day.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3 mb-4">
            <div>
              <label className="block text-xs text-foreground/60 mb-1">Frequency</label>
              <select
                value={frequency}
                onChange={(e) => setFrequency(e.target.value as Frequency)}
                className={inputClass}
              >
                <option value="HOURLY">Hourly</option>
                <option value="DAILY">Daily</option>
                <option value="WEEKLY">Weekly</option>
              </select>
            </div>

            {frequency === 'WEEKLY' && (
              <div>
                <label className="block text-xs text-foreground/60 mb-1">Day</label>
                <select
                  value={dayOfWeek}
                  onChange={(e) => setDayOfWeek(Number(e.target.value))}
                  className={inputClass}
                >
                  {DAY_NAMES.map((name, i) => (
                    <option key={name} value={i + 1}>{name}</option>
                  ))}
                </select>
              </div>
            )}

            {frequency !== 'HOURLY' && (
              <div>
                <label className="block text-xs text-foreground/60 mb-1">Hour (0-23)</label>
                <input
                  type="number"
                  min={0}
                  max={23}
                  value={hour}
                  onChange={(e) => setHour(clamp(Number(e.target.value), 0, 23))}
                  className={inputClass}
                />
              </div>
            )}

            <div>
              <label className="block text-xs text-foreground/60 mb-1">Minute (0-59)</label>
              <input
                type="number"
                min={0}
                max={59}
                value={minute}
                onChange={(e) => setMinute(clamp(Number(e.target.value), 0, 59))}
                className={inputClass}
              />
            </div>
          </div>
        )}

        {createError && (
          <div className="mb-3 flex items-start gap-2 rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-2 text-sm text-red-600">
            <AlertTriangle className="w-4 h-4 mt-0.5 flex-none" />
            <span>{createError.message}</span>
          </div>
        )}

        <div className="flex justify-end">
          <button
            onClick={submit}
            disabled={createSchedule.isPending || !pathId || cronInvalid}
            className="inline-flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:opacity-90 transition-opacity disabled:opacity-50"
          >
            {createSchedule.isPending ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
            Create Scheduled Job
          </button>
        </div>
      </div>

      {/* Scheduler status */}
      <div className="bg-card border border-foreground/10 rounded-lg p-6">
        <div className="flex justify-between items-start gap-4">
          <div>
            <h2 className="text-xl font-semibold text-foreground mb-2">Scheduler Status</h2>
            <p className="text-sm text-foreground/60">
              The scheduler runs your saved schedules automatically. Pausing holds all of them until resumed.
            </p>
          </div>
          <button
            onClick={toggleScheduler}
            disabled={schedulerLoading || pauseScheduler.isPending || resumeScheduler.isPending}
            className={`flex items-center gap-2 px-6 py-3 rounded-lg font-medium text-white transition-colors disabled:opacity-50 ${
              isPaused ? 'bg-green-600 hover:bg-green-700' : 'bg-yellow-600 hover:bg-yellow-700'
            }`}
          >
            {isPaused ? <PlayCircle className="w-5 h-5" /> : <PauseCircle className="w-5 h-5" />}
            {isPaused ? 'Resume Scheduler' : 'Pause Scheduler'}
          </button>
        </div>

        <div className="mt-4 flex items-center gap-2">
          <div className={`w-3 h-3 rounded-full ${isPaused ? 'bg-yellow-500' : 'bg-green-500'} animate-pulse`} />
          <span className="text-sm font-medium text-foreground">
            {schedulerLoading ? 'Checking status...' : isPaused ? 'Scheduler Paused' : 'Scheduler Running'}
          </span>
        </div>

        <div className="mt-4 grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div className="bg-foreground/5 border border-foreground/10 rounded-lg p-3">
            <div className="text-xs text-foreground/60">Schedules</div>
            <div className="text-xl font-semibold text-foreground">{schedules?.length ?? 0}</div>
          </div>
          <div className="bg-foreground/5 border border-foreground/10 rounded-lg p-3">
            <div className="text-xs text-foreground/60">Scheduler status</div>
            <div className="text-xl font-semibold text-foreground">{schedulerStatus?.status ?? 'Unknown'}</div>
          </div>
          <div className="bg-foreground/5 border border-foreground/10 rounded-lg p-3">
            <div className="text-xs text-foreground/60">Running</div>
            <div className="text-xl font-semibold text-foreground">{running ? 'Yes' : 'No'}</div>
          </div>
        </div>
      </div>

      {/* Schedule list */}
      <div className="bg-card border border-foreground/10 rounded-lg overflow-hidden">
        <div className="px-6 py-4 border-b border-foreground/10">
          <h3 className="text-lg font-semibold text-foreground">Scheduled Jobs</h3>
        </div>

        {schedulesLoading ? (
          <div className="p-6 flex items-center gap-2 text-foreground/70">
            <RefreshCw className="w-4 h-4 animate-spin" /> Loading schedules...
          </div>
        ) : !schedules || schedules.length === 0 ? (
          <div className="p-10 text-center text-foreground/60 flex flex-col items-center gap-2">
            <Clock className="w-6 h-6 text-foreground/30" />
            No scheduled jobs yet. Create one above to run scans automatically.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-foreground/5 border-b border-foreground/10">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-foreground/70 uppercase tracking-wider">Path</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-foreground/70 uppercase tracking-wider">Schedule</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-foreground/70 uppercase tracking-wider">Algorithm</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-foreground/70 uppercase tracking-wider">Cron</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-foreground/70 uppercase tracking-wider">Created</th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-foreground/70 uppercase tracking-wider">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-foreground/10">
                {schedules.map((s) => (
                  <tr key={s.id} className="hover:bg-foreground/5 transition-colors">
                    <td className="px-6 py-4">
                      <div className="font-medium text-foreground">{s.pathName}</div>
                      <div className="text-xs text-foreground/50 font-mono">{s.pathRoot}</div>
                    </td>
                    <td className="px-6 py-4 text-sm text-foreground/80">{s.description}</td>
                    <td className="px-6 py-4 text-sm text-foreground/80">{s.algorithm}</td>
                    <td className="px-6 py-4 text-xs text-foreground/60 font-mono">{s.cron}</td>
                    <td className="px-6 py-4 text-sm text-foreground/80">{formatJavaDateTime(s.created)}</td>
                    <td className="px-6 py-4 text-right">
                      <button
                        onClick={() => deleteSchedule.mutate(s.id)}
                        disabled={deleteSchedule.isPending}
                        className="inline-flex items-center gap-1 px-2.5 py-1.5 text-xs rounded border border-red-500/30 text-red-600 bg-red-500/10 hover:bg-red-500/20 disabled:opacity-50"
                      >
                        <Trash2 className="w-3.5 h-3.5" /> Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
