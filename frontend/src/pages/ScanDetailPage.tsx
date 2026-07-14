import { useParams, Link, useNavigate } from 'react-router-dom';
import { useAlgorithms, useRecheckScanFile, useScan, useScanPath, queryKeys } from '@/hooks/api';
import { defaultAlgorithm, AlgorithmHint } from '@/lib/algorithm';
import { useMemo, useState, useEffect } from 'react';
import { RefreshCw, ArrowLeft, CheckCircle, XCircle, Clock, FileCheck, FileLock, FileX, FileQuestion, FilePlus, FileEdit, File, Copy, Check, ArrowUpDown, ArrowUp, ArrowDown, Search, X, ChevronLeft, ChevronRight, List, FolderTree, ChevronDown, ChevronRight as ChevronRightIcon, Download, AlertTriangle } from 'lucide-react';
import type { FileRecheckResult, FileScanResult, PathAuditStatus } from '@/types/api';
import { javaDateTimeToDate } from '@/types/api';
import { formatDistanceToNow } from 'date-fns';
import { apiClient } from '@/lib/api-client';
import { useQueryClient } from '@tanstack/react-query';
import { useTutorial } from '@/lib/tutorial';

// Status icons mapping from PathAuditStatus enum with descriptions
const statusIcons: Record<PathAuditStatus, { icon: any; color: string; label: string; description: string }> = {
  DAMAGED: { icon: FileX, color: 'text-red-600 bg-red-500/10', label: 'Damaged', description: 'File checksum mismatch - file has been modified or corrupted' },
  DENIED: { icon: FileLock, color: 'text-yellow-600 bg-yellow-500/10', label: 'Denied', description: 'Access denied - insufficient permissions to read file' },
  IGNORED: { icon: File, color: 'text-foreground/50 bg-foreground/5', label: 'Ignored', description: 'File was skipped during scan' },
  NOTFOUND: { icon: FileQuestion, color: 'text-orange-600 bg-orange-500/10', label: 'Not Found', description: 'File not found - may have been deleted' },
  ADDED: { icon: FilePlus, color: 'text-blue-600 bg-blue-500/10', label: 'Added', description: 'Newly added file not seen in previous scan' },
  CHANGED: { icon: FileEdit, color: 'text-yellow-600 bg-yellow-500/10', label: 'Changed', description: 'File modified since last scan' },
  VERIFIED: { icon: FileCheck, color: 'text-green-600 bg-green-500/10', label: 'Verified', description: 'File verified - checksum matches previous scan' },
  UNVERIFIED: { icon: FileX, color: 'text-red-600 bg-red-500/10', label: 'Unverified', description: 'File not yet verified' },
};

// Helper to get the display status - prioritize scan status (DENIED, DAMAGED, NOTFOUND) over audit status
const getDisplayStatus = (result: FileScanResult): PathAuditStatus => {
  // If scan status indicates a problem, show that instead of audit status
  if (result.status === 'DENIED') return 'DENIED';
  if (result.status === 'DAMAGED') return 'DAMAGED';
  if (result.status === 'NOTFOUND') return 'NOTFOUND';
  if (result.status === 'IGNORED') return 'IGNORED';

  if (!result.auditStatus || result.auditStatus.trim() === '') {
    if (result.status === 'SCANNED') return 'VERIFIED';
    return 'IGNORED';
  }

  // Otherwise show the audit status
  return result.auditStatus;
};

// Helper to make paths relative to scan root
const makeRelativePath = (filePath: string | undefined, rootPath: string | undefined): string => {
  if (!filePath) return '';
  
  // Remove file:// protocol and decode URL encoding
  const decodedPath = filePath.replace(/^file:\/\//, '').replace(/%20/g, ' ');
  
  if (!rootPath) return decodedPath;
  
  // Remove file:// protocol from root path and ensure it ends with /
  const decodedRoot = rootPath.replace(/^file:\/\//, '').replace(/\/$/, '');
  
  // If the file path starts with the root path, make it relative
  if (decodedPath.startsWith(decodedRoot)) {
    const relative = decodedPath.slice(decodedRoot.length);
    // Remove leading slash for cleaner display
    return relative.startsWith('/') ? relative.slice(1) : relative;
  }
  
  return decodedPath;
};

const resolveAbsolutePath = (filePath: string | undefined, rootPath: string | undefined): string => {
  if (!filePath) return '';
  const cleanFilePath = filePath.replace(/^file:\/\//, '');
  if (!rootPath) return cleanFilePath;

  const cleanRoot = rootPath.replace(/^file:\/\//, '').replace(/\/$/, '');
  return cleanFilePath.startsWith('/') ? `${cleanRoot}${cleanFilePath}` : `${cleanRoot}/${cleanFilePath}`;
};

const isPreviewable = (path: string): boolean => {
  const lower = path.toLowerCase();
  return lower.endsWith('.png') || lower.endsWith('.jpg') || lower.endsWith('.jpeg') || lower.endsWith('.gif') || lower.endsWith('.webp') || lower.endsWith('.pdf') || lower.endsWith('.txt') || lower.endsWith('.md') || lower.endsWith('.json') || lower.endsWith('.xml') || lower.endsWith('.csv');
};

/**
 * Convert base64-encoded digest to hexadecimal string
 * Backend serializes digestBytes as base64, but we need hex for display
 */
const base64ToHex = (base64: string): string => {
  try {
    const binaryString = atob(base64);
    let hex = '';
    for (let i = 0; i < binaryString.length; i++) {
      const byte = binaryString.charCodeAt(i);
      hex += byte.toString(16).padStart(2, '0');
    }
    return hex;
  } catch {
    return '';
  }
};

const getDigestValue = (result: FileScanResult, index = 0): string => {
  const digest = result.digestResults?.[index];
  if (!digest) return '';
  // Use hexString first (proper field from backend)
  if (digest.hexString && digest.hexString.trim() !== '') return digest.hexString;
  // Fallback to legacy fields
  if (digest.hex && digest.hex.trim() !== '') return digest.hex;
  if (digest.value && digest.value.trim() !== '') return digest.value;
  // Convert base64 digestBytes to hex
  if (digest.digestBytes && digest.digestBytes.trim() !== '') {
    return base64ToHex(digest.digestBytes);
  }
  return '';
};

const shortenDigest = (digest: string, head = 10, tail = 10): string => {
  if (!digest) return '';
  if (digest.length <= head + tail + 3) return digest;
  return `${digest.slice(0, head)}...${digest.slice(-tail)}`;
};

export default function ScanDetailPage() {
  const tutorial = useTutorial();
  const { id } = useParams<{ id: string }>();
  const scanId = id ? parseInt(id, 10) : 0;
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  
  const { data: scan, isLoading, error } = useScan(scanId);
  const { data: algorithms } = useAlgorithms();
  const scanPath = useScanPath();
  const recheckScanFile = useRecheckScanFile();
  const [selectedAlgorithm, setSelectedAlgorithm] = useState('');
  const [recheckingPath, setRecheckingPath] = useState<string | null>(null);
  const [fileRecheckResult, setFileRecheckResult] = useState<FileRecheckResult | null>(null);
  const [fileRecheckError, setFileRecheckError] = useState<string | null>(null);
  const [fileRecheckCompletedAt, setFileRecheckCompletedAt] = useState<Date | null>(null);
  const [previewPath, setPreviewPath] = useState<string | null>(null);
  
  useEffect(() => {
    if (scan) {
      document.title = `Scan #${scanId} - ${scan.collectionPath?.name || scan.collectionPath?.root?.replace('file://', '') || 'Unknown'} | OpenFixity`;
    } else {
      document.title = 'Scan Details | OpenFixity';
    }
  }, [scan, scanId]);

  // Default the picker to the backend default algorithm once the list loads.
  useEffect(() => {
    if (!selectedAlgorithm && algorithms?.length) {
      setSelectedAlgorithm(defaultAlgorithm(algorithms));
    }
  }, [algorithms, selectedAlgorithm]);

  // Set algorithm to match the current scan's algorithm for apples-to-apples rescans
  useEffect(() => {
    if (scan?.allFiles && scan.allFiles.length > 0) {
      const firstFile = scan.allFiles[0];
      if (firstFile.digestResults && firstFile.digestResults.length > 0) {
        const scanAlgorithm = firstFile.digestResults[0].algorithm;
        if (scanAlgorithm) {
          setSelectedAlgorithm(scanAlgorithm);
        }
      }
    } else if (scan?.results && scan.results.length > 0) {
      const firstFile = scan.results[0];
      if (firstFile.digestResults && firstFile.digestResults.length > 0) {
        const scanAlgorithm = firstFile.digestResults[0].algorithm;
        if (scanAlgorithm) {
          setSelectedAlgorithm(scanAlgorithm);
        }
      }
    }
  }, [scan]);

  useEffect(() => {
    if (!scanId || !scan) return;
    if (tutorial.enabled && tutorial.status === 'running' && tutorial.currentStep === 'reviewResults') {
      tutorial.completeStep('reviewResults', { scanId });
    }
  }, [scanId, scan, tutorial]);

  useEffect(() => {
    if (!scanId) return;

    let eventSource: EventSource | null = null;
    let pollingInterval: ReturnType<typeof setInterval> | null = null;

    const refreshScanData = () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.scan(scanId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.scans });
      queryClient.invalidateQueries({ queryKey: queryKeys.paths });
    };

    const onScanEvent = (event: MessageEvent) => {
      try {
        const payload = JSON.parse(event.data) as { scanId?: number };
        if (payload.scanId && payload.scanId === scanId) {
          refreshScanData();
        }
      } catch {
        // Ignore malformed event payloads
      }
    };

    // Try to connect to event stream
    try {
      eventSource = new EventSource('/api/events');

      eventSource.addEventListener('scan.progress', onScanEvent);
      eventSource.addEventListener('scan.completed', onScanEvent);
      eventSource.addEventListener('scan.started', onScanEvent);

      // Handle connection errors (Java backend doesn't have /api/events)
      eventSource.onerror = () => {
        eventSource?.close();
        eventSource = null;

        // Fallback to polling every 3 seconds for this specific scan
        if (!pollingInterval) {
          pollingInterval = setInterval(refreshScanData, 3000);
        }
      };
    } catch (error) {
      // Fallback to polling if EventSource not available
      pollingInterval = setInterval(refreshScanData, 3000);
    }

    return () => {
      if (eventSource) {
        eventSource.close();
      }
      if (pollingInterval) {
        clearInterval(pollingInterval);
      }
    };
  }, [scanId, queryClient]);
  
  // Debug logging (only in dev mode)
  const devMode = localStorage.getItem('devMode') === 'true';
  if (devMode) {
    console.log('ScanDetailPage:', { scanId, scan, isLoading, error });
  }
  
  // State for filtering, sorting, and pagination
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<PathAuditStatus | 'ALL'>('ALL');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [sortBy, setSortBy] = useState<'path' | 'status' | 'size'>('path');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');
  const [currentPage, setCurrentPage] = useState(1);
  const [perPage, setPerPage] = useState(25);
  const [copiedDigest, setCopiedDigest] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<FileScanResult | null>(null);
  const [viewMode, setViewMode] = useState<'table' | 'tree'>('table');
  const [expandedFolders, setExpandedFolders] = useState<Set<string>>(new Set());
  const [tooltipPos, setTooltipPos] = useState<{ x: number; y: number } | null>(null);
  const [hoveredStatus, setHoveredStatus] = useState<{ label: string; description: string } | null>(null);

  const handleRescan = () => {
    if (scan?.collectionPath?.id) {
      scanPath.mutate(
        { pathId: scan.collectionPath.id, algorithm: selectedAlgorithm },
        {
          onSuccess: () => {
            navigate('/scans');
          }
        }
      );
    }
  };

  // Calculate status counts from results
  const statusCounts = useMemo(() => {
    const files = scan?.allFiles || scan?.results || [];
    if (files.length === 0) return null;
    
    const counts = {
      verified: 0,
      added: 0,
      changed: 0,
      damaged: 0,
      denied: 0,
      notfound: 0,
      unverified: 0,
      ignored: 0,
    };
    
    files.forEach(result => {
      const status = getDisplayStatus(result);
      switch (status) {
        case 'VERIFIED': counts.verified++; break;
        case 'ADDED': counts.added++; break;
        case 'CHANGED': counts.changed++; break;
        case 'DAMAGED': counts.damaged++; break;
        case 'DENIED': counts.denied++; break;
        case 'NOTFOUND': counts.notfound++; break;
        case 'UNVERIFIED': counts.unverified++; break;
        case 'IGNORED': counts.ignored++; break;
      }
    });
    
    return counts;
  }, [scan?.allFiles, scan?.results]);

  // Filter and sort results
  const filteredResults = useMemo(() => {
    const files = scan?.allFiles || scan?.results || [];
    if (files.length === 0) return [];
    
    return files
      .filter(result => {
        // Status filter - need to check different fields based on status type
        // DENIED, DAMAGED, NOTFOUND are in 'status' field
        // ADDED, CHANGED, VERIFIED, etc. are in 'auditStatus' field
        if (statusFilter !== 'ALL') {
          if (statusFilter === 'DENIED' || statusFilter === 'DAMAGED' || statusFilter === 'NOTFOUND') {
            // These are FileScanStatus values - check status field
            if (result.status !== statusFilter) return false;
          } else {
            // These are PathAuditStatus values - compare against normalized display status
            if (getDisplayStatus(result) !== statusFilter) return false;
          }
        }
        
        // Date range filter (based on scanned date). `scanned` is a Java LocalDateTime array,
        // so parse it via javaDateTimeToDate (NOT new Date(arr.toString()), which is Invalid
        // Date). The dateFrom/dateTo bounds are "YYYY-MM-DD" from <input type="date">; append a
        // time so they parse in LOCAL time too, keeping both sides of the comparison consistent.
        if (dateFrom || dateTo) {
          const scannedDate = result.scanned ? javaDateTimeToDate(result.scanned) : null;
          if (scannedDate && !isNaN(scannedDate.getTime())) {
            if (dateFrom && scannedDate < new Date(`${dateFrom}T00:00:00`)) return false;
            if (dateTo && scannedDate > new Date(`${dateTo}T23:59:59.999`)) return false;
          }
        }
        
        // Search filter (must pass if specified)
        if (searchQuery) {
          const query = searchQuery.toLowerCase();
          const relativePath = makeRelativePath(result.path, scan?.collectionPath?.root);
          if (!relativePath.toLowerCase().includes(query)) return false;
        }
        
        return true;
      })
      .sort((a, b) => {
        let comparison = 0;
        
        switch (sortBy) {
          case 'path':
            comparison = (a.path || '').localeCompare(b.path || '');
            break;
          case 'status':
            comparison = getDisplayStatus(a).localeCompare(getDisplayStatus(b));
            break;
          case 'size':
            comparison = (a.length || 0) - (b.length || 0);
            break;
        }
        
        return sortOrder === 'asc' ? comparison : -comparison;
      });
  }, [scan?.allFiles, scan?.results, statusFilter, searchQuery, sortBy, sortOrder, dateFrom, dateTo]);

  // Pagination
  const totalPages = perPage === -1 ? 1 : Math.ceil(filteredResults.length / perPage);
  const paginatedResults = useMemo(() => {
    if (perPage === -1) return filteredResults;
    const start = (currentPage - 1) * perPage;
    return filteredResults.slice(start, start + perPage);
  }, [filteredResults, currentPage, perPage]);

  // Reset to page 1 when filters change
  useMemo(() => setCurrentPage(1), [statusFilter, searchQuery, perPage]);

  // Copy digest to clipboard
  const copyDigest = async (digest: string) => {
    await navigator.clipboard.writeText(digest);
    setCopiedDigest(digest);
    setTimeout(() => setCopiedDigest(null), 2000);
  };

  const toggleSort = (column: 'path' | 'status' | 'size') => {
    if (sortBy === column) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(column);
      setSortOrder('asc');
    }
  };

  const getSortIcon = (column: 'path' | 'status' | 'size') => {
    if (sortBy !== column) return <ArrowUpDown className="w-4 h-4 opacity-40" />;
    return sortOrder === 'asc' ? <ArrowUp className="w-4 h-4" /> : <ArrowDown className="w-4 h-4" />;
  };

  // Build tree structure from flat file list
  type TreeNode = {
    name: string;
    path: string;
    isFile: boolean;
    file?: FileScanResult;
    children: Map<string, TreeNode>;
  };

  const buildTree = (files: FileScanResult[]): TreeNode => {
    const root: TreeNode = { name: '', path: '', isFile: false, children: new Map() };
    
    files.forEach(file => {
      const relativePath = makeRelativePath(file.path, scan?.collectionPath?.root);
      const parts = relativePath.split('/').filter(Boolean);
      
      let current = root;
      parts.forEach((part, idx) => {
        if (!current.children.has(part)) {
          current.children.set(part, {
            name: part,
            path: parts.slice(0, idx + 1).join('/'),
            isFile: idx === parts.length - 1,
            file: idx === parts.length - 1 ? file : undefined,
            children: new Map(),
          });
        }
        current = current.children.get(part)!;
      });
    });
    
    return root;
  };

  const treeData = useMemo(() => buildTree(filteredResults), [filteredResults]);

  const toggleFolder = (path: string) => {
    setExpandedFolders(prev => {
      const next = new Set(prev);
      if (next.has(path)) {
        next.delete(path);
      } else {
        next.add(path);
      }
      return next;
    });
  };

  const handleFileRescan = (result: FileScanResult) => {
    if (!scanId) return;

    setRecheckingPath(result.path);
    setFileRecheckResult(null);
    setFileRecheckError(null);
    setFileRecheckCompletedAt(null);

    recheckScanFile.mutate(
      { scanId, path: result.path },
      {
        onSuccess: (response) => {
          setFileRecheckResult(response);
          setFileRecheckCompletedAt(new Date());
        },
        onError: (error: Error) => {
          setFileRecheckError(error.message);
          setFileRecheckCompletedAt(new Date());
        },
        onSettled: () => {
          setRecheckingPath(null);
        },
      }
    );
  };

  useEffect(() => {
    setFileRecheckResult(null);
    setFileRecheckError(null);
    setFileRecheckCompletedAt(null);
  }, [selectedFile?.path]);

  // Export functions
  const exportToCSV = () => {
    if (!filteredResults.length) return;

    const headers = ['Path', 'Display Status', 'Audit Status', 'Scan Status', 'Size (bytes)', 'Algorithm', 'Digest', 'Created', 'Modified', 'Scanned'];
    const rows = filteredResults.map(result => {
      const path = makeRelativePath(result.path, scan?.collectionPath?.root);
      const displayStatus = getDisplayStatus(result);
      const auditStatus = result.auditStatus;
      const scanStatus = result.status;
      const size = result.length?.toString() || '';
      const algorithm = (result.digestResults || []).map(d => d.algorithm).join('; ');
      const digest = (result.digestResults || []).map((_, idx) => getDigestValue(result, idx)).filter(Boolean).join('; ');
      const created = result.created?.toString() || '';
      const modified = result.modified?.toString() || '';
      const scanned = result.scanned?.toString() || '';
      
      return [path, displayStatus, auditStatus, scanStatus, size, algorithm, digest, created, modified, scanned]
        .map(field => `"${field.replace(/"/g, '""')}"`)
        .join(',');
    });

    const csv = [headers.join(','), ...rows].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `scan-${scanId}-results-${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
  };

  const exportToJSON = () => {
    if (!filteredResults.length) return;

    // Calculate status summary
    const statusSummary = {
      verified: 0,
      added: 0,
      changed: 0,
      damaged: 0,
      denied: 0,
      notfound: 0,
      unverified: 0,
      ignored: 0,
    };
    
    filteredResults.forEach(result => {
      const status = getDisplayStatus(result);
      switch (status) {
        case 'VERIFIED': statusSummary.verified++; break;
        case 'ADDED': statusSummary.added++; break;
        case 'CHANGED': statusSummary.changed++; break;
        case 'DAMAGED': statusSummary.damaged++; break;
        case 'DENIED': statusSummary.denied++; break;
        case 'NOTFOUND': statusSummary.notfound++; break;
        case 'UNVERIFIED': statusSummary.unverified++; break;
        case 'IGNORED': statusSummary.ignored++; break;
      }
    });

    const exportData = {
      scanId: scanId,
      exportDate: new Date().toISOString(),
      scanStatus: scan?.status,
      collectionPath: scan?.collectionPath?.root,
      totalFiles: filteredResults.length,
      statusSummary: statusSummary,
      results: filteredResults.map(result => ({
        path: makeRelativePath(result.path, scan?.collectionPath?.root),
        displayStatus: getDisplayStatus(result),
        auditStatus: result.auditStatus,
        scanStatus: result.status,
        size: result.length,
        algorithms: (result.digestResults || []).map(d => d.algorithm),
        digests: (result.digestResults || []).map((_, idx) => getDigestValue(result, idx)),
        created: result.created,
        modified: result.modified,
        scanned: result.scanned,
      }))
    };

    const json = JSON.stringify(exportData, null, 2);
    const blob = new Blob([json], { type: 'application/json' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `scan-${scanId}-results-${new Date().toISOString().split('T')[0]}.json`;
    link.click();
    URL.revokeObjectURL(link.href);
  };
  // Render tree recursively
  const renderTree = (node: TreeNode, depth: number): React.ReactNode => {
    const entries = Array.from(node.children.values()).sort((a, b) => {
      // Folders first, then files
      if (!a.isFile && b.isFile) return -1;
      if (a.isFile && !b.isFile) return 1;
      return a.name.localeCompare(b.name);
    });

    return entries.map((child) => {
      const isExpanded = expandedFolders.has(child.path);
      const paddingLeft = `${(depth + 1) * 1.5}rem`;

      if (child.isFile && child.file) {
        const displayStatus = getDisplayStatus(child.file);
        const statusConfig = statusIcons[displayStatus] || statusIcons.IGNORED;
        const StatusIcon = statusConfig.icon;
        
        return (
          <div key={child.path}>
            <div
              onClick={() => setSelectedFile(child.file!)}
              className="flex items-center gap-2 p-2 hover:bg-foreground/5 cursor-pointer rounded transition-colors"
              style={{ paddingLeft }}
            >
              <StatusIcon className={`w-4 h-4 flex-shrink-0 ${statusConfig.color.split(' ')[0]}`} />
              <span className="text-sm font-mono text-foreground">{child.name}</span>
              <span className={`text-xs px-2 py-0.5 rounded ${statusConfig.color}`}>
                {statusConfig.label}
              </span>
              <span className="text-xs text-foreground/40 ml-auto">
                {child.file.length ? `${(child.file.length / 1024).toFixed(1)} KiB` : ''}
              </span>
            </div>
          </div>
        );
      }

      return (
        <div key={child.path}>
          <div
            onClick={() => toggleFolder(child.path)}
            className="flex items-center gap-2 p-2 hover:bg-foreground/5 cursor-pointer rounded transition-colors"
            style={{ paddingLeft }}
          >
            {isExpanded ? (
              <ChevronDown className="w-4 h-4 text-foreground/60" />
            ) : (
              <ChevronRightIcon className="w-4 h-4 text-foreground/60" />
            )}
            <FolderTree className="w-4 h-4 text-accent" />
            <span className="text-sm font-medium text-foreground">{child.name}</span>
            <span className="text-xs text-foreground/40 ml-auto">
              {child.children.size} {child.children.size === 1 ? 'item' : 'items'}
            </span>
          </div>
          {isExpanded && (
            <div>
              {renderTree(child, depth + 1)}
            </div>
          )}
        </div>
      );
    });
  };

  if (isLoading) {
    return (
      <div className="text-foreground p-6">
        <div className="flex items-center gap-2">
          <RefreshCw className="w-5 h-5 animate-spin" />
          <span>Loading scan...</span>
        </div>
      </div>
    );
  }

  if (error || !scan) {
    return (
      <div className="text-foreground p-6">
        <div className="mb-4">
          <Link to="/scans" className="inline-flex items-center gap-2 text-accent hover:underline">
            <ArrowLeft className="w-4 h-4" />
            Back to Scans
          </Link>
        </div>
        <div className="bg-card border border-red-500/20 rounded-lg p-6">
          <XCircle className="w-12 h-12 text-red-500 mb-4" />
          <h2 className="text-xl font-semibold mb-2">Scan not found</h2>
          <p className="text-foreground/60 mb-4">
            {error?.message || 'Could not load scan details. The scan may not exist or there was an error loading the data.'}
          </p>
          <p className="text-xs text-foreground/40">Scan ID: {scanId}</p>
        </div>
      </div>
    );
  }

  const getStatusBadge = () => {
    const config = {
      INITIALISED: { icon: Clock, color: 'text-foreground/50 bg-foreground/5', label: 'Initialized', animate: false },
      STARTED: { icon: RefreshCw, color: 'text-blue-600 bg-blue-500/10', label: 'Running', animate: true },
      COMPLETED: { icon: CheckCircle, color: 'text-green-600 bg-green-500/10', label: 'Completed', animate: false },
      FAILED: { icon: XCircle, color: 'text-red-600 bg-red-500/10', label: 'Failed', animate: false },
    };
    
    const { icon: Icon, color, label, animate } = config[scan.status] || config.INITIALISED;
    return (
      <span className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm font-medium ${color}`}>
        <Icon className={`w-4 h-4 ${animate ? 'animate-spin' : ''}`} />
        {label}
      </span>
    );
  };

  return (
    <div className="text-foreground p-6">
      <div className="mb-6">
        <div className="flex items-center justify-between mb-4">
          <Link to="/scans" className="inline-flex items-center gap-2 text-accent hover:underline">
            <ArrowLeft className="w-4 h-4" />
            Back to Scans
          </Link>
          <div className="flex items-center gap-2">
            <select
              value={selectedAlgorithm}
              onChange={(e) => setSelectedAlgorithm(e.target.value)}
              className="px-3 py-2 bg-card border border-foreground/20 text-foreground rounded-lg text-sm"
            >
              {(algorithms || []).map((algorithm) => (
                <option key={algorithm.id} value={algorithm.id}>
                  {algorithm.displayName}
                </option>
              ))}
            </select>
            <button
              onClick={handleRescan}
              disabled={scanPath.isPending || !scan?.collectionPath?.id}
              className="inline-flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <RefreshCw className={`w-4 h-4 ${scanPath.isPending ? 'animate-spin' : ''}`} />
              {scanPath.isPending ? 'Starting Scan...' : 'Re-scan Path'}
            </button>
          </div>
        </div>
        <AlgorithmHint className="mb-2 text-right" />
        <h1 className="text-3xl font-bold text-accent mb-2">
          {scan.collectionPath?.name || 'Unknown Path'}
        </h1>
        <p className="text-foreground/60 mb-1">
          {scan.collectionPath?.root?.replace('file://', '') || scan.collectionPath?.fullPath || 'Unknown location'}
        </p>
        <p className="text-foreground/50 text-sm">
          Full results of scan for this path
        </p>
      </div>

      {/* Scan Summary */}
      <div className="bg-card border border-foreground/10 rounded-lg p-6 mb-6">
        <h2 className="text-lg font-semibold mb-4">Scan Summary</h2>
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
          <div>
            <span className="text-foreground/60 text-sm">Scan ID:</span>
            <div className="text-foreground font-medium">{scan.id}</div>
          </div>
          <div>
            <span className="text-foreground/60 text-sm">Status:</span>
            <div className="mt-1">{getStatusBadge()}</div>
          </div>
          <div>
            <span className="text-foreground/60 text-sm">Algorithm:</span>
            <div className="text-foreground font-medium">
              {scan.allFiles?.[0]?.digestResults?.[0]?.algorithm
                ?? scan.results?.[0]?.digestResults?.[0]?.algorithm
                ?? '—'}
            </div>
          </div>
          <div>
            <span className="text-foreground/60 text-sm">Duration:</span>
            <div className="text-foreground font-medium">{scan.duration}</div>
          </div>
          <div>
            <span className="text-foreground/60 text-sm">Scanned:</span>
            <div className="text-foreground font-medium">
              {scan.summary.totalFiles.toLocaleString()} Files, {scan.summary.formattedTotalBytes}
            </div>
          </div>
        </div>

        {/* Timestamp Information */}
        <div className="mt-4 pt-4 border-t border-foreground/10 text-xs text-foreground/60">
          <span>Started: {scan.started ? `${javaDateTimeToDate(scan.started)?.toLocaleString()} (${formatDistanceToNow(javaDateTimeToDate(scan.started)!, { addSuffix: true })})` : 'N/A'}</span>
          {scan.stopped && (
            <span className="ml-6">Finished: {javaDateTimeToDate(scan.stopped)?.toLocaleString()} ({formatDistanceToNow(javaDateTimeToDate(scan.stopped)!, { addSuffix: true })})</span>
          )}
        </div>

        {/* Status Summary with Badges */}
        {statusCounts && (scan.allFiles || scan.results) && ((scan.allFiles && scan.allFiles.length > 0) || (scan.results && scan.results.length > 0)) && (
          <div className="mt-6">
            <h3 className="text-sm font-semibold text-foreground mb-3">Scan Results Summary</h3>
            <div className="flex flex-wrap gap-2">
              {statusCounts.verified > 0 && (
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium text-green-600 bg-green-500/10 border border-green-500/20">
                  <FileCheck className="w-4 h-4" />
                  {statusCounts.verified.toLocaleString()} Verified
                </span>
              )}
              {statusCounts.added > 0 && (
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium text-blue-600 bg-blue-500/10 border border-blue-500/20">
                  <FilePlus className="w-4 h-4" />
                  {statusCounts.added.toLocaleString()} Added
                </span>
              )}
              {statusCounts.changed > 0 && (
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium text-yellow-600 bg-yellow-500/10 border border-yellow-500/20">
                  <FileEdit className="w-4 h-4" />
                  {statusCounts.changed.toLocaleString()} Changed
                </span>
              )}
              {statusCounts.damaged > 0 && (
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium text-red-600 bg-red-500/10 border border-red-500/20">
                  <FileX className="w-4 h-4" />
                  {statusCounts.damaged.toLocaleString()} Damaged
                </span>
              )}
              {statusCounts.denied > 0 && (
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium text-yellow-600 bg-yellow-500/10 border border-yellow-500/20">
                  <FileLock className="w-4 h-4" />
                  {statusCounts.denied.toLocaleString()} Denied
                </span>
              )}
              {statusCounts.notfound > 0 && (
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium text-orange-600 bg-orange-500/10 border border-orange-500/20">
                  <FileQuestion className="w-4 h-4" />
                  {statusCounts.notfound.toLocaleString()} Not Found
                </span>
              )}
              {statusCounts.unverified > 0 && (
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium text-foreground/60 bg-foreground/5 border border-foreground/10">
                  <FileX className="w-4 h-4" />
                  {statusCounts.unverified.toLocaleString()} Unverified
                </span>
              )}
              {statusCounts.ignored > 0 && (
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium text-foreground/40 bg-foreground/5 border border-foreground/10">
                  <File className="w-4 h-4" />
                  {statusCounts.ignored.toLocaleString()} Ignored
                </span>
              )}
            </div>
            
            {/* Issues Alert */}
            {(statusCounts.damaged > 0 || statusCounts.denied > 0 || statusCounts.notfound > 0) && (
              <div className="mt-4 p-3 bg-red-500/10 border border-red-500/20 rounded-lg">
                <p className="text-sm text-foreground/70">
                  <AlertTriangle className="w-4 h-4 inline mr-1 text-red-600" />
                  {statusCounts.damaged + statusCounts.denied + statusCounts.notfound} file{(statusCounts.damaged + statusCounts.denied + statusCounts.notfound) !== 1 ? 's' : ''} require{(statusCounts.damaged + statusCounts.denied + statusCounts.notfound) === 1 ? 's' : ''} attention. Use the status filter below to investigate.
                </p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Scan Results Table */}
      {((scan.allFiles && scan.allFiles.length > 0) || (scan.results && scan.results.length > 0)) && (
        <div className="space-y-4">
          {/* Search and Filters */}
          <div className="flex flex-col md:flex-row gap-4">
            {/* Search */}
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-foreground/40" />
              <input
                type="text"
                placeholder="Search by file path..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-2 bg-card border border-foreground/10 rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
            
            {/* Status Filter */}
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as PathAuditStatus | 'ALL')}
              aria-label="Filter by status"
              className="px-4 py-2 bg-card border border-foreground/20 text-foreground rounded-lg [&>option]:bg-card [&>option]:text-foreground"
            >
              <option value="ALL">All Statuses</option>
              <option value="ADDED">Added</option>
              <option value="VERIFIED">Verified</option>
              <option value="CHANGED">Changed</option>
              <option value="DAMAGED">Damaged</option>
              <option value="DENIED">Denied</option>
              <option value="NOTFOUND">Not Found</option>
              <option value="IGNORED">Ignored</option>
              <option value="UNVERIFIED">Unverified</option>
            </select>

            {/* Date Range Filter */}
            <div className="flex items-center gap-2">
              <input
                type="date"
                value={dateFrom}
                onChange={(e) => setDateFrom(e.target.value)}
                placeholder="From date"
                className="px-3 py-2 bg-card border border-foreground/20 text-foreground rounded-lg"
                aria-label="Filter from date"
              />
              <span className="text-foreground/60">to</span>
              <input
                type="date"
                value={dateTo}
                onChange={(e) => setDateTo(e.target.value)}
                placeholder="To date"
                className="px-3 py-2 bg-card border border-foreground/20 text-foreground rounded-lg"
                aria-label="Filter to date"
              />
              {(dateFrom || dateTo) && (
                <button
                  onClick={() => {
                    setDateFrom('');
                    setDateTo('');
                  }}
                  className="p-2 hover:bg-foreground/10 rounded-lg transition-colors"
                  title="Clear date filter"
                >
                  <X className="w-4 h-4" />
                </button>
              )}
            </div>

            {/* View Mode Toggle */}
            <div className="flex gap-2 bg-card border border-foreground/10 rounded-lg p-1">
              <button
                onClick={() => setViewMode('table')}
                className={`px-3 py-2 rounded flex items-center gap-2 transition-colors ${
                  viewMode === 'table'
                    ? 'bg-primary text-primary-foreground'
                    : 'hover:bg-foreground/5'
                }`}
              >
                <List className="w-4 h-4" />
                <span className="text-sm">Table</span>
              </button>
              <button
                onClick={() => setViewMode('tree')}
                className={`px-3 py-2 rounded flex items-center gap-2 transition-colors ${
                  viewMode === 'tree'
                    ? 'bg-primary text-primary-foreground'
                    : 'hover:bg-foreground/5'
                }`}
              >
                <FolderTree className="w-4 h-4" />
                <span className="text-sm">Tree</span>
              </button>
            </div>

            {/* Export Buttons */}
            <div className="flex gap-2">
              <button
                onClick={exportToCSV}
                disabled={filteredResults.length === 0}
                className="px-3 py-2 bg-card border border-foreground/10 rounded-lg hover:bg-foreground/5 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2"
                title="Export as CSV"
              >
                <Download className="w-4 h-4" />
                <span className="text-sm">CSV</span>
              </button>
              <button
                onClick={exportToJSON}
                disabled={filteredResults.length === 0}
                className="px-3 py-2 bg-card border border-foreground/10 rounded-lg hover:bg-foreground/5 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2"
                title="Export as JSON"
              >
                <Download className="w-4 h-4" />
                <span className="text-sm">JSON</span>
              </button>
            </div>
          </div>

          {/* Per Page Selector */}
          <div className="flex justify-between items-center">
            <div className="flex items-center gap-2">
              <label htmlFor="results-per-page" className="text-sm text-foreground/60">Show:</label>
              <select
                id="results-per-page"
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
            {filteredResults.length > 0 && (
              <div className="text-sm text-foreground/60">
                Showing {perPage === -1 ? filteredResults.length : Math.min((currentPage - 1) * perPage + 1, filteredResults.length)}-{perPage === -1 ? filteredResults.length : Math.min(currentPage * perPage, filteredResults.length)} of {filteredResults.length}
              </div>
            )}
          </div>

          {/* Table or Tree View */}
          {viewMode === 'table' ? (
            <div className="bg-card border border-foreground/10 rounded-lg overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-primary/10">
                    <tr>
                      <th 
                        className="text-left p-4 font-semibold text-foreground cursor-pointer hover:bg-primary/20 transition-colors"
                        onClick={() => toggleSort('path')}
                      >
                        <div className="flex items-center gap-2">
                          Path
                          {getSortIcon('path')}
                        </div>
                      </th>
                      <th 
                        className="text-left p-4 font-semibold text-foreground cursor-pointer hover:bg-primary/20 transition-colors"
                        onClick={() => toggleSort('status')}
                      >
                        <div className="flex items-center gap-2">
                          Status
                          {getSortIcon('status')}
                        </div>
                      </th>
                      <th 
                        className="text-left p-4 font-semibold text-foreground cursor-pointer hover:bg-primary/20 transition-colors"
                        onClick={() => toggleSort('size')}
                      >
                        <div className="flex items-center gap-2">
                          Size
                          {getSortIcon('size')}
                        </div>
                      </th>
                      <th className="text-left p-4 font-semibold text-foreground">Digest</th>
                    </tr>
                  </thead>
                  <tbody>
                    {paginatedResults.map((result, index) => {
                      const displayStatus = getDisplayStatus(result);
                      const statusConfig = statusIcons[displayStatus] || statusIcons.IGNORED;
                      const StatusIcon = statusConfig.icon;
                      const relativePath = makeRelativePath(result.path, scan?.collectionPath?.root);
                      const primaryAlgorithm = result.digestResults?.[0]?.algorithm || '';
                      const fullDigest = getDigestValue(result, 0);
                      const digestDisplay = shortenDigest(fullDigest);
                      
                      return (
                        <tr 
                          key={index}
                          onClick={() => setSelectedFile(result)}
                          className={`${index !== paginatedResults.length - 1 ? 'border-b border-foreground/10' : ''} hover:bg-foreground/5 transition-colors cursor-pointer`}
                        >
                          <td className="p-4">
                            <div className="flex items-center gap-2">
                              <div 
                                onMouseMove={(e) => {
                                  setTooltipPos({ x: e.clientX, y: e.clientY });
                                  setHoveredStatus({ label: statusConfig.label, description: statusConfig.description });
                                }}
                                onMouseLeave={() => {
                                  setTooltipPos(null);
                                  setHoveredStatus(null);
                                }}
                              >
                                <StatusIcon className={`w-4 h-4 flex-shrink-0 ${statusConfig.color.split(' ')[0]}`} />
                              </div>
                              <span className="text-sm font-mono text-foreground">
                                {relativePath}
                              </span>
                            </div>
                          </td>
                          <td className="p-4">
                            <span className={`px-2 py-1 rounded text-xs font-medium ${statusConfig.color}`}>
                              {displayStatus}
                            </span>
                          </td>
                          <td className="p-4 text-sm text-foreground/70">
                            {result.length ? `${(result.length / 1024).toFixed(2)} KiB` : '-'}
                          </td>
                          <td className="p-4">
                            {digestDisplay ? (
                              <div className="flex items-center gap-2">
                                {primaryAlgorithm ? (
                                  <span className="text-xs px-1.5 py-0.5 rounded bg-foreground/5 text-foreground/60 uppercase">
                                    {primaryAlgorithm}
                                  </span>
                                ) : null}
                                <span className="text-xs font-mono text-foreground/60">
                                  {digestDisplay}
                                </span>
                                <button
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    copyDigest(fullDigest);
                                  }}
                                  className="p-1 hover:bg-foreground/10 rounded transition-colors"
                                  title="Copy full digest"
                                >
                                  {copiedDigest === fullDigest ? (
                                    <Check className="w-3 h-3 text-green-600" />
                                  ) : (
                                    <Copy className="w-3 h-3 text-foreground/60" />
                                  )}
                                </button>
                              </div>
                            ) : (
                              <span className="text-xs text-foreground/40">-</span>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          ) : (
            <div className="bg-card border border-foreground/10 rounded-lg p-4">
              {/* Tree View - Recursive Component */}
              {renderTree(treeData, 0)}
            </div>
          )}

          {/* Pagination Controls - Only show in table mode */}
          {viewMode === 'table' && totalPages > 1 && (
            <div className="flex justify-center items-center gap-2">
              <button
                onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                disabled={currentPage === 1}
                aria-label="Previous page"
                className="px-3 py-2 bg-card border border-foreground/10 rounded hover:bg-foreground/5 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              
              <div className="flex gap-1">
                {Array.from({ length: Math.min(7, totalPages) }, (_, i) => {
                  let pageNum;
                  if (totalPages <= 7) {
                    pageNum = i + 1;
                  } else if (currentPage <= 4) {
                    pageNum = i + 1;
                  } else if (currentPage >= totalPages - 3) {
                    pageNum = totalPages - 6 + i;
                  } else {
                    pageNum = currentPage - 3 + i;
                  }
                  
                  if (pageNum < 1 || pageNum > totalPages) return null;
                  
                  return (
                    <button
                      key={pageNum}
                      onClick={() => setCurrentPage(pageNum)}
                      className={`px-3 py-2 rounded transition-colors ${
                        currentPage === pageNum
                          ? 'bg-primary text-primary-foreground'
                          : 'bg-card border border-foreground/10 hover:bg-foreground/5'
                      }`}
                    >
                      {pageNum}
                    </button>
                  );
                })}
              </div>
              
              <button
                onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                disabled={currentPage === totalPages}
                aria-label="Next page"
                className="px-3 py-2 bg-card border border-foreground/10 rounded hover:bg-foreground/5 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          )}
        </div>
      )}

      {/* File Detail Modal */}
      {selectedFile && (
        <div 
          className="fixed inset-0 bg-background/80 backdrop-blur-sm flex items-center justify-center z-50 p-4"
          onClick={() => setSelectedFile(null)}
        >
          <div 
            className="bg-card border border-foreground/10 rounded-lg max-w-3xl w-full max-h-[90vh] overflow-y-auto shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="sticky top-0 bg-card border-b border-foreground/10 p-6 flex justify-between items-start">
              <div className="flex-1 mr-4">
                <h2 className="text-xl font-bold text-accent mb-2">File Details</h2>
                <p className="text-sm font-mono text-foreground/70 break-all">
                  {makeRelativePath(selectedFile.path, scan?.collectionPath?.root)}
                </p>
              </div>
              <button
                onClick={() => setSelectedFile(null)}
                aria-label="Close modal"
                className="p-2 hover:bg-foreground/10 rounded-lg transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            
            <div className="p-6 space-y-6">
              {/* Status */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <h3 className="text-sm font-semibold text-foreground/60">Status</h3>
                  <div className="flex items-center gap-2">
                    {(() => {
                      const absolutePath = resolveAbsolutePath(selectedFile.path, scan?.collectionPath?.root);
                      return absolutePath && isPreviewable(absolutePath) ? (
                        <button
                          onClick={() => setPreviewPath(absolutePath)}
                          className="inline-flex items-center gap-2 px-3 py-1.5 text-xs border border-foreground/20 rounded hover:bg-foreground/5"
                          title="Preview file"
                        >
                          Preview
                        </button>
                      ) : null;
                    })()}
                    <button
                      onClick={() => handleFileRescan(selectedFile)}
                      disabled={recheckScanFile.isPending && recheckingPath === selectedFile.path}
                      className="inline-flex items-center gap-2 px-3 py-1.5 text-xs border border-foreground/20 rounded hover:bg-foreground/5 disabled:opacity-50 disabled:cursor-not-allowed"
                      title="Re-scan this file and compare with stored digest"
                    >
                      <RefreshCw className={`w-3 h-3 ${(recheckScanFile.isPending && recheckingPath === selectedFile.path) ? 'animate-spin' : ''}`} />
                      {(recheckScanFile.isPending && recheckingPath === selectedFile.path) ? 'Checking...' : 'Re-scan & Compare'}
                    </button>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  {(() => {
                    const displayStatus = getDisplayStatus(selectedFile);
                    const statusConfig = statusIcons[displayStatus] || statusIcons.IGNORED;
                    const StatusIcon = statusConfig.icon;
                    return (
                      <>
                        <StatusIcon className={`w-5 h-5 ${statusConfig.color.split(' ')[0]}`} />
                        <div>
                          <div className={`px-3 py-1.5 rounded text-sm font-medium inline-block ${statusConfig.color}`}>
                            {statusConfig.label}
                          </div>
                          <p className="text-sm text-foreground/60 mt-1">{statusConfig.description}</p>
                        </div>
                      </>
                    );
                  })()}
                </div>
              </div>

              {/* Re-scan Comparison Result */}
              {fileRecheckError && (
                <div className="border border-red-500/20 bg-red-500/10 rounded-lg p-4">
                  <h3 className="text-sm font-semibold text-red-600 mb-1">Re-scan check failed</h3>
                  <p className="text-sm text-foreground/70">{fileRecheckError}</p>
                </div>
              )}

              {fileRecheckResult && fileRecheckResult.path === selectedFile.path && (
                <div className={`border rounded-lg p-4 ${fileRecheckResult.matchedAll ? 'border-green-500/20 bg-green-500/10' : 'border-yellow-500/20 bg-yellow-500/10'}`}>
                  <h3 className={`text-sm font-semibold mb-2 ${fileRecheckResult.matchedAll ? 'text-green-600' : 'text-yellow-700'}`}>
                    {fileRecheckResult.matchedAll ? 'Re-scan result: file matches stored scan' : 'Re-scan result: file differs from stored scan'}
                  </h3>
                  <p className="text-xs text-foreground/60 mb-3">
                    Current file was hashed and compared against this scan record{fileRecheckCompletedAt ? ` at ${fileRecheckCompletedAt.toLocaleTimeString()}` : ''}.
                  </p>
                  <div className="space-y-2">
                    {fileRecheckResult.comparisons.map((comparison) => (
                      <div key={comparison.algorithm} className="bg-card border border-foreground/10 rounded p-3">
                        <div className="flex items-center justify-between mb-2">
                          <span className="text-xs font-semibold text-foreground/60 uppercase">{comparison.algorithm}</span>
                          <span className={`text-xs px-2 py-0.5 rounded ${comparison.matched ? 'text-green-700 bg-green-500/20' : 'text-red-700 bg-red-500/20'}`}>
                            {comparison.matched ? 'MATCH' : 'DIFFERENT'}
                          </span>
                        </div>
                        <div className="mt-2 rounded bg-foreground/5 p-2">
                          <pre className="text-xs font-mono text-foreground/70 whitespace-pre-wrap break-all">Stored:  {comparison.storedDigest || '-'}</pre>
                          <pre className="text-xs font-mono text-foreground/70 whitespace-pre-wrap break-all mt-1">Current: {comparison.currentDigest || '-'}</pre>
                        </div>
                        {comparison.error ? (
                          <div className="text-xs text-red-600 mt-2">Error: {comparison.error}</div>
                        ) : null}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* File Info */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <h3 className="text-sm font-semibold text-foreground/60 mb-2">Size</h3>
                  <p className="text-foreground">
                    {selectedFile.length ? `${(selectedFile.length / 1024).toFixed(2)} KiB (${selectedFile.length.toLocaleString()} bytes)` : '-'}
                  </p>
                </div>
                <div>
                  <h3 className="text-sm font-semibold text-foreground/60 mb-2">Scan Status</h3>
                  <p className="text-foreground">{selectedFile.status}</p>
                </div>
              </div>

              {/* Digests */}
              {(selectedFile.digestResults && selectedFile.digestResults.length > 0) ? (
                <div>
                  <h3 className="text-sm font-semibold text-foreground/60 mb-2">Checksums</h3>
                  <div className="space-y-3">
                    {(selectedFile.digestResults || []).map((digest, index) => {
                      const fullDigest = getDigestValue(selectedFile, index);
                      const copied = copiedDigest === fullDigest;
                      return (
                      <div key={`${digest.algorithm}-${index}`} className="bg-background border border-foreground/10 rounded-lg p-4">
                        <div className="flex justify-between items-start mb-2">
                          <span className="text-xs font-semibold text-foreground/60 uppercase">{digest.algorithm}</span>
                          <button
                            onClick={() => {
                              copyDigest(fullDigest);
                            }}
                            className="px-2 py-1 text-xs bg-foreground/5 hover:bg-foreground/10 rounded flex items-center gap-1 transition-colors"
                          >
                            {copied ? (
                              <>
                                <Check className="w-3 h-3" />
                                Copied
                              </>
                            ) : (
                              <>
                                <Copy className="w-3 h-3" />
                                Copy
                              </>
                            )}
                          </button>
                        </div>
                        <code className="text-xs font-mono text-foreground break-all block">
                          {fullDigest || '-'}
                        </code>
                      </div>
                      );
                    })}
                  </div>
                </div>
              ) : null}
            </div>
          </div>
        </div>
      )}

      {previewPath && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-[60] p-4" onClick={() => setPreviewPath(null)}>
          <div className="bg-card border border-foreground/10 rounded-lg shadow-xl w-full max-w-4xl max-h-[90vh] overflow-hidden" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between p-4 border-b border-foreground/10">
              <h3 className="text-lg font-semibold text-foreground truncate">{previewPath}</h3>
              <button onClick={() => setPreviewPath(null)} className="p-2 hover:bg-foreground/10 rounded transition-colors" aria-label="Close preview">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="p-4 bg-background">
              {previewPath.toLowerCase().endsWith('.pdf') ? (
                <iframe src={apiClient.getFileUrl(previewPath)} title="PDF Preview" className="w-full h-[70vh] rounded border border-foreground/10" />
              ) : previewPath.toLowerCase().match(/\.(png|jpg|jpeg|gif|webp)$/) ? (
                <img src={apiClient.getFileUrl(previewPath)} alt="File Preview" className="max-h-[70vh] mx-auto rounded border border-foreground/10" />
              ) : (
                <iframe src={apiClient.getFileUrl(previewPath)} title="File Preview" className="w-full h-[70vh] rounded border border-foreground/10" />
              )}
            </div>
          </div>
        </div>
      )}

      {/* Floating Tooltip */}
      {tooltipPos && hoveredStatus && (
        <div 
          className="fixed px-3 py-2 bg-foreground text-background text-xs rounded-lg shadow-lg pointer-events-none whitespace-nowrap z-50"
          style={{
            left: `${tooltipPos.x}px`,
            top: `${tooltipPos.y}px`,
            transform: 'translate(8px, -100%)',
          }}
        >
          <div className="font-semibold">{hoveredStatus.label}</div>
          <div className="max-w-xs">{hoveredStatus.description}</div>
        </div>
      )}
    </div>
  );
}
