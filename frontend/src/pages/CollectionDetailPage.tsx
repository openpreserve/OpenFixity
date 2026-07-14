import { useState, useEffect, useMemo } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useCollection, useScans, usePaths, useDeregisterPath, useScanCollection, useScanPathInCollection, useHomeFolder, useFolderChildren, useFolderParents, useFolder, useRegisterFolder } from '@/hooks/api';
import { ArrowLeft, Plus, Folder, Trash2, Play, Calendar, Hash, Layers, Home, ChevronRight, RefreshCw, FolderOpen, X, Grid3x3, List as ListIcon, Eye, EyeOff, CheckCircle, Clock, XCircle } from 'lucide-react';
import { formatJavaDateTime, javaDateTimeToDate } from '@/types/api';

type ViewMode = 'grid' | 'table';
type SortOption = 'name-asc' | 'name-desc' | 'readable';

export default function CollectionDetailPage() {
  const { name } = useParams<{ name: string }>();
  const [showAddFolderModal, setShowAddFolderModal] = useState(false);
  const [modalFolderId, setModalFolderId] = useState<number | null>(null);
  const [modalViewMode, setModalViewMode] = useState<ViewMode>('grid');
  const [modalSortBy, setModalSortBy] = useState<SortOption>('name-asc');
  const [showHidden, setShowHidden] = useState(false);

  const { data: collection, isLoading: collectionLoading } = useCollection(name || '');
  const { data: scans } = useScans();
  const { data: allPaths } = usePaths();
  const deregisterPath = useDeregisterPath();
  const scanCollection = useScanCollection();
  const scanPathInCollection = useScanPathInCollection();

  useEffect(() => {
    if (name) {
      document.title = `${name} - Collection | OpenFixity`;
    } else {
      document.title = 'Collection | OpenFixity';
    }
  }, [name]);

  // Folder browser state for modal
  const { data: homeFolder } = useHomeFolder();
  const { data: modalChildren, isLoading: modalChildrenLoading } = useFolderChildren(modalFolderId || homeFolder?.id || 0);
  const { data: modalParents } = useFolderParents(modalFolderId || homeFolder?.id || 0);
  const { data: modalCurrentFolder } = useFolder(modalFolderId || homeFolder?.id || 0);
  const registerFolder = useRegisterFolder();

  // Initialize modal folder to home when opened
  useEffect(() => {
    if (showAddFolderModal && homeFolder && modalFolderId === null) {
      setModalFolderId(homeFolder.id);
    }
  }, [showAddFolderModal, homeFolder, modalFolderId]);

  // Helper function to compare registration timestamps
  const timestampsMatch = (ts1: number[], ts2: number[]) => {
    return ts1 && ts2 &&
      ts1[0] === ts2[0] && // year
      ts1[1] === ts2[1] && // month
      ts1[2] === ts2[2] && // day
      ts1[3] === ts2[3] && // hour
      ts1[4] === ts2[4] && // minute
      ts1[5] === ts2[5] && // second
      ts1[6] === ts2[6];   // nanosecond
  };

  // Match collection's pathRegistrations to actual CollectionPath objects via timestamps
  // The API returns pathRegistrations with timestamps but no collectionPath data,
  // so we match them with paths from the /api/paths endpoint using registration timestamps
  const collectionPaths = useMemo(() => {
    if (!collection?.pathRegistrations || !allPaths) return [];
    
    return collection.pathRegistrations
      .filter(collectionReg => collectionReg.registered && !collectionReg.deRegistered)
      .map(collectionReg => {
        // Find the matching path by comparing registration timestamps
        const matchingPath = allPaths.find(path =>
          path.registeredPaths?.some(pathReg =>
            timestampsMatch(pathReg.registeredAt, collectionReg.registeredAt)
          )
        );
        return matchingPath;
      })
      .filter((path): path is NonNullable<typeof path> => path !== undefined);
  }, [collection, allPaths]);

  const registeredPathsCount = collectionPaths.length;

  const collectionPathIds = useMemo(() => new Set(collectionPaths.map(path => path.id)), [collectionPaths]);

  const collectionScans = useMemo(() => {
    if (!scans || collectionPathIds.size === 0) return [];
    return scans
      .filter((scan) => scan.collectionPath?.id && collectionPathIds.has(scan.collectionPath.id))
      .sort((a, b) => {
        const aDate = javaDateTimeToDate(a.started)?.getTime() || 0;
        const bDate = javaDateTimeToDate(b.started)?.getTime() || 0;
        return bDate - aDate;
      });
  }, [scans, collectionPathIds]);

  const recentCollectionScans = collectionScans.slice(0, 5);

  const scanStats = useMemo(() => {
    const totalScans = collectionScans.length;
    const running = collectionScans.filter(scan => scan.status === 'STARTED').length;
    const completed = collectionScans.filter(scan => scan.status === 'COMPLETED').length;
    const failed = collectionScans.filter(scan => scan.status === 'FAILED').length;
    const totalFiles = collectionScans.reduce((sum, scan) => sum + (scan.summary?.totalFiles || 0), 0);
    const damaged = collectionScans.reduce((sum, scan) => sum + (scan.damagedCount || 0), 0);
    const denied = collectionScans.reduce((sum, scan) => sum + (scan.deniedCount || 0), 0);
    const missing = collectionScans.reduce((sum, scan) => sum + (scan.notFoundCount || 0), 0);

    return {
      totalScans,
      running,
      completed,
      failed,
      totalFiles,
      damaged,
      denied,
      missing,
    };
  }, [collectionScans]);

  const getScanStatusBadge = (status: string) => {
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
          <CheckCircle className="w-3 h-3" />
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
        <Clock className="w-3 h-3" />
        Initialised
      </span>
    );
  };

  const handleDeregister = (pathId: number, pathName: string) => {
    if (!name) return;

    if (confirm(`Are you sure you want to deregister "${pathName}" from this collection?`)) {
      deregisterPath.mutate({ collectionName: name, pathId });
    }
  };

  const handleScanCollection = () => {
    if (!name) return;
    // Collection scans run the backend default algorithm (no per-collection override in the API).
    scanCollection.mutate({ name });
  };

  const handleScanPath = (pathId: number) => {
    if (!name) return;
    scanPathInCollection.mutate({ collectionName: name, pathId });
  };

  const handleModalFolderClick = (folderId: number) => {
    setModalFolderId(folderId);
  };

  const handleRegisterModalFolder = (folderId: number) => {
    if (!name) return;

    registerFolder.mutate(
      { collectionName: name, folderId },
      {
        onSuccess: () => {
          setShowAddFolderModal(false);
          setModalFolderId(null);
        }
      }
    );
  };

  // Sort and filter visible modal children
  const visibleModalChildren = (modalChildren || [])
    .filter((folder) => showHidden || !folder.isHidden)
    .sort((a, b) => {
      switch (modalSortBy) {
        case 'name-asc':
          return a.name.toLowerCase().localeCompare(b.name.toLowerCase());
        case 'name-desc':
          return b.name.toLowerCase().localeCompare(a.name.toLowerCase());
        case 'readable':
          if (a.isReadable === b.isReadable) {
            return a.name.toLowerCase().localeCompare(b.name.toLowerCase());
          }
          return a.isReadable ? -1 : 1;
        default:
          return 0;
      }
    });

  // Build modal breadcrumbs
  const modalBreadcrumbs = modalParents ? [...modalParents].reverse() : [];
  if (modalCurrentFolder && modalCurrentFolder.name !== '/') {
    modalBreadcrumbs.push(modalCurrentFolder);
  }

  const modalCurrentPath = modalBreadcrumbs.length > 0
    ? modalBreadcrumbs.map(p => p.name).filter(name => name !== '/').join('/').replace(/^/, '/')
    : '/';

  if (!name) {
    return <div className="p-6 text-foreground">Collection not found</div>;
  }

  if (collectionLoading) {
    return (
      <div className="p-6">
        <div className="animate-pulse space-y-4">
          <div className="h-8 bg-foreground/10 rounded w-1/3"></div>
          <div className="h-64 bg-foreground/10 rounded"></div>
        </div>
      </div>
    );
  }

  if (!collection) {
    return (
      <div className="p-6 text-foreground">
        <p>Collection "{name}" not found</p>
        <Link to="/collections" className="text-accent hover:underline">
          Back to Collections
        </Link>
      </div>
    );
  }

  return (
    <div className="p-6 text-foreground">
      {/* Header */}
      <div className="mb-6">
        <Link
          to="/collections"
          className="inline-flex items-center gap-2 text-accent hover:underline mb-4"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to Collections
        </Link>
        <div className="flex items-center justify-between">
          <h1 className="text-3xl font-bold">{collection.name}</h1>
          <div className="flex items-center gap-3">
            <button
              onClick={handleScanCollection}
              disabled={scanCollection.isPending}
              className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:opacity-90 transition-opacity disabled:opacity-50"
              title="Scan all paths in collection"
            >
              {scanCollection.isPending ? (
                <RefreshCw className="w-4 h-4 animate-spin" />
              ) : (
                <Play className="w-4 h-4" />
              )}
              Scan All
            </button>
            <button
              onClick={() => setShowAddFolderModal(true)}
              className="flex items-center gap-2 px-4 py-2 bg-accent text-accent-foreground rounded-lg hover:opacity-90 transition-opacity"
            >
              <Plus className="w-4 h-4" />
              Add Path
            </button>
          </div>
        </div>
      </div>

      {/* Collection Info Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div className="bg-card border border-foreground/10 rounded-lg p-4">
          <div className="flex items-center gap-2 text-foreground/60 text-sm mb-1">
            <Hash className="w-4 h-4" />
            Collection ID
          </div>
          <div className="text-2xl font-bold text-foreground">{collection.id}</div>
        </div>

        <div className="bg-card border border-foreground/10 rounded-lg p-4">
          <div className="flex items-center gap-2 text-foreground/60 text-sm mb-1">
            <Calendar className="w-4 h-4" />
            Created
          </div>
          <div className="text-lg font-medium text-foreground">
            {formatJavaDateTime(collection.created)}
          </div>
        </div>

        <div className="bg-card border border-foreground/10 rounded-lg p-4">
          <div className="flex items-center gap-2 text-foreground/60 text-sm mb-1">
            <Layers className="w-4 h-4" />
            Registered Paths
          </div>
          <div className="text-2xl font-bold text-foreground">
            {registeredPathsCount}
          </div>
        </div>
      </div>

      {/* Job ID Info */}
      <div className="bg-card border border-foreground/10 rounded-lg p-4 mb-6">
        <div className="text-sm text-foreground/60 mb-1">Job ID</div>
        <div className="font-mono text-foreground">{collection.jobId}</div>
      </div>

      {/* Registered Paths */}
      <div className="bg-card border border-foreground/10 rounded-lg p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-semibold text-foreground flex items-center gap-2">
            <Folder className="w-5 h-5" />
            Registered Paths
          </h2>
        </div>

        {collectionPaths.length === 0 ? (
          <div className="text-center py-12">
            <Folder className="w-16 h-16 mx-auto mb-4 text-foreground/30" />
            <p className="text-foreground/60 mb-4">No paths registered yet</p>
            <button
              onClick={() => setShowAddFolderModal(true)}
              className="inline-flex items-center gap-2 px-6 py-3 bg-accent hover:bg-accent-hover text-accent-foreground rounded-lg transition-colors"
            >
              <Plus className="w-5 h-5" />
              Add First Path
            </button>
          </div>
        ) : (
          <div className="space-y-3">
            {collectionPaths.map((path) => (
              <div
                key={path.id}
                className="flex items-center justify-between p-4 bg-background border border-foreground/10 rounded-lg hover:border-accent/50 transition-colors"
              >
                <div className="flex items-center gap-3 flex-1">
                  <Folder className="w-5 h-5 text-accent flex-shrink-0" />
                  <div className="flex-1 min-w-0">
                    <div className="font-medium text-foreground truncate">
                      {path.name}
                    </div>
                    <div className="text-sm text-foreground/60 font-mono truncate">
                      {path.fullPath}
                    </div>
                    <div className="text-xs text-foreground/50 mt-1">
                      Path ID: {path.id} • Added: {formatJavaDateTime(path.added)}
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <Link
                    to={`/scans?tab=scans&search=${encodeURIComponent(path.fullPath)}`}
                    className="p-2 text-foreground/60 hover:text-accent hover:bg-accent/10 rounded transition-colors"
                    title="View scan history for this path"
                  >
                    <Calendar className="w-4 h-4" />
                  </Link>
                  <button
                    onClick={() => handleScanPath(path.id)}
                    disabled={scanPathInCollection.isPending}
                    className="p-2 text-foreground/60 hover:text-primary hover:bg-primary/10 rounded transition-colors disabled:opacity-50"
                    title="Scan this path"
                  >
                    {scanPathInCollection.isPending ? (
                      <RefreshCw className="w-4 h-4 animate-spin" />
                    ) : (
                      <Play className="w-4 h-4" />
                    )}
                  </button>
                  <button
                    onClick={() => handleDeregister(path.id, path.name)}
                    disabled={deregisterPath.isPending}
                    className="p-2 text-foreground/60 hover:text-red-600 hover:bg-red-500/10 rounded transition-colors disabled:opacity-50"
                    title="Deregister path"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Collection Scan Insights */}
      <div className="mt-6 grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-card border border-foreground/10 rounded-lg p-6">
          <h3 className="text-lg font-semibold text-foreground mb-2">Recent Scans</h3>
          {recentCollectionScans.length === 0 ? (
            <p className="text-sm text-foreground/60">No scans for this collection yet.</p>
          ) : (
            <div className="space-y-3">
              {recentCollectionScans.map((scan) => (
                <Link
                  key={scan.id}
                  to={`/scans/${scan.id}`}
                  className="flex items-center justify-between gap-3 p-3 bg-background border border-foreground/10 rounded-lg hover:border-accent/50 transition-colors"
                >
                  <div className="min-w-0">
                    <div className="text-sm font-medium text-foreground truncate">
                      {scan.collectionPath?.name || 'Unknown Path'}
                    </div>
                    <div className="text-xs text-foreground/60">
                      {formatJavaDateTime(scan.started)}
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-foreground/60">
                      {(scan.summary?.totalFiles || 0).toLocaleString()} files
                    </span>
                    {getScanStatusBadge(scan.status)}
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>

        <div className="bg-card border border-foreground/10 rounded-lg p-6">
          <h3 className="text-lg font-semibold text-foreground mb-2">Scan Statistics</h3>
          <div className="grid grid-cols-2 gap-3">
            <div className="bg-background border border-foreground/10 rounded-lg p-3">
              <div className="text-xs text-foreground/60">Total Scans</div>
              <div className="text-xl font-semibold text-foreground">{scanStats.totalScans}</div>
            </div>
            <div className="bg-background border border-foreground/10 rounded-lg p-3">
              <div className="text-xs text-foreground/60">Running</div>
              <div className="text-xl font-semibold text-blue-600">{scanStats.running}</div>
            </div>
            <div className="bg-background border border-foreground/10 rounded-lg p-3">
              <div className="text-xs text-foreground/60">Completed</div>
              <div className="text-xl font-semibold text-green-600">{scanStats.completed}</div>
            </div>
            <div className="bg-background border border-foreground/10 rounded-lg p-3">
              <div className="text-xs text-foreground/60">Failed</div>
              <div className="text-xl font-semibold text-red-600">{scanStats.failed}</div>
            </div>
            <div className="bg-background border border-foreground/10 rounded-lg p-3">
              <div className="text-xs text-foreground/60">Files Scanned</div>
              <div className="text-xl font-semibold text-foreground">{scanStats.totalFiles.toLocaleString()}</div>
            </div>
            <div className="bg-background border border-foreground/10 rounded-lg p-3">
              <div className="text-xs text-foreground/60">Issues</div>
              <div className="text-xl font-semibold text-yellow-600">{scanStats.damaged + scanStats.denied + scanStats.missing}</div>
              <div className="text-[11px] text-foreground/50">{scanStats.damaged} damaged • {scanStats.denied} denied • {scanStats.missing} missing</div>
            </div>
          </div>
        </div>
      </div>

      {/* Add Path Modal */}
      {showAddFolderModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4" onClick={() => {
          setShowAddFolderModal(false);
          setModalFolderId(null);
        }}>
          <div className="bg-card border border-foreground/10 rounded-lg shadow-xl max-w-6xl w-full max-h-[85vh] overflow-hidden flex flex-col" onClick={(e) => e.stopPropagation()}>
            {/* Modal Header */}
            <div className="sticky top-0 bg-card border-b border-foreground/10 p-4 flex justify-between items-center">
              <h2 className="text-xl font-semibold text-foreground">Browse and Add Path to Collection</h2>
              <button
                onClick={() => {
                  setShowAddFolderModal(false);
                  setModalFolderId(null);
                }}
                className="p-2 hover:bg-foreground/10 rounded transition-colors"
                aria-label="Close modal"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-6">
              {/* Controls Bar */}
              <div className="flex justify-between items-center mb-4 gap-4 flex-wrap">
                <div className="flex items-center text-sm text-foreground/60">
                  <Home className="h-4 w-4 mr-2" />
                  <span className="font-mono">{modalCurrentPath || '/'}</span>
                </div>

                <div className="flex items-center gap-2">
                  {/* Sort Dropdown */}
                  <select
                    value={modalSortBy}
                    onChange={(e) => setModalSortBy(e.target.value as SortOption)}
                    className="px-3 py-2 bg-card border border-foreground/20 text-foreground rounded-lg hover:bg-foreground/5 transition-colors text-sm [&>option]:bg-card [&>option]:text-foreground"
                    aria-label="Sort folders by"
                  >
                    <option value="name-asc">Name (A-Z)</option>
                    <option value="name-desc">Name (Z-A)</option>
                    <option value="readable">Readable First</option>
                  </select>

                  {/* View Mode Toggle */}
                  <div className="flex bg-foreground/10 rounded-lg p-1">
                    <button
                      onClick={() => setModalViewMode('grid')}
                      className={`p-2 rounded transition-colors ${modalViewMode === 'grid'
                          ? 'bg-primary text-primary-foreground'
                          : 'text-foreground/60 hover:text-foreground'
                        }`}
                      title="Grid view"
                    >
                      <Grid3x3 className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => setModalViewMode('table')}
                      className={`p-2 rounded transition-colors ${modalViewMode === 'table'
                          ? 'bg-primary text-primary-foreground'
                          : 'text-foreground/60 hover:text-foreground'
                        }`}
                      title="Table view"
                    >
                      <ListIcon className="w-4 h-4" />
                    </button>
                  </div>

                  {/* Show/Hide Hidden Toggle */}
                  <button
                    onClick={() => setShowHidden(!showHidden)}
                    className="flex items-center gap-2 px-3 py-2 bg-foreground/10 text-foreground rounded-lg hover:bg-foreground/20 transition-colors"
                    title={showHidden ? 'Hide hidden folders' : 'Show hidden folders'}
                  >
                    {showHidden ? <Eye className="w-4 h-4" /> : <EyeOff className="w-4 h-4" />}
                    <span className="hidden sm:inline text-sm">{showHidden ? 'Hide' : 'Show'}</span>
                  </button>
                </div>
              </div>

              {/* Breadcrumb Navigation */}
              <div className="bg-background border border-foreground/10 rounded-lg p-4 mb-6">
                <div className="flex items-center justify-between gap-4 flex-wrap">
                  <div className="flex items-center gap-2 text-sm flex-wrap flex-1">
                    <button
                      onClick={() => homeFolder && setModalFolderId(homeFolder.id)}
                      className="flex items-center gap-1 px-2 py-1 hover:bg-foreground/10 rounded transition-colors"
                    >
                      <Home className="w-4 h-4" />
                      <span>Home</span>
                    </button>

                    {modalBreadcrumbs.map((parent) => (
                      <div key={parent.id} className="flex items-center gap-2">
                        <ChevronRight className="w-4 h-4 text-foreground/30" />
                        <button
                          onClick={() => handleModalFolderClick(parent.id)}
                          className="px-2 py-1 hover:bg-foreground/10 rounded transition-colors text-foreground"
                        >
                          {parent.name}
                        </button>
                      </div>
                    ))}
                  </div>

                  {/* Register Current Folder Button */}
                  {modalCurrentFolder && modalCurrentFolder.isReadable && (
                    <button
                      onClick={() => handleRegisterModalFolder(modalFolderId || homeFolder?.id || 0)}
                      disabled={registerFolder.isPending}
                      className="flex items-center gap-2 px-4 py-2 bg-accent text-accent-foreground rounded-lg hover:opacity-90 disabled:opacity-50 transition-opacity flex-shrink-0"
                    >
                      <Plus className="w-4 h-4" />
                      <span>Register This Path</span>
                    </button>
                  )}
                </div>
              </div>

              {/* Folder List */}
              {modalChildrenLoading ? (
                <div className="flex items-center justify-center py-12">
                  <RefreshCw className="h-8 w-8 animate-spin text-foreground/40" />
                </div>
              ) : visibleModalChildren.length > 0 ? (
                modalViewMode === 'grid' ? (
                  /* Grid View */
                  <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                    {visibleModalChildren.map((folder) => (
                      <div key={folder.id} className="relative">
                        <button
                          onClick={() => handleModalFolderClick(folder.id)}
                          disabled={!folder.isReadable}
                          className="w-full flex flex-col items-center p-4 border border-foreground/10 rounded-lg hover:border-accent hover:bg-foreground/5 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          {folder.isReadable ? (
                            <FolderOpen className="h-12 w-12 mb-2 text-accent" />
                          ) : (
                            <Folder className="h-12 w-12 mb-2 text-foreground/30" />
                          )}
                          <span className="text-sm font-medium text-foreground text-center break-all">
                            {folder.name}
                          </span>

                          <div className="flex gap-2 text-xs mt-2">
                            {folder.isHidden && (
                              <span className="px-2 py-0.5 bg-foreground/10 text-foreground/70 rounded">
                                Hidden
                              </span>
                            )}
                            {!folder.isReadable && (
                              <span className="px-2 py-0.5 bg-red-500/10 text-red-600 rounded">
                                No access
                              </span>
                            )}
                          </div>
                        </button>

                        {folder.isReadable && (
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              handleRegisterModalFolder(folder.id);
                            }}
                            disabled={registerFolder.isPending}
                            className="absolute top-2 right-2 p-1.5 bg-accent text-accent-foreground rounded hover:opacity-90 transition-opacity shadow-lg"
                            title="Register this folder"
                          >
                            <Plus className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                    ))}
                  </div>
                ) : (
                  /* Table View */
                  <div className="bg-background border border-foreground/10 rounded-lg overflow-hidden">
                    <table className="w-full">
                      <thead className="bg-primary/10">
                        <tr>
                          <th className="text-left p-4 font-semibold text-foreground">Name</th>
                          <th className="text-left p-4 font-semibold text-foreground hidden md:table-cell">Status</th>
                          <th className="text-right p-4 font-semibold text-foreground">Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {visibleModalChildren.map((folder, index) => (
                          <tr
                            key={folder.id}
                            className={`${index !== visibleModalChildren.length - 1 ? 'border-b border-foreground/10' : ''} hover:bg-foreground/5 transition-colors`}
                          >
                            <td className="p-4">
                              <button
                                onClick={() => handleModalFolderClick(folder.id)}
                                disabled={!folder.isReadable}
                                className="flex items-center gap-2 text-left hover:text-primary transition-colors disabled:opacity-50 disabled:cursor-not-allowed w-full"
                              >
                                {folder.isReadable ? (
                                  <FolderOpen className="w-5 h-5 flex-shrink-0" />
                                ) : (
                                  <Folder className="w-5 h-5 flex-shrink-0 text-foreground/30" />
                                )}
                                <span className="font-medium truncate" title={folder.name}>
                                  {folder.name}
                                </span>
                              </button>
                            </td>
                            <td className="p-4 hidden md:table-cell">
                              <div className="flex gap-2 text-xs">
                                {folder.isHidden && (
                                  <span className="px-2 py-0.5 bg-foreground/10 text-foreground/70 rounded">
                                    Hidden
                                  </span>
                                )}
                                {!folder.isReadable && (
                                  <span className="px-2 py-0.5 bg-red-500/10 text-red-600 rounded">
                                    No access
                                  </span>
                                )}
                              </div>
                            </td>
                            <td className="p-4 text-right">
                              {folder.isReadable && (
                                <button
                                  onClick={() => handleRegisterModalFolder(folder.id)}
                                  disabled={registerFolder.isPending}
                                  className="inline-flex items-center gap-1 px-3 py-1.5 bg-accent text-accent-foreground rounded hover:opacity-90 transition-opacity disabled:opacity-50"
                                  title="Register this folder"
                                >
                                  <Plus className="w-4 h-4" />
                                  <span className="hidden sm:inline">Register</span>
                                </button>
                              )}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )
              ) : (
                <div className="text-center py-12 text-foreground/60">
                  <FolderOpen className="h-16 w-16 mx-auto mb-4 opacity-50" />
                  <p>No {showHidden ? '' : 'visible '}subpaths in this directory</p>
                  {!showHidden && (
                    <button
                      onClick={() => setShowHidden(true)}
                      className="mt-4 text-sm text-accent hover:underline"
                    >
                      Show hidden folders
                    </button>
                  )}
                </div>
              )}
            </div>

            <div className="p-4 border-t border-foreground/10 flex justify-between items-center">
              <div className="text-sm text-foreground/60">
                {visibleModalChildren.length} folder{visibleModalChildren.length !== 1 ? 's' : ''}
                {!showHidden && modalChildren && modalChildren.filter(f => f.isHidden).length > 0 && (
                  <span className="ml-2">
                    ({modalChildren.filter(f => f.isHidden).length} hidden)
                  </span>
                )}
              </div>
              <button
                onClick={() => {
                  setShowAddFolderModal(false);
                  setModalFolderId(null);
                }}
                className="px-4 py-2 text-foreground hover:bg-foreground/10 rounded transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
