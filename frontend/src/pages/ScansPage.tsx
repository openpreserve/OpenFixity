import { useMemo, useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useCollections, usePaths, useScans, usePauseScheduler, useResumeScheduler, useSchedulerStatus, useJobs, useAlgorithms, useScheduleJob, useStartScheduledJob, useStopScheduledJob, useDeleteScheduledJob, queryKeys } from '@/hooks/api';
import type { JobDetail, PathScanWithPath, ScanStatus } from '@/types/api';
import { formatJavaDateTime, javaDateTimeToDate } from '@/types/api';
import { RefreshCw, CheckCircle, XCircle, Clock, AlertTriangle, Search, Filter, ChevronLeft, ChevronRight, PlayCircle, PauseCircle, FilePlus, FileEdit, FileCheck, FileLock, FileX, FileQuestion } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import { useQueryClient } from '@tanstack/react-query';

type TabView = 'scans' | 'jobs';

export default function ScansPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const tabParam = searchParams.get('tab') as TabView | null;

  const [activeTab, setActiveTab] = useState<TabView>(() => {
    // Check URL param first, then localStorage, then default to 'scans'
    if (tabParam && ['scans', 'jobs'].includes(tabParam)) {
      return tabParam;
    }
    const saved = localStorage.getItem('scansActiveTab');
    return (saved as TabView) || 'scans';
  });

  const { data: scans, isLoading: scansLoading, error: scansError } = useScans();
  const { data: collections } = useCollections();
  const { data: paths, isLoading: pathsLoading } = usePaths();
  const { data: algorithms } = useAlgorithms();
  const { data: schedulerStatus, isLoading: schedulerLoading } = useSchedulerStatus();
  const { data: jobs, isLoading: jobsLoading } = useJobs();
  const scheduleJob = useScheduleJob();
  const startScheduledJob = useStartScheduledJob();
  const stopScheduledJob = useStopScheduledJob();
  const deleteScheduledJob = useDeleteScheduledJob();
  const pauseScheduler = usePauseScheduler();
  const resumeScheduler = useResumeScheduler();
  const isPaused = schedulerStatus?.paused ?? false;

  const isLoading = scansLoading || pathsLoading;
  const error = scansError;

  useEffect(() => {
    document.title = activeTab === 'scans' ? 'Scans | OpenFixity' : 'Scheduled Jobs | OpenFixity';
  }, [activeTab]);

  const [statusFilter, setStatusFilter] = useState<ScanStatus | 'ALL'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [perPage, setPerPage] = useState(10);
  const [targetType, setTargetType] = useState<'path' | 'collection'>('path');
  const [selectedPathId, setSelectedPathId] = useState<number>(0);
  const [selectedCollectionName, setSelectedCollectionName] = useState('');
  const [jobTriggerKey, setJobTriggerKey] = useState('');
  const [useCron, setUseCron] = useState(false);
  const [cronExpression, setCronExpression] = useState('0 */6 * * *');
  const [everySeconds, setEverySeconds] = useState(600);
  const [jobAlgorithm, setJobAlgorithm] = useState('SHA-256');
  const [selectedJob, setSelectedJob] = useState<JobDetail | null>(null);

  useEffect(() => {
    if (!selectedPathId && paths && paths.length > 0) {
      setSelectedPathId(paths[0].id);
    }
  }, [paths, selectedPathId]);

  useEffect(() => {
    if (!selectedCollectionName && collections && collections.length > 0) {
      setSelectedCollectionName(collections[0].name);
    }
  }, [collections, selectedCollectionName]);

  useEffect(() => {
    if (algorithms && algorithms.length > 0 && !algorithms.some(a => a.id === jobAlgorithm)) {
      setJobAlgorithm(algorithms[0].id);
    }
  }, [algorithms, jobAlgorithm]);

  useEffect(() => {
    let eventSource: EventSource | null = null;
    let debounceTimer: ReturnType<typeof setTimeout> | null = null;
    let pollingInterval: ReturnType<typeof setInterval> | null = null;

    const handleScanEvent = () => {
      if (debounceTimer) {
        clearTimeout(debounceTimer);
      }
      debounceTimer = setTimeout(() => {
        queryClient.invalidateQueries({ queryKey: queryKeys.scans });
        queryClient.invalidateQueries({ queryKey: queryKeys.paths });
        queryClient.invalidateQueries({ queryKey: queryKeys.jobs });
        queryClient.invalidateQueries({ queryKey: queryKeys.scheduler });
      }, 250);
    };

    // Try to connect to event stream
    try {
      eventSource = new EventSource('/api/events');
      
      eventSource.addEventListener('scan.started', handleScanEvent);
      eventSource.addEventListener('scan.progress', handleScanEvent);
      eventSource.addEventListener('scan.completed', handleScanEvent);
      eventSource.addEventListener('scheduler.updated', handleScanEvent);
      eventSource.addEventListener('scheduler.job.changed', handleScanEvent);

      // Handle connection errors (e.g., Java backend doesn't have /api/events)
      eventSource.onerror = () => {
        console.warn('Event stream unavailable, falling back to polling');
        eventSource?.close();
        eventSource = null;

        // Fallback to polling every 5 seconds
        if (!pollingInterval) {
          pollingInterval = setInterval(() => {
            queryClient.invalidateQueries({ queryKey: queryKeys.scans });
            queryClient.invalidateQueries({ queryKey: queryKeys.paths });
          }, 5000);
        }
      };
    } catch (error) {
      console.warn('Event stream not supported, using polling');
      // Fallback to polling if EventSource not available
      pollingInterval = setInterval(() => {
        queryClient.invalidateQueries({ queryKey: queryKeys.scans });
        queryClient.invalidateQueries({ queryKey: queryKeys.paths });
      }, 5000);
    }

    return () => {
      if (debounceTimer) {
        clearTimeout(debounceTimer);
      }
      if (eventSource) {
        eventSource.close();
      }
      if (pollingInterval) {
        clearInterval(pollingInterval);
      }
    };
  }, [queryClient]);

  // Keep UI state in sync with URL params
  useEffect(() => {
    if (tabParam && ['scans', 'jobs'].includes(tabParam) && tabParam !== activeTab) {
      setActiveTab(tabParam);
      localStorage.setItem('scansActiveTab', tabParam);
    }

    const searchParam = searchParams.get('search') || '';
    if (searchParam !== searchQuery) {
      setSearchQuery(searchParam);
    }
  }, [tabParam, searchParams, activeTab, searchQuery]);

  // Handle tab change
  const handleTabChange = (tab: TabView) => {
    setActiveTab(tab);
    localStorage.setItem('scansActiveTab', tab);
    setSearchParams({ tab });
  };

  // Initialize URL param
  useEffect(() => {
    if (!tabParam) {
      setSearchParams({ tab: activeTab });
    }
  }, [tabParam, activeTab, setSearchParams]);

  // Handle scheduler toggle
  const handleSchedulerToggle = () => {
    if (isPaused) {
      resumeScheduler.mutate();
    } else {
      pauseScheduler.mutate();
    }
  };

  const handleCreateScheduledJob = () => {
    const payload: Record<string, unknown> = {
      algorithm: jobAlgorithm,
    };

    if (jobTriggerKey.trim() !== '') {
      payload.triggerKey = jobTriggerKey.trim();
    }

    if (targetType === 'path') {
      if (!selectedPathId) {
        return;
      }
      payload.pathId = selectedPathId;
    } else {
      if (!selectedCollectionName) {
        return;
      }
      payload.collectionName = selectedCollectionName;
    }

    if (useCron) {
      if (!cronExpression.trim()) {
        return;
      }
      payload.cron = cronExpression.trim();
    } else {
      payload.everySeconds = Math.max(10, everySeconds || 10);
    }

    scheduleJob.mutate(payload, {
      onSuccess: () => {
        setJobTriggerKey('');
        queryClient.invalidateQueries({ queryKey: queryKeys.jobs });
        queryClient.invalidateQueries({ queryKey: queryKeys.scheduler });
      },
    });
  };

  const handlePauseJob = (triggerKey: string) => {
    stopScheduledJob.mutate(triggerKey);
  };

  const handleStartJob = (triggerKey: string) => {
    startScheduledJob.mutate(triggerKey);
  };

  const handleDeleteJob = (triggerKey: string) => {
    if (!confirm(`Delete scheduled job "${triggerKey}"? This cannot be undone.`)) {
      return;
    }
    deleteScheduledJob.mutate(triggerKey);
    if (selectedJob?.triggerKey === triggerKey) {
      setSelectedJob(null);
    }
  };

  const getJobTargetLabel = (job: JobDetail) => {
    if (job.targetType === 'path') {
      if (job.pathName && job.pathRoot) {
        return `${job.pathName} (${job.pathRoot})`;
      }
      if (job.pathName) {
        return job.pathName;
      }
      if (job.pathId) {
        return `Path #${job.pathId}`;
      }
      return 'Path';
    }

    if (job.collectionName) {
      return `Collection: ${job.collectionName}`;
    }

    return 'Collection';
  };

  // Calculate detailed status counts from scan results
  const getStatusCounts = (scan: PathScanWithPath) => {
    // Use allFiles from Java backend, fallback to results, then to count properties
    const files = scan.allFiles || scan.results || [];
    
    if (files.length === 0) {
      return {
        verified: scan.verifiedCount || 0,
        added: scan.addedCount || 0,
        changed: scan.changedCount || 0,
        unverified: scan.unverifiedCount || 0,
        damaged: scan.damagedCount || 0,
        denied: scan.deniedCount || 0,
        notfound: scan.notFoundCount || 0,
        ignored: scan.ignoredCount || 0,
      };
    }

    const counts = {
      verified: 0,
      added: 0,
      changed: 0,
      unverified: 0,
      damaged: 0,
      denied: 0,
      notfound: 0,
      ignored: 0,
    };

    files.forEach(result => {
      // Prioritize scan status (DENIED, DAMAGED, NOTFOUND, IGNORED) over audit status
      if (result.status === 'DENIED') {
        counts.denied++;
      } else if (result.status === 'DAMAGED') {
        counts.damaged++;
      } else if (result.status === 'NOTFOUND') {
        counts.notfound++;
      } else if (result.status === 'IGNORED') {
        counts.ignored++;
      } else {
        // Use audit status for SCANNED files
        switch (result.auditStatus) {
          case 'VERIFIED': counts.verified++; break;
          case 'ADDED': counts.added++; break;
          case 'CHANGED': counts.changed++; break;
          case 'UNVERIFIED': counts.unverified++; break;
          case 'IGNORED': counts.ignored++; break;
        }
      }
    });

    return counts;
  };

  // Flatten all scans from all paths
  const allScans = useMemo<PathScanWithPath[]>(() => {
    if (!scans || !paths) return [];

    return scans.map(scan => {
      // Prefer scan.collectionPath if available (Java backend includes this now)
      if (scan.collectionPath) {
        return {
          ...scan,
          pathId: scan.collectionPath.id,
          pathName: scan.collectionPath.name,
          pathRoot: scan.collectionPath.root,
        };
      }
      
      // Fallback: Find associated path by matching scan's summary.path with paths' root
      const scanPath = scan.summary?.path || scan.summaryRecord?.path || '';
      const matchedPath = paths.find(p => p.root === scanPath);
      
      return {
        ...scan,
        pathId: matchedPath?.id || 0,
        pathName: matchedPath?.name || 'Unknown',
        pathRoot: matchedPath?.root || scanPath || 'Unknown Root',
      };
    }).sort((a, b) => {
      // Sort by started time, newest first
      const aDate = javaDateTimeToDate(a.started);
      const bDate = javaDateTimeToDate(b.started);
      if (!aDate || !bDate) return 0;
      return bDate.getTime() - aDate.getTime();
    });
  }, [scans, paths]);

  const selectedJobScans = useMemo(() => {
    if (!selectedJob) return [] as PathScanWithPath[];

    if (selectedJob.targetType === 'path' && selectedJob.pathId) {
      return allScans
        .filter(scan => scan.pathId === selectedJob.pathId)
        .slice(0, 3);
    }

    if (selectedJob.targetType === 'collection' && selectedJob.collectionName) {
      return allScans
        .filter(scan => scan.pathName.toLowerCase().includes(selectedJob.collectionName!.toLowerCase()))
        .slice(0, 3);
    }

    return [] as PathScanWithPath[];
  }, [selectedJob, allScans]);

  // Apply filters
  const filteredScans = useMemo(() => {
    return allScans.filter(scan => {
      // Status filter
      if (statusFilter !== 'ALL' && scan.status !== statusFilter) {
        return false;
      }

      // Search filter
      if (searchQuery) {
        const query = searchQuery.toLowerCase();
        return scan.pathName.toLowerCase().includes(query) ||
          scan.pathRoot.toLowerCase().includes(query);
      }

      return true;
    });
  }, [allScans, statusFilter, searchQuery]);

  // Pagination
  const totalPages = perPage === -1 ? 1 : Math.ceil(filteredScans.length / perPage);
  const paginatedScans = useMemo(() => {
    if (perPage === -1) return filteredScans;
    const start = (currentPage - 1) * perPage;
    return filteredScans.slice(start, start + perPage);
  }, [filteredScans, currentPage, perPage]);

  // Reset to page 1 when filters change
  useMemo(() => setCurrentPage(1), [statusFilter, searchQuery, perPage]);

  // Status counts
  const statusCounts = useMemo(() => {
    return {
      all: allScans.length,
      initialised: allScans.filter(s => s.status === 'INITIALISED').length,
      started: allScans.filter(s => s.status === 'STARTED').length,
      completed: allScans.filter(s => s.status === 'COMPLETED').length,
      failed: allScans.filter(s => s.status === 'FAILED').length,
    };
  }, [allScans]);

  const getStatusBadge = (status: ScanStatus, scan?: PathScanWithPath) => {
    const config: Record<ScanStatus, { icon: any; color: string; label: string; animate?: boolean }> = {
      INITIALISED: { icon: Clock, color: 'text-foreground/50 bg-foreground/5 border border-foreground/10', label: 'Initialized' },
      STARTED: { icon: RefreshCw, color: 'text-blue-600 bg-blue-500/10 border border-blue-500/20', label: 'Running', animate: true },
      COMPLETED: { icon: CheckCircle, color: 'text-green-600 bg-green-500/10 border border-green-500/20', label: 'Completed' },
      FAILED: { icon: XCircle, color: 'text-red-600 bg-red-500/10 border border-red-500/20', label: 'Failed' },
    };

    // Show warning if completed but has issues
    const hasIssues = scan && (scan.damagedCount > 0 || scan.deniedCount > 0);
    if (status === 'COMPLETED' && hasIssues) {
      return (
        <span className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium text-yellow-600 bg-yellow-500/10 border border-yellow-500/20`}>
          <AlertTriangle className={`w-3 h-3`} />
          Completed with issues
        </span>
      );
    }

    const { icon: Icon, color, label, animate = false } = config[status];
    return (
      <span className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium ${color}`}>
        <Icon className={`w-3 h-3 ${animate ? 'animate-spin' : ''}`} />
        {label}
      </span>
    );
  };

  const getRelativeTime = (dateTime: [number, number, number, number, number, number, number] | undefined | null) => {
    if (!dateTime) return 'N/A';
    const date = javaDateTimeToDate(dateTime);
    if (!date) return 'N/A';
    try {
      return formatDistanceToNow(date, { addSuffix: true });
    } catch {
      return formatJavaDateTime(dateTime);
    }
  };

  const formatDateTime = (dateTime: [number, number, number, number, number, number, number] | null) => {
    if (!dateTime) return '—';
    const date = javaDateTimeToDate(dateTime);
    if (!date) return '—';
    return date.toLocaleString();
  };

  if (isLoading) {
    return (
      <div className="text-foreground p-6">
        <div className="flex items-center gap-2">
          <RefreshCw className="w-5 h-5 animate-spin" />
          <span>Loading scans...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-foreground p-6">
        <div className="bg-card border border-foreground/10 rounded-lg p-6">
          <h1 className="text-3xl font-bold mb-4">Path Scans</h1>
          <div className="bg-red-500/10 border border-red-500/20 rounded-lg p-4">
            <div className="flex items-start gap-3">
              <AlertTriangle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
              <div>
                <h3 className="font-semibold text-red-600 mb-1">Unable to load scans</h3>
                <p className="text-sm text-foreground/70 mb-2">
                  The /api/paths endpoint is unavailable. This typically happens when:
                </p>
                <ul className="text-sm text-foreground/60 list-disc list-inside space-y-1 mb-3">
                  <li>Root path "/" was registered (causes JSON serialization failure)</li>
                  <li>Backend needs database reset: <code className="bg-foreground/10 px-1 rounded">rm -rf ~/.openfixity/</code></li>
                </ul>
                <p className="text-xs text-foreground/50">
                  See java-requirements.md "Known Backend Issues" section for details.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="text-foreground p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold mb-2">Scans & Scheduled Jobs</h1>
        <p className="text-foreground/60">
          {activeTab === 'scans'
            ? 'View completed and active scans for all paths.'
            : 'Manage scheduled jobs and fixity check automation.'}
        </p>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 mb-6 border-b border-foreground/10">
        <button
          onClick={() => handleTabChange('scans')}
          className={`px-4 py-2 font-medium transition-colors relative ${activeTab === 'scans'
            ? 'text-primary'
            : 'text-foreground/60 hover:text-foreground'
            }`}
        >
          Scans
          {activeTab === 'scans' && (
            <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-primary" />
          )}
        </button>
        <button
          onClick={() => handleTabChange('jobs')}
          className={`px-4 py-2 font-medium transition-colors relative ${activeTab === 'jobs'
            ? 'text-primary'
            : 'text-foreground/60 hover:text-foreground'
            }`}
        >
          Scheduled Jobs
          {activeTab === 'jobs' && (
            <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-primary" />
          )}
        </button>
      </div>

      {/* Scans Tab Content */}
      {activeTab === 'scans' && (
        <div>

          {/* Status Filter Tabs */}
          <div className="flex gap-2 overflow-x-auto pb-2 mb-6">
            <button
              onClick={() => setStatusFilter('ALL')}
              className={`px-4 py-2 rounded-lg transition-colors flex-shrink-0 ${statusFilter === 'ALL'
                ? 'bg-primary text-primary-foreground'
                : 'bg-card hover:bg-foreground/5 border border-foreground/10'
                }`}
            >
              All ({statusCounts.all})
            </button>
            <button
              onClick={() => setStatusFilter('STARTED')}
              className={`px-4 py-2 rounded-lg transition-colors flex-shrink-0 ${statusFilter === 'STARTED'
                ? 'bg-primary text-primary-foreground'
                : 'bg-card hover:bg-foreground/5 border border-foreground/10'
                }`}
            >
              Running ({statusCounts.started})
            </button>
            <button
              onClick={() => setStatusFilter('COMPLETED')}
              className={`px-4 py-2 rounded-lg transition-colors flex-shrink-0 ${statusFilter === 'COMPLETED'
                ? 'bg-primary text-primary-foreground'
                : 'bg-card hover:bg-foreground/5 border border-foreground/10'
                }`}
            >
              Completed ({statusCounts.completed})
            </button>
            <button
              onClick={() => setStatusFilter('FAILED')}
              className={`px-4 py-2 rounded-lg transition-colors flex-shrink-0 ${statusFilter === 'FAILED'
                ? 'bg-primary text-primary-foreground'
                : 'bg-card hover:bg-foreground/5 border border-foreground/10'
                }`}
            >
              Failed ({statusCounts.failed})
            </button>
          </div>

          {/* Search */}
          <div className="relative mb-6">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-foreground/40" />
            <input
              type="text"
              placeholder="Search by path name..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-card border border-foreground/10 rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          {/* Per Page Selector */}
          <div className="flex justify-between items-center mb-4">
            <div className="flex items-center gap-2">
              <label htmlFor="per-page-select" className="text-sm text-foreground/60">Show:</label>
              <select
                id="per-page-select"
                value={perPage}
                onChange={(e) => setPerPage(parseInt(e.target.value))}
                className="px-3 py-1.5 bg-card border border-foreground/20 text-foreground rounded-lg text-sm [&>option]:bg-card [&>option]:text-foreground"
              >
                <option value="10">10</option>
                <option value="25">25</option>
                <option value="50">50</option>
                <option value="100">100</option>
                <option value="-1">All</option>
              </select>
              <span className="text-sm text-foreground/60">per page</span>
            </div>
            {filteredScans.length > 0 && (
              <div className="text-sm text-foreground/60">
                Showing {perPage === -1 ? filteredScans.length : Math.min((currentPage - 1) * perPage + 1, filteredScans.length)}-{perPage === -1 ? filteredScans.length : Math.min(currentPage * perPage, filteredScans.length)} of {filteredScans.length}
              </div>
            )}
          </div>

          {/* Scans Table */}
          {filteredScans.length === 0 ? (
            <div className="bg-card border border-foreground/10 rounded-lg p-12 text-center">
              <Filter className="w-16 h-16 mx-auto mb-4 text-foreground/30" />
              <p className="text-foreground/60 mb-2">
                {allScans.length === 0 ? 'No scans yet' : 'No scans match your filters'}
              </p>
              {allScans.length === 0 && (
                <p className="text-sm text-foreground/40">
                  Trigger a scan from the Collections or Paths page to see results here.
                </p>
              )}
            </div>
          ) : (
            <div className="bg-card border border-foreground/10 rounded-lg overflow-hidden mb-6">
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-foreground/5 border-b border-foreground/10">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-foreground/70 uppercase tracking-wider">
                        Path
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-foreground/70 uppercase tracking-wider">
                        Status
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-foreground/70 uppercase tracking-wider">
                        Started
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-foreground/70 uppercase tracking-wider">
                        Duration
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-foreground/70 uppercase tracking-wider">
                        Files
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-foreground/70 uppercase tracking-wider">
                        Outcomes
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-foreground/10">
                    {paginatedScans.map((scan) => (
                      <tr
                        key={scan.id}
                        onClick={() => navigate(`/scans/${scan.id}`)}
                        className="hover:bg-foreground/5 transition-colors cursor-pointer"
                      >
                        <td className="px-6 py-4">
                          <div className="font-medium text-foreground">{scan.pathName}</div>
                          <div className="text-sm text-foreground/60 font-mono truncate max-w-xs">
                            {scan.pathRoot}
                          </div>
                        </td>
                        <td className="px-6 py-4">
                          {getStatusBadge(scan.status, scan)}
                        </td>
                        <td className="px-6 py-4 text-sm text-foreground/80">
                          {getRelativeTime(scan.started)}
                        </td>
                        <td className="px-6 py-4 text-sm text-foreground/80">
                          {scan.duration}
                        </td>
                        <td className="px-6 py-4 text-sm">
                          <div className="text-foreground/80">
                            {scan.summary.totalFiles.toLocaleString()} files
                          </div>
                          <div className="text-xs text-foreground/60">
                            {scan.summary.formattedTotalBytes}
                          </div>
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex flex-wrap gap-2">
                            {(() => {
                              const statusCounts = getStatusCounts(scan);

                              // Show status badges with hover tooltips
                              const hasIssues = statusCounts.damaged > 0 || statusCounts.denied > 0 || statusCounts.notfound > 0;

                              return (
                                <>
                                  {statusCounts.verified > 0 && !hasIssues && (
                                    <span
                                      className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium text-green-600 bg-green-500/10 border border-green-500/20 cursor-help"
                                      title="Files verified - checksums match previous scan"
                                    >
                                      <FileCheck className="w-3 h-3" />
                                      {statusCounts.verified}
                                    </span>
                                  )}
                                  {statusCounts.unverified > 0 && (
                                    <span
                                      className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium text-gray-600 bg-gray-500/10 border border-gray-500/20 cursor-help"
                                      title="Files not yet verified - need subsequent scan to verify checksums"
                                    >
                                      <FileQuestion className="w-3 h-3" />
                                      {statusCounts.unverified} unverified
                                    </span>
                                  )}
                                  {statusCounts.added > 0 && (
                                    <span
                                      className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium text-blue-600 bg-blue-500/10 border border-blue-500/20 cursor-help"
                                      title="New files added since last scan - click to investigate"
                                    >
                                      <FilePlus className="w-3 h-3" />
                                      {statusCounts.added} added
                                    </span>
                                  )}
                                  {statusCounts.changed > 0 && (
                                    <span
                                      className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium text-yellow-600 bg-yellow-500/10 border border-yellow-500/20 cursor-help"
                                      title="Files modified since last scan - click to investigate"
                                    >
                                      <FileEdit className="w-3 h-3" />
                                      {statusCounts.changed} changed
                                    </span>
                                  )}
                                  {statusCounts.damaged > 0 && (
                                    <span
                                      className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium text-red-600 bg-red-500/10 border border-red-500/20 cursor-help"
                                      title="Files with checksum mismatches indicating corruption - requires immediate attention"
                                    >
                                      <FileX className="w-3 h-3" />
                                      {statusCounts.damaged} damaged
                                    </span>
                                  )}
                                  {statusCounts.denied > 0 && (
                                    <span
                                      className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium text-yellow-600 bg-yellow-500/10 border border-yellow-500/20 cursor-help"
                                      title="Files that could not be accessed due to permissions"
                                    >
                                      <FileLock className="w-3 h-3" />
                                      {statusCounts.denied} denied
                                    </span>
                                  )}
                                  {statusCounts.notfound > 0 && (
                                    <span
                                      className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium text-orange-600 bg-orange-500/10 border border-orange-500/20 cursor-help"
                                      title="Files that existed in previous scan but are now missing"
                                    >
                                      <FileQuestion className="w-3 h-3" />
                                      {statusCounts.notfound} missing
                                    </span>
                                  )}
                                  {statusCounts.ignored > 0 && (
                                    <span
                                      className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium text-gray-600 bg-gray-500/10 border border-gray-500/20 cursor-help"
                                      title="Files that were intentionally ignored during scan"
                                    >
                                      <FileQuestion className="w-3 h-3" />
                                      {statusCounts.ignored} ignored
                                    </span>
                                  )}
                                </>
                              );
                            })()}
                            {scan.status === 'FAILED' && (
                              <span
                                className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium text-red-600 bg-red-500/10 border border-red-500/20 cursor-help"
                                title="Scan failed to complete"
                              >
                                <XCircle className="w-3 h-3" />
                                Scan failed
                              </span>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Pagination */}
          {totalPages > 1 && perPage !== -1 && (
            <div className="flex items-center justify-between mt-4 mb-6">
              <button
                onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                disabled={currentPage === 1}
                className="inline-flex items-center gap-2 px-4 py-2 bg-card border border-foreground/10 text-foreground rounded-lg hover:bg-foreground/5 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                <ChevronLeft className="w-4 h-4" />
                Previous
              </button>
              <div className="flex items-center gap-2">
                {Array.from({ length: totalPages }, (_, i) => i + 1)
                  .filter(page => {
                    // Show first, last, current, and pages around current
                    return page === 1 ||
                      page === totalPages ||
                      (page >= currentPage - 1 && page <= currentPage + 1);
                  })
                  .map((page, index, array) => {
                    // Add ellipsis
                    const prevPage = array[index - 1];
                    const showEllipsis = prevPage && page - prevPage > 1;

                    return (
                      <div key={page} className="flex items-center gap-2">
                        {showEllipsis && <span className="text-foreground/40">...</span>}
                        <button
                          onClick={() => setCurrentPage(page)}
                          className={`px-3 py-1.5 rounded-lg transition-colors ${page === currentPage
                            ? 'bg-primary text-primary-foreground'
                            : 'bg-card border border-foreground/10 text-foreground hover:bg-foreground/5'
                            }`}
                        >
                          {page}
                        </button>
                      </div>
                    );
                  })}
              </div>
              <button
                onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                disabled={currentPage === totalPages}
                className="inline-flex items-center gap-2 px-4 py-2 bg-card border border-foreground/10 text-foreground rounded-lg hover:bg-foreground/5 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                Next
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          )}

          {/* Summary Stats */}
          {allScans.length > 0 && (
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="bg-card border border-foreground/10 rounded-lg p-4">
                <div className="text-sm text-foreground/60 mb-1">Total Scans</div>
                <div className="text-2xl font-bold text-foreground">{allScans.length}</div>
              </div>
              <div className="bg-card border border-foreground/10 rounded-lg p-4">
                <div className="text-sm text-foreground/60 mb-1">Total Files Scanned</div>
                <div className="text-2xl font-bold text-foreground">
                  {allScans.reduce((sum, scan) => sum + scan.summary.totalFiles, 0).toLocaleString()}
                </div>
              </div>
              <div className="bg-card border border-foreground/10 rounded-lg p-4">
                <div className="text-sm text-foreground/60 mb-1">Damaged Files</div>
                <div className="text-2xl font-bold text-red-600">
                  {allScans.reduce((sum, scan) => sum + scan.damagedCount, 0)}
                </div>
              </div>
              <div className="bg-card border border-foreground/10 rounded-lg p-4">
                <div className="text-sm text-foreground/60 mb-1">Access Denied</div>
                <div className="text-2xl font-bold text-yellow-600">
                  {allScans.reduce((sum, scan) => sum + scan.deniedCount, 0)}
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Scheduled Jobs Tab Content */}
      {activeTab === 'jobs' && (
        <div>
          {/* Coming Soon banner — scheduler is not implemented yet in this pre-alpha demo */}
          <div className="mb-6 rounded-lg border border-yellow-500/30 bg-yellow-500/10 px-5 py-4 flex items-start gap-3">
            <Clock className="w-5 h-5 text-yellow-600 mt-0.5 flex-none" />
            <div>
              <h2 className="text-base font-semibold text-foreground">Scheduled scans — coming soon</h2>
              <p className="text-sm text-foreground/70 mt-0.5">
                Automated, recurring scans aren't available in this pre-alpha demo yet. The controls
                below are previewed but disabled. For now, run scans on demand from the Paths page.
              </p>
            </div>
          </div>

          {/* All scheduler controls disabled until the feature ships */}
          <fieldset disabled aria-label="Scheduled jobs (coming soon)" className="min-w-0 opacity-50 pointer-events-none select-none">
          {/* Create Job Form */}
          <div className="bg-card border border-foreground/10 rounded-lg p-6 mb-6">
            <h2 className="text-xl font-semibold text-foreground mb-2">Create Scheduled Job</h2>
            <p className="text-sm text-foreground/60 mb-4">Schedule periodic scans for a path or a collection.</p>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3 mb-3">
              <div>
                <label className="block text-xs text-foreground/60 mb-1">Target Type</label>
                <select
                  value={targetType}
                  onChange={(e) => setTargetType(e.target.value as 'path' | 'collection')}
                  className="w-full px-3 py-2 bg-card border border-foreground/20 text-foreground rounded-lg text-sm"
                >
                  <option value="path">Path</option>
                  <option value="collection">Collection</option>
                </select>
              </div>

              {targetType === 'path' ? (
                <div>
                  <label className="block text-xs text-foreground/60 mb-1">Path</label>
                  <select
                    value={selectedPathId}
                    onChange={(e) => setSelectedPathId(parseInt(e.target.value, 10))}
                    className="w-full px-3 py-2 bg-card border border-foreground/20 text-foreground rounded-lg text-sm"
                  >
                    {(paths || []).map((path) => (
                      <option key={path.id} value={path.id}>{path.name}</option>
                    ))}
                  </select>
                </div>
              ) : (
                <div>
                  <label className="block text-xs text-foreground/60 mb-1">Collection</label>
                  <select
                    value={selectedCollectionName}
                    onChange={(e) => setSelectedCollectionName(e.target.value)}
                    className="w-full px-3 py-2 bg-card border border-foreground/20 text-foreground rounded-lg text-sm"
                  >
                    {(collections || []).map((collection) => (
                      <option key={collection.id} value={collection.name}>{collection.name}</option>
                    ))}
                  </select>
                </div>
              )}

              <div>
                <label className="block text-xs text-foreground/60 mb-1">Algorithm</label>
                <select
                  value={jobAlgorithm}
                  onChange={(e) => setJobAlgorithm(e.target.value)}
                  className="w-full px-3 py-2 bg-card border border-foreground/20 text-foreground rounded-lg text-sm"
                >
                  {(algorithms || []).map((algorithm) => (
                    <option key={algorithm.id} value={algorithm.id}>{algorithm.displayName}</option>
                  ))}
                </select>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3 mb-3">
              <div>
                <label className="block text-xs text-foreground/60 mb-1">Trigger Key (optional)</label>
                <input
                  type="text"
                  value={jobTriggerKey}
                  onChange={(e) => setJobTriggerKey(e.target.value)}
                  placeholder="e.g. nightly-desktop"
                  className="w-full px-3 py-2 bg-card border border-foreground/20 text-foreground rounded-lg text-sm"
                />
              </div>

              <div>
                <label className="block text-xs text-foreground/60 mb-1">Schedule Mode</label>
                <select
                  value={useCron ? 'cron' : 'interval'}
                  onChange={(e) => setUseCron(e.target.value === 'cron')}
                  className="w-full px-3 py-2 bg-card border border-foreground/20 text-foreground rounded-lg text-sm"
                >
                  <option value="interval">Interval (seconds)</option>
                  <option value="cron">Cron expression</option>
                </select>
              </div>

              {useCron ? (
                <div>
                  <label className="block text-xs text-foreground/60 mb-1">Cron</label>
                  <input
                    type="text"
                    value={cronExpression}
                    onChange={(e) => setCronExpression(e.target.value)}
                    placeholder="0 */6 * * *"
                    className="w-full px-3 py-2 bg-card border border-foreground/20 text-foreground rounded-lg text-sm"
                  />
                </div>
              ) : (
                <div>
                  <label className="block text-xs text-foreground/60 mb-1">Every (seconds)</label>
                  <input
                    type="number"
                    min={10}
                    value={everySeconds}
                    onChange={(e) => setEverySeconds(parseInt(e.target.value || '10', 10))}
                    className="w-full px-3 py-2 bg-card border border-foreground/20 text-foreground rounded-lg text-sm"
                  />
                </div>
              )}
            </div>

            <div className="flex justify-end">
              <button
                onClick={handleCreateScheduledJob}
                disabled={scheduleJob.isPending || ((targetType === 'path' && !selectedPathId) || (targetType === 'collection' && !selectedCollectionName))}
                className="inline-flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:opacity-90 transition-opacity disabled:opacity-50"
              >
                {scheduleJob.isPending ? <RefreshCw className="w-4 h-4 animate-spin" /> : <PlayCircle className="w-4 h-4" />}
                Create Scheduled Job
              </button>
            </div>
          </div>

          {/* Scheduler Control Panel */}
          <div className="bg-card border border-foreground/10 rounded-lg p-6 mb-6">
            <div className="flex justify-between items-center">
              <div>
                <h2 className="text-xl font-semibold text-foreground mb-2">Scheduler Status</h2>
                <p className="text-sm text-foreground/60">
                  The scheduler manages automated fixity checks based on your job configuration.
                </p>
              </div>
              <button
                onClick={handleSchedulerToggle}
                disabled={schedulerLoading || pauseScheduler.isPending || resumeScheduler.isPending}
                className={`flex items-center gap-2 px-6 py-3 rounded-lg font-medium transition-colors disabled:opacity-50 ${isPaused
                  ? 'bg-green-600 hover:bg-green-700 text-white'
                  : 'bg-yellow-600 hover:bg-yellow-700 text-white'
                  }`}
              >
                {isPaused ? (
                  <>
                    <PlayCircle className="w-5 h-5" />
                    Resume Scheduler
                  </>
                ) : (
                  <>
                    <PauseCircle className="w-5 h-5" />
                    Pause Scheduler
                  </>
                )}
              </button>
            </div>

            <div className="mt-4 flex items-center gap-2">
              <div className={`w-3 h-3 rounded-full ${isPaused ? 'bg-yellow-500' : 'bg-green-500'} animate-pulse`}></div>
              <span className="text-sm font-medium text-foreground">
                {schedulerLoading ? 'Checking status...' : isPaused ? 'Scheduler Paused' : 'Scheduler Running'}
              </span>
            </div>

            <div className="mt-4 grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div className="bg-foreground/5 border border-foreground/10 rounded-lg p-3">
                <div className="text-xs text-foreground/60">Jobs configured</div>
                <div className="text-xl font-semibold text-foreground">{schedulerStatus?.jobCount ?? jobs?.length ?? 0}</div>
              </div>
              <div className="bg-foreground/5 border border-foreground/10 rounded-lg p-3">
                <div className="text-xs text-foreground/60">Scheduler status</div>
                <div className="text-xl font-semibold text-foreground">{schedulerStatus?.status ?? (isPaused ? 'Paused' : 'Running')}</div>
              </div>
              <div className="bg-foreground/5 border border-foreground/10 rounded-lg p-3">
                <div className="text-xs text-foreground/60">Running</div>
                <div className="text-xl font-semibold text-foreground">{schedulerStatus?.running ? 'Yes' : 'No'}</div>
              </div>
            </div>
          </div>

          {/* Jobs List */}
          <div className="bg-card border border-foreground/10 rounded-lg overflow-hidden">
            <div className="px-6 py-4 border-b border-foreground/10 flex items-center justify-between">
              <h3 className="text-lg font-semibold text-foreground">Scheduled Jobs</h3>
              <button
                onClick={() => {
                  queryClient.invalidateQueries({ queryKey: queryKeys.jobs });
                  queryClient.invalidateQueries({ queryKey: queryKeys.scheduler });
                }}
                className="inline-flex items-center gap-2 px-3 py-1.5 bg-foreground/5 border border-foreground/10 rounded-lg text-sm text-foreground hover:bg-foreground/10 transition-colors"
              >
                <RefreshCw className="w-4 h-4" />
                Refresh
              </button>
            </div>

            {jobsLoading ? (
              <div className="p-6 flex items-center gap-2 text-foreground/70">
                <RefreshCw className="w-4 h-4 animate-spin" />
                Loading jobs...
              </div>
            ) : !jobs || jobs.length === 0 ? (
              <div className="p-10 text-center text-foreground/60">
                No scheduled jobs yet.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-foreground/5 border-b border-foreground/10">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-foreground/70 uppercase tracking-wider">Trigger</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-foreground/70 uppercase tracking-wider">Target</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-foreground/70 uppercase tracking-wider">Status</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-foreground/70 uppercase tracking-wider">Next Run</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-foreground/70 uppercase tracking-wider">Last Run</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-foreground/70 uppercase tracking-wider">Job ID</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-foreground/70 uppercase tracking-wider">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-foreground/10">
                    {jobs.map((job) => (
                      <tr key={job.jobId} onClick={() => setSelectedJob(job)} className="hover:bg-foreground/5 transition-colors cursor-pointer">
                        <td className="px-6 py-4">
                          <div className="font-medium text-foreground">{job.triggerKey}</div>
                        </td>
                        <td className="px-6 py-4 text-sm text-foreground/80">
                          {getJobTargetLabel(job)}
                        </td>
                        <td className="px-6 py-4">
                          <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${job.status === 'PAUSED'
                            ? 'text-yellow-600 bg-yellow-500/10 border border-yellow-500/20'
                            : job.status === 'STOPPED'
                              ? 'text-foreground/70 bg-foreground/5 border border-foreground/20'
                              : 'text-green-600 bg-green-500/10 border border-green-500/20'
                            }`}>
                            {job.status}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-sm text-foreground/80">
                          {formatDateTime(job.nextFireTime)}
                        </td>
                        <td className="px-6 py-4 text-sm text-foreground/80">
                          {formatDateTime(job.previousFireTime)}
                        </td>
                        <td className="px-6 py-4 text-xs text-foreground/60 font-mono">
                          {job.jobId}
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-2">
                            {job.status === 'STOPPED' ? (
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleStartJob(job.triggerKey);
                                }}
                                disabled={startScheduledJob.isPending || deleteScheduledJob.isPending || stopScheduledJob.isPending}
                                className="px-2.5 py-1.5 text-xs rounded border border-green-500/30 text-green-600 bg-green-500/10 hover:bg-green-500/20 disabled:opacity-50"
                              >
                                Start
                              </button>
                            ) : (
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handlePauseJob(job.triggerKey);
                                }}
                                disabled={stopScheduledJob.isPending || deleteScheduledJob.isPending || startScheduledJob.isPending}
                                className="px-2.5 py-1.5 text-xs rounded border border-yellow-500/30 text-yellow-600 bg-yellow-500/10 hover:bg-yellow-500/20 disabled:opacity-50"
                              >
                                Pause
                              </button>
                            )}
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                handleDeleteJob(job.triggerKey);
                              }}
                              disabled={deleteScheduledJob.isPending || stopScheduledJob.isPending || startScheduledJob.isPending}
                              className="px-2.5 py-1.5 text-xs rounded border border-red-500/30 text-red-600 bg-red-500/10 hover:bg-red-500/20 disabled:opacity-50"
                            >
                              Delete
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
          </fieldset>
        </div>
      )}

      {selectedJob && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4" onClick={() => setSelectedJob(null)}>
          <div className="w-full max-w-3xl max-h-[85vh] bg-card border border-foreground/10 rounded-lg shadow-xl overflow-hidden" onClick={(e) => e.stopPropagation()}>
            <div className="px-6 py-4 border-b border-foreground/10 flex items-center justify-between">
              <h3 className="text-lg font-semibold text-foreground">Scheduled Job Details</h3>
              <button onClick={() => setSelectedJob(null)} className="px-2 py-1 text-sm rounded border border-foreground/20 hover:bg-foreground/10">Close</button>
            </div>

            <div className="overflow-y-auto max-h-[calc(85vh-64px)]">
            <div className="p-6 grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <div className="text-xs text-foreground/60">Trigger Key</div>
                <div className="font-medium text-foreground">{selectedJob.triggerKey}</div>
              </div>
              <div>
                <div className="text-xs text-foreground/60">Status</div>
                <div className="font-medium text-foreground">{selectedJob.status}</div>
              </div>
              <div>
                <div className="text-xs text-foreground/60">Target</div>
                <div className="font-medium text-foreground">{getJobTargetLabel(selectedJob)}</div>
              </div>
              <div>
                <div className="text-xs text-foreground/60">Algorithm</div>
                <div className="font-medium text-foreground">{selectedJob.algorithm || 'Default'}</div>
              </div>
              <div>
                <div className="text-xs text-foreground/60">Schedule</div>
                <div className="font-medium text-foreground">
                  {selectedJob.cron ? `Cron: ${selectedJob.cron}` : `Every ${selectedJob.everySeconds || 300} seconds`}
                </div>
              </div>
              <div>
                <div className="text-xs text-foreground/60">Next Run</div>
                <div className="font-medium text-foreground">{formatDateTime(selectedJob.nextFireTime)}</div>
              </div>
            </div>

            <div className="px-6 pb-6">
              <h4 className="text-sm font-semibold text-foreground/80 mb-2">Recent Results (3)</h4>
              {selectedJobScans.length === 0 ? (
                <div className="text-sm text-foreground/60 mb-3">No recent scans linked to this scheduled target yet.</div>
              ) : (
                <div className="space-y-2 mb-3">
                  {selectedJobScans.map((scan) => (
                    <button
                      key={scan.id}
                      onClick={() => {
                        setSelectedJob(null);
                        navigate(`/scans/${scan.id}`);
                      }}
                      className="w-full text-left px-3 py-2 bg-background border border-foreground/10 rounded hover:border-accent/50"
                    >
                      <div className="text-sm font-medium text-foreground">Scan #{scan.id} • {scan.pathName}</div>
                      <div className="text-xs text-foreground/60">{getRelativeTime(scan.started)} • {scan.summary.totalFiles} files</div>
                    </button>
                  ))}
                </div>
              )}

              <div className="flex gap-2">
                {selectedJob.targetType === 'path' && selectedJob.pathRoot && (
                  <button
                    onClick={() => {
                      setSelectedJob(null);
                      navigate(`/scans?tab=scans&search=${encodeURIComponent(selectedJob.pathRoot || '')}`);
                    }}
                    className="px-3 py-2 text-sm bg-foreground/5 border border-foreground/10 rounded hover:bg-foreground/10"
                  >
                    View Path Scans
                  </button>
                )}
                {selectedJob.targetType === 'collection' && selectedJob.collectionName && (
                  <button
                    onClick={() => {
                      setSelectedJob(null);
                      navigate(`/collections/${encodeURIComponent(selectedJob.collectionName || '')}`);
                    }}
                    className="px-3 py-2 text-sm bg-foreground/5 border border-foreground/10 rounded hover:bg-foreground/10"
                  >
                    Open Collection
                  </button>
                )}
              </div>
            </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
