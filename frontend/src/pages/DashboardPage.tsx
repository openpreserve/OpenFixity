import { useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { formatDistanceToNow } from 'date-fns';
import {
  useCollections,
  usePaths,
  useScans,
  useSchedulerStatus,
  useJobs,
  usePauseScheduler,
  useResumeScheduler,
  queryKeys,
} from '@/hooks/api';
import { javaDateTimeToDate } from '@/types/api';
import { Activity, CheckCircle2, Clock3, FolderTree, PauseCircle, PlayCircle, RefreshCw, ShieldAlert, ShieldCheck, XCircle } from 'lucide-react';

export default function DashboardPage() {
  const queryClient = useQueryClient();
  const { data: collections, isLoading: collectionsLoading } = useCollections();
  const { data: paths, isLoading: pathsLoading } = usePaths();
  const { data: scans, isLoading: scansLoading } = useScans();
  const { data: schedulerStatus, isLoading: schedulerLoading } = useSchedulerStatus();
  const { data: jobs, isLoading: jobsLoading } = useJobs();
  const pauseScheduler = usePauseScheduler();
  const resumeScheduler = useResumeScheduler();
  const isPaused = schedulerStatus?.paused ?? false;

  useEffect(() => {
    document.title = 'Dashboard | OpenFixity';
  }, []);

  useEffect(() => {
    let eventSource: EventSource | null = null;
    let pollingInterval: ReturnType<typeof setInterval> | null = null;

    const invalidateDashboard = () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.scans });
      queryClient.invalidateQueries({ queryKey: queryKeys.paths });
      queryClient.invalidateQueries({ queryKey: queryKeys.jobs });
      queryClient.invalidateQueries({ queryKey: queryKeys.scheduler });
      queryClient.invalidateQueries({ queryKey: queryKeys.collections });
    };

    // Try to connect to event stream
    try {
      eventSource = new EventSource('/api/events');

      eventSource.addEventListener('scan.started', invalidateDashboard);
      eventSource.addEventListener('scan.progress', invalidateDashboard);
      eventSource.addEventListener('scan.completed', invalidateDashboard);
      eventSource.addEventListener('scheduler.updated', invalidateDashboard);
      eventSource.addEventListener('scheduler.job.changed', invalidateDashboard);

      // Handle connection errors (Java backend doesn't have /api/events)
      eventSource.onerror = () => {
        eventSource?.close();
        eventSource = null;

        // Fallback to polling every 5 seconds
        if (!pollingInterval) {
          pollingInterval = setInterval(invalidateDashboard, 5000);
        }
      };
    } catch (error) {
      // Fallback to polling if EventSource not available
      pollingInterval = setInterval(invalidateDashboard, 5000);
    }

    return () => {
      if (eventSource) {
        eventSource.close();
      }
      if (pollingInterval) {
        clearInterval(pollingInterval);
      }
    };
  }, [queryClient]);

  const allScans = scans || [];
  const sortedScans = useMemo(
    () => [...allScans].sort((a, b) => {
      const aTime = javaDateTimeToDate(a.started)?.getTime() || 0;
      const bTime = javaDateTimeToDate(b.started)?.getTime() || 0;
      return bTime - aTime;
    }),
    [allScans]
  );

  const recentScans = sortedScans.slice(0, 8);

  const stats = useMemo(() => {
    const totalScans = allScans.length;
    const runningScans = allScans.filter(scan => scan.status === 'STARTED').length;
    const completedScans = allScans.filter(scan => scan.status === 'COMPLETED').length;
    const failedScans = allScans.filter(scan => scan.status === 'FAILED').length;

    const totalFilesScanned = allScans.reduce((sum, scan) => sum + (scan.summary?.totalFiles || 0), 0);
    const damaged = allScans.reduce((sum, scan) => sum + (scan.damagedCount || 0), 0);
    const denied = allScans.reduce((sum, scan) => sum + (scan.deniedCount || 0), 0);
    const missing = allScans.reduce((sum, scan) => sum + (scan.notFoundCount || 0), 0);

    return {
      totalScans,
      runningScans,
      completedScans,
      failedScans,
      totalFilesScanned,
      damaged,
      denied,
      missing,
      totalIssues: damaged + denied + missing,
    };
  }, [allScans]);

  const topRiskPaths = useMemo(() => {
    const riskMap = new Map<number, { name: string; issues: number }>();

    for (const scan of allScans) {
      const pathId = scan.collectionPath?.id;
      if (!pathId) continue;

      const issues = (scan.damagedCount || 0) + (scan.deniedCount || 0) + (scan.notFoundCount || 0);
      if (issues === 0) continue;

      const current = riskMap.get(pathId);
      if (current) {
        current.issues += issues;
      } else {
        riskMap.set(pathId, {
          name: scan.collectionPath?.name || scan.collectionPath?.root || `Path ${pathId}`,
          issues,
        });
      }
    }

    return Array.from(riskMap.values())
      .sort((a, b) => b.issues - a.issues)
      .slice(0, 5);
  }, [allScans]);

  const isLoading = collectionsLoading || pathsLoading || scansLoading || schedulerLoading || jobsLoading;

  const toggleScheduler = () => {
    if (isPaused) {
      resumeScheduler.mutate();
    } else {
      pauseScheduler.mutate();
    }
  };

  const statusBadge = (status: string) => {
    if (status === 'STARTED') {
      return (
        <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium text-blue-600 bg-blue-500/10 border border-blue-500/20">
          <RefreshCw className="w-3 h-3 animate-spin" />
          Running
        </span>
      );
    }
    if (status === 'COMPLETED') {
      return (
        <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium text-green-600 bg-green-500/10 border border-green-500/20">
          <CheckCircle2 className="w-3 h-3" />
          Completed
        </span>
      );
    }
    if (status === 'FAILED') {
      return (
        <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium text-red-600 bg-red-500/10 border border-red-500/20">
          <XCircle className="w-3 h-3" />
          Failed
        </span>
      );
    }
    return (
      <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium text-foreground/60 bg-foreground/5 border border-foreground/10">
        <Clock3 className="w-3 h-3" />
        Initialised
      </span>
    );
  };

  return (
    <div className="text-foreground">
      <div className="mb-6">
        <h2 className="text-3xl font-bold mb-2">Dashboard</h2>
        <p className="text-foreground/60">Operational overview, realtime scan activity, and system health.</p>
      </div>

      {isLoading ? (
        <div className="bg-card border border-foreground/10 rounded-lg p-6 flex items-center gap-2">
          <RefreshCw className="w-5 h-5 animate-spin" />
          <span>Loading dashboard...</span>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
            <div className="bg-card border border-foreground/10 rounded-lg p-4">
              <div className="text-sm text-foreground/60 mb-1">Collections</div>
              <div className="text-2xl font-bold">{collections?.length || 0}</div>
              <div className="text-xs text-foreground/50 mt-1">{paths?.length || 0} registered paths</div>
            </div>
            <div className="bg-card border border-foreground/10 rounded-lg p-4">
              <div className="text-sm text-foreground/60 mb-1">Scans</div>
              <div className="text-2xl font-bold">{stats.totalScans}</div>
              <div className="text-xs text-foreground/50 mt-1">{stats.runningScans} running • {stats.completedScans} completed • {stats.failedScans} failed</div>
            </div>
            <div className="bg-card border border-foreground/10 rounded-lg p-4">
              <div className="text-sm text-foreground/60 mb-1">Files Verified</div>
              <div className="text-2xl font-bold">{stats.totalFilesScanned.toLocaleString()}</div>
              <div className="text-xs text-foreground/50 mt-1">Across all completed and active scans</div>
            </div>
            <div className="bg-card border border-foreground/10 rounded-lg p-4">
              <div className="text-sm text-foreground/60 mb-1">Issues</div>
              <div className={`text-2xl font-bold ${stats.totalIssues > 0 ? 'text-yellow-600' : 'text-green-600'}`}>
                {stats.totalIssues}
              </div>
              <div className="text-xs text-foreground/50 mt-1">{stats.damaged} damaged • {stats.denied} denied • {stats.missing} missing</div>
            </div>
          </div>

          <div className="grid grid-cols-1 xl:grid-cols-3 gap-6 mb-6">
            <div className="xl:col-span-2 bg-card border border-foreground/10 rounded-lg p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold flex items-center gap-2">
                  <Activity className="w-5 h-5" />
                  Recent Activity
                </h3>
                <Link to="/scans" className="text-sm text-accent hover:underline">View all scans</Link>
              </div>

              {recentScans.length === 0 ? (
                <p className="text-sm text-foreground/60">No scans yet. Start a scan from Collections or Paths.</p>
              ) : (
                <div className="space-y-3">
                  {recentScans.map(scan => {
                    const started = javaDateTimeToDate(scan.started);
                    return (
                      <Link
                        key={scan.id}
                        to={`/scans/${scan.id}`}
                        className="flex items-center justify-between gap-3 p-3 bg-background border border-foreground/10 rounded-lg hover:border-accent/50 transition-colors"
                      >
                        <div className="min-w-0">
                          <div className="text-sm font-medium truncate">{scan.collectionPath?.name || scan.collectionPath?.root || 'Unknown path'}</div>
                          <div className="text-xs text-foreground/60">
                            {started ? formatDistanceToNow(started, { addSuffix: true }) : 'Unknown time'} • {scan.summary?.totalFiles || 0} files
                          </div>
                        </div>
                        <div className="flex items-center gap-2">
                          {statusBadge(scan.status)}
                        </div>
                      </Link>
                    );
                  })}
                </div>
              )}
            </div>

            <div className="bg-card border border-foreground/10 rounded-lg p-6">
              <h3 className="text-lg font-semibold flex items-center gap-2 mb-4">
                <ShieldAlert className="w-5 h-5" />
                Health & Risk
              </h3>
              <div className="space-y-3 mb-4">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-foreground/70">Scheduler</span>
                  <span className={isPaused ? 'text-yellow-600 font-medium' : 'text-green-600 font-medium'}>
                    {isPaused ? 'Paused' : 'Running'}
                  </span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-foreground/70">Jobs</span>
                  <span className="font-medium">{schedulerStatus?.jobCount ?? jobs?.length ?? 0}</span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-foreground/70">Realtime events</span>
                  <span className="text-green-600 font-medium">Enabled</span>
                </div>
              </div>

              <h4 className="text-sm font-semibold text-foreground/80 mb-2">Top affected paths</h4>
              {topRiskPaths.length === 0 ? (
                <div className="text-sm text-green-600 bg-green-500/10 border border-green-500/20 rounded-lg p-3 flex items-center gap-2">
                  <ShieldCheck className="w-4 h-4" />
                  No issue-heavy paths detected.
                </div>
              ) : (
                <div className="space-y-2">
                  {topRiskPaths.map((item) => (
                    <div key={item.name} className="flex items-center justify-between text-sm bg-background border border-foreground/10 rounded p-2">
                      <span className="truncate pr-2">{item.name}</span>
                      <span className="text-yellow-600 font-medium">{item.issues} issues</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          <div className="bg-card border border-foreground/10 rounded-lg p-6">
            <h3 className="text-lg font-semibold flex items-center gap-2 mb-4">
              <FolderTree className="w-5 h-5" />
              Quick Actions
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
              <Link to="/collections" className="px-4 py-3 bg-background border border-foreground/10 rounded-lg hover:border-accent/50 transition-colors text-sm font-medium text-center">
                Manage Collections
              </Link>
              <Link to="/paths" className="px-4 py-3 bg-background border border-foreground/10 rounded-lg hover:border-accent/50 transition-colors text-sm font-medium text-center">
                Browse Paths
              </Link>
              <Link to="/scans" className="px-4 py-3 bg-background border border-foreground/10 rounded-lg hover:border-accent/50 transition-colors text-sm font-medium text-center">
                Open Scans
              </Link>
              <Link to="/settings" className="px-4 py-3 bg-background border border-foreground/10 rounded-lg hover:border-accent/50 transition-colors text-sm font-medium text-center">
                Settings
              </Link>
              <button
                onClick={toggleScheduler}
                disabled={pauseScheduler.isPending || resumeScheduler.isPending}
                className="px-4 py-3 bg-background border border-foreground/10 rounded-lg hover:border-accent/50 transition-colors text-sm font-medium inline-flex items-center justify-center gap-2 disabled:opacity-50"
              >
                {isPaused ? (
                  <>
                    <PlayCircle className="w-4 h-4" />
                    Resume Scheduler
                  </>
                ) : (
                  <>
                    <PauseCircle className="w-4 h-4" />
                    Pause Scheduler
                  </>
                )}
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
