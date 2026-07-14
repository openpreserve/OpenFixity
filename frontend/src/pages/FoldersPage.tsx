import { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { useHomeFolder, useFolderChildren, useFolderParents, useFolder, useRegisterFolder, useCollections, usePaths, useScanPath } from '@/hooks/api';
import { Folder, FolderOpen, Home, ChevronRight, RefreshCw, Eye, EyeOff, Plus, X, Play, Grid3x3, List, FileText } from 'lucide-react';
import { formatJavaDateTime } from '@/types/api';
import { DEFAULT_ALGORITHM } from '@/lib/algorithm';
import { apiClient } from '@/lib/api-client';

type TabView = 'registered' | 'explorer';
type ViewMode = 'grid' | 'table';
type SortOption = 'name-asc' | 'name-desc' | 'readable';

export default function FoldersPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const tabParam = searchParams.get('tab') as TabView | null;
  const folderIdParam = searchParams.get('folderId');

  const [activeTab, setActiveTab] = useState<TabView>(() => {
    // Check URL param first, then localStorage, then default to 'registered'
    if (tabParam && ['registered', 'explorer'].includes(tabParam)) {
      return tabParam;
    }
    const saved = localStorage.getItem('pathsActiveTab');
    return (saved as TabView) || 'registered';
  });
  const [viewMode, setViewMode] = useState<ViewMode>(() => {
    // Default to table on mobile
    if (typeof window !== 'undefined' && window.innerWidth < 768) {
      return 'table';
    }
    return 'grid';
  });
  const [sortBy, setSortBy] = useState<SortOption>('name-asc');
  const [currentFolderId, setCurrentFolderId] = useState<number | null>(
    folderIdParam ? parseInt(folderIdParam, 10) : null
  );
  const [showHidden, setShowHidden] = useState(false);
  const [showFiles, setShowFiles] = useState(false);
  const [showRegisterModal, setShowRegisterModal] = useState(false);
  const [selectedFolderId, setSelectedFolderId] = useState<number | null>(null);
  const [selectedCollection, setSelectedCollection] = useState<string>('');
  const [previewPath, setPreviewPath] = useState<string | null>(null);
  const [devMode, setDevMode] = useState(false);

  const { data: homeFolder } = useHomeFolder();
  const { data: children, isLoading: childrenLoading } = useFolderChildren(currentFolderId || homeFolder?.id || 0, showFiles);
  const { data: parents } = useFolderParents(currentFolderId || homeFolder?.id || 0);
  const { data: currentFolder } = useFolder(currentFolderId || homeFolder?.id || 0);
  const { data: collections } = useCollections();
  const { data: paths, isLoading: pathsLoading } = usePaths();
  const registerFolder = useRegisterFolder();
  const scanPath = useScanPath();
  // Folder browser scans use the backend default algorithm (no per-scan picker here).
  const [selectedAlgorithm] = useState(DEFAULT_ALGORITHM);

  // Load dev mode from localStorage
  useEffect(() => {
    const savedDevMode = localStorage.getItem('devMode');
    if (savedDevMode) setDevMode(savedDevMode === 'true');
  }, []);

  // Initialize with home folder or URL param
  useEffect(() => {
    if (folderIdParam) {
      const parsedId = parseInt(folderIdParam, 10);
      if (!isNaN(parsedId)) {
        setCurrentFolderId(parsedId);
      }
    } else if (homeFolder && currentFolderId === null) {
      setCurrentFolderId(homeFolder.id);
    }
  }, [homeFolder, folderIdParam]);

  const handleFolderClick = (folderId: number) => {
    if (devMode) console.log('🖱️ Path clicked:', folderId);
    setCurrentFolderId(folderId);
  };

  const handleRegisterClick = (folderId: number) => {
    setSelectedFolderId(folderId);
    setShowRegisterModal(true);
    if (collections && collections.length > 0) {
      setSelectedCollection(collections[0].name);
    }
  };

  const handleRegisterConfirm = () => {
    if (selectedFolderId && selectedCollection) {
      registerFolder.mutate(
        { collectionName: selectedCollection, folderId: selectedFolderId },
        {
          onSuccess: () => {
            setShowRegisterModal(false);
            setSelectedFolderId(null);
            setSelectedCollection('');
          }
        }
      );
    }
  };

  const handleScanPath = (pathId: number) => {
    scanPath.mutate({ pathId, algorithm: selectedAlgorithm });
  };

  const handleTabChange = (tab: TabView) => {
    setActiveTab(tab);
    localStorage.setItem('pathsActiveTab', tab);
    const params: Record<string, string> = { tab };
    if (tab === 'explorer' && currentFolderId) {
      params.folderId = currentFolderId.toString();
    }
    setSearchParams(params);
  };

  const isPreviewable = (path: string) => {
    const lower = path.toLowerCase();
    return lower.endsWith('.png') || lower.endsWith('.jpg') || lower.endsWith('.jpeg') || lower.endsWith('.gif') || lower.endsWith('.webp') || lower.endsWith('.pdf') || lower.endsWith('.txt') || lower.endsWith('.md') || lower.endsWith('.json') || lower.endsWith('.xml') || lower.endsWith('.csv');
  };

  // Sort visible children
  const visibleChildren = (children || [])
    .filter((folder) => showHidden || !folder.isHidden)
    .sort((a, b) => {
      switch (sortBy) {
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

  // Build breadcrumbs: parents (reversed) + current folder
  const breadcrumbs = parents ? [...parents].reverse() : [];
  if (currentFolder && currentFolder.name !== '/') {
    breadcrumbs.push(currentFolder);
  }

  // Build full path from breadcrumbs
  const currentPath = breadcrumbs.length > 0
    ? breadcrumbs.map(p => p.name).filter(name => name !== '/').join('/').replace(/^/, '/')
    : '/';

  // Update URL when folder changes (only for explorer tab)
  useEffect(() => {
    if (activeTab === 'explorer' && currentFolderId !== null) {
      setSearchParams({
        tab: 'explorer',
        folderId: currentFolderId.toString(),
        path: currentPath
      });
    }
  }, [currentFolderId, currentPath, activeTab]);

  // Debug logging (only if dev mode enabled)
  if (devMode) {
    console.log('📊 Breadcrumb Debug:', {
      currentFolderId,
      homeFolder: homeFolder?.id,
      parents: parents?.map(p => ({ id: p.id, name: p.name })),
      currentFolder: currentFolder ? { id: currentFolder.id, name: currentFolder.name } : null,
      breadcrumbs: breadcrumbs.map(b => ({ id: b.id, name: b.name })),
      currentPath
    });
  }

  // Get registered paths (only those that are currently registered)
  const registeredPaths = (paths || []).filter(path =>
    path.registeredPaths && path.registeredPaths.length > 0
  );

  return (
    <div className="text-foreground p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold mb-2">Paths</h1>
        <p className="text-foreground/60">
          {activeTab === 'registered'
            ? 'See the paths that make up your collections.'
            : 'Browse and register paths to your collections.'}
        </p>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 mb-6 border-b border-foreground/10">
        <button
          onClick={() => handleTabChange('registered')}
          className={`px-4 py-2 font-medium transition-colors relative ${activeTab === 'registered'
              ? 'text-primary'
              : 'text-foreground/60 hover:text-foreground'
            }`}
        >
          Registered Paths
          {activeTab === 'registered' && (
            <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-primary" />
          )}
        </button>
        <button
          onClick={() => handleTabChange('explorer')}
          className={`px-4 py-2 font-medium transition-colors relative ${activeTab === 'explorer'
              ? 'text-primary'
              : 'text-foreground/60 hover:text-foreground'
            }`}
        >
          File Explorer
          {activeTab === 'explorer' && (
            <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-primary" />
          )}
        </button>
      </div>

      {/* Registered Paths Tab */}
      {activeTab === 'registered' && (
        <div>
          {pathsLoading ? (
            <div className="flex items-center gap-2 text-foreground/60">
              <RefreshCw className="w-5 h-5 animate-spin" />
              <span>Loading paths...</span>
            </div>
          ) : !registeredPaths || registeredPaths.length === 0 ? (
            <div className="bg-card border border-foreground/10 rounded-lg p-8 text-center">
              <Folder className="w-16 h-16 mx-auto mb-4 text-foreground/30" />
              <p className="text-foreground/60 mb-4">No registered paths yet</p>
              <button
                onClick={() => handleTabChange('explorer')}
                className="px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:opacity-90 transition-opacity"
              >
                Browse and Register Paths
              </button>
            </div>
          ) : (
            <div className="bg-card border border-foreground/10 rounded-lg overflow-hidden">
              <table className="w-full">
                <thead className="bg-primary/10">
                  <tr>
                    <th className="text-left p-4 font-semibold text-foreground">Path</th>
                    <th className="text-left p-4 font-semibold text-foreground">Added</th>
                    <th className="text-right p-4 font-semibold text-foreground">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {registeredPaths.map((path, index) => (
                    <tr
                      key={path.id}
                      className={`${index !== registeredPaths.length - 1 ? 'border-b border-foreground/10' : ''} hover:bg-foreground/5 transition-colors`}
                    >
                      <td className="p-4">
                        <Link
                          to={`/paths/${path.id}`}
                          className="text-accent hover:underline font-medium"
                        >
                          {path.root.replace('file://', '')}
                        </Link>
                      </td>
                      <td className="p-4 text-foreground/70">
                        {formatJavaDateTime(path.added)}
                      </td>
                      <td className="p-4 text-right">
                        <button
                          onClick={() => handleScanPath(path.id)}
                          disabled={scanPath.isPending}
                          className="inline-flex items-center gap-2 px-4 py-2 bg-accent text-accent-foreground rounded-lg hover:opacity-90 disabled:opacity-50 transition-opacity"
                        >
                          <Play className="w-4 h-4" />
                          <span>Scan Now</span>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* File Explorer Tab */}
      {activeTab === 'explorer' && (
        <div>
          <div className="flex justify-between items-center mb-6 gap-4 flex-wrap">
            <h2 className="text-xl font-semibold">Browse Filesystem</h2>
            <div className="flex items-center gap-2">
              {/* Sort Dropdown */}
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as SortOption)}
                className="px-3 py-2 bg-card border border-foreground/20 text-foreground rounded-lg hover:bg-foreground/5 transition-colors text-sm [&>option]:bg-card [&>option]:text-foreground"
              >
                <option value="name-asc">Name (A-Z)</option>
                <option value="name-desc">Name (Z-A)</option>
                <option value="readable">Readable First</option>
              </select>

              {/* View Mode Toggle */}
              <div className="flex bg-foreground/10 rounded-lg p-1">
                <button
                  onClick={() => setViewMode('grid')}
                  className={`p-2 rounded transition-colors ${viewMode === 'grid'
                      ? 'bg-primary text-primary-foreground'
                      : 'text-foreground/60 hover:text-foreground'
                    }`}
                  title="Grid view"
                >
                  <Grid3x3 className="w-4 h-4" />
                </button>
                <button
                  onClick={() => setViewMode('table')}
                  className={`p-2 rounded transition-colors ${viewMode === 'table'
                      ? 'bg-primary text-primary-foreground'
                      : 'text-foreground/60 hover:text-foreground'
                    }`}
                  title="Table view"
                >
                  <List className="w-4 h-4" />
                </button>
              </div>

              {/* Show/Hide Hidden Toggle */}
              <button
                onClick={() => setShowHidden(!showHidden)}
                className="flex items-center gap-2 px-4 py-2 bg-foreground/10 text-foreground rounded-lg hover:bg-foreground/20 transition-colors"
              >
                {showHidden ? <Eye className="w-4 h-4" /> : <EyeOff className="w-4 h-4" />}
                <span className="hidden sm:inline">{showHidden ? 'Hide Hidden' : 'Show Hidden'}</span>
              </button>

              <button
                onClick={() => setShowFiles(!showFiles)}
                className="flex items-center gap-2 px-4 py-2 bg-foreground/10 text-foreground rounded-lg hover:bg-foreground/20 transition-colors"
              >
                <FileText className="w-4 h-4" />
                <span className="hidden sm:inline">{showFiles ? 'Folders Only' : 'Show Files'}</span>
              </button>
            </div>
          </div>

          {/* Current Path Display */}
          <div className="bg-card border border-foreground/10 rounded-lg p-4 mb-4">
            <div className="text-sm text-foreground/60 mb-1">Current Path:</div>
            <div className="font-mono text-foreground">{currentPath}</div>
          </div>

          {/* Breadcrumb Navigation */}
          <div className="bg-card border border-foreground/10 rounded-lg p-4 mb-6">
            <div className="flex items-center justify-between gap-4">
              <div className="flex items-center gap-2 text-sm flex-wrap flex-1">
                <button
                  onClick={() => homeFolder && setCurrentFolderId(homeFolder.id)}
                  className="flex items-center gap-1 px-2 py-1 hover:bg-foreground/10 rounded transition-colors"
                >
                  <Home className="w-4 h-4" />
                  <span>Home</span>
                </button>

                {breadcrumbs.map((parent) => (
                  <div key={parent.id} className="flex items-center gap-2">
                    <ChevronRight className="w-4 h-4 text-foreground/30" />
                    <button
                      onClick={() => setCurrentFolderId(parent.id)}
                      className="px-2 py-1 hover:bg-foreground/10 rounded transition-colors"
                    >
                      {parent.name}
                    </button>
                  </div>
                ))}
              </div>

              {/* Register Current Folder Button */}
              {currentFolderId && (
                <button
                  onClick={() => handleRegisterClick(currentFolderId)}
                  className="flex items-center gap-1 px-3 py-1.5 bg-accent text-accent-foreground rounded hover:opacity-90 transition-opacity flex-shrink-0"
                  title="Register current folder to collection"
                >
                  <Plus className="w-4 h-4" />
                  <span className="text-sm">Register</span>
                </button>
              )}
            </div>
          </div>

          {/* Folder List */}
          {childrenLoading ? (
            <div className="flex items-center gap-2 text-foreground/60">
              <RefreshCw className="w-5 h-5 animate-spin" />
              <span>Loading folders...</span>
            </div>
          ) : !visibleChildren || visibleChildren.length === 0 ? (
            <div className="bg-card border border-foreground/10 rounded-lg p-8 text-center">
              <Folder className="w-16 h-16 mx-auto mb-4 text-foreground/30" />
              <p className="text-foreground/60">
                {children?.length === 0 ? 'No subpaths' : 'No visible paths (toggle "Show Hidden")'}
              </p>
            </div>
          ) : viewMode === 'grid' ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
              {visibleChildren.map((folder) => (
                <div
                  key={folder.id}
                  className="bg-card border border-foreground/10 rounded-lg p-4 hover:border-primary/50 transition-colors group"
                >
                  <div className="flex items-start justify-between gap-2 mb-3">
                    <button
                      onClick={() => (folder.isDir === false ? null : handleFolderClick(folder.id))}
                      disabled={!folder.isReadable || folder.isDir === false}
                      className="flex items-center gap-2 flex-1 text-left group-hover:text-primary transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {folder.isDir === false ? (
                        <FileText className="w-5 h-5 flex-shrink-0" />
                      ) : folder.isReadable ? (
                        <FolderOpen className="w-5 h-5 flex-shrink-0" />
                      ) : (
                        <Folder className="w-5 h-5 flex-shrink-0 text-foreground/30" />
                      )}
                      <span className="font-medium truncate" title={folder.name}>
                        {folder.name}
                      </span>
                    </button>

                    {folder.isReadable && folder.isDir !== false && (
                      <button
                        onClick={() => handleRegisterClick(folder.id)}
                        className="p-1.5 bg-accent text-accent-foreground rounded hover:opacity-90 transition-opacity flex-shrink-0"
                        title="Register folder to collection"
                      >
                        <Plus className="w-4 h-4" />
                      </button>
                    )}
                    {folder.isReadable && folder.isDir === false && folder.path && isPreviewable(folder.path) && (
                      <button
                        onClick={() => setPreviewPath(folder.path || null)}
                        className="p-1.5 bg-foreground/10 text-foreground rounded hover:bg-foreground/20 transition-colors flex-shrink-0"
                        title="Preview file"
                      >
                        <Eye className="w-4 h-4" />
                      </button>
                    )}
                  </div>

                  <div className="flex gap-2 text-xs">
                    {folder.isHidden && (
                      <span className="px-2 py-0.5 bg-foreground/10 text-foreground/70 rounded">
                        Hidden
                      </span>
                    )}
                    {!folder.isReadable && (
                      <span className="px-2 py-0.5 bg-red-500/10 text-red-600 dark:text-red-400 rounded">
                        No access
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="bg-card border border-foreground/10 rounded-lg overflow-hidden">
              <table className="w-full">
                <thead className="bg-primary/10">
                  <tr>
                    <th className="text-left p-4 font-semibold text-foreground">Name</th>
                    <th className="text-left p-4 font-semibold text-foreground hidden md:table-cell">Status</th>
                    <th className="text-right p-4 font-semibold text-foreground">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {visibleChildren.map((folder, index) => (
                    <tr
                      key={folder.id}
                      className={`${index !== visibleChildren.length - 1 ? 'border-b border-foreground/10' : ''} hover:bg-foreground/5 transition-colors`}
                    >
                      <td className="p-4">
                        <button
                          onClick={() => (folder.isDir === false ? null : handleFolderClick(folder.id))}
                          disabled={!folder.isReadable || folder.isDir === false}
                          className="flex items-center gap-2 text-left hover:text-primary transition-colors disabled:opacity-50 disabled:cursor-not-allowed w-full"
                        >
                          {folder.isDir === false ? (
                            <FileText className="w-5 h-5 flex-shrink-0" />
                          ) : folder.isReadable ? (
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
                            <span className="px-2 py-0.5 bg-red-500/10 text-red-600 dark:text-red-400 rounded">
                              No access
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="p-4 text-right">
                        {folder.isReadable && folder.isDir !== false && (
                          <button
                            onClick={() => handleRegisterClick(folder.id)}
                            className="inline-flex items-center gap-1 px-3 py-1.5 bg-accent text-accent-foreground rounded hover:opacity-90 transition-opacity"
                            title="Register folder to collection"
                          >
                            <Plus className="w-4 h-4" />
                            <span className="hidden sm:inline">Register</span>
                          </button>
                        )}
                        {folder.isReadable && folder.isDir === false && folder.path && isPreviewable(folder.path) && (
                          <button
                            onClick={() => setPreviewPath(folder.path || null)}
                            className="inline-flex items-center gap-1 px-3 py-1.5 bg-foreground/10 text-foreground rounded hover:bg-foreground/20 transition-colors"
                            title="Preview file"
                          >
                            <Eye className="w-4 h-4" />
                            <span className="hidden sm:inline">Preview</span>
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {previewPath && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4" onClick={() => setPreviewPath(null)}>
          <div className="bg-card border border-foreground/10 rounded-lg shadow-xl w-full max-w-4xl max-h-[90vh] overflow-hidden" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between p-4 border-b border-foreground/10">
              <h3 className="text-lg font-semibold text-foreground truncate">{previewPath}</h3>
              <button onClick={() => setPreviewPath(null)} className="p-2 hover:bg-foreground/10 rounded transition-colors">
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

      {/* Register to Collection Modal */}
      {showRegisterModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" onClick={() => setShowRegisterModal(false)}>
          <div className="bg-card border border-foreground/10 rounded-lg shadow-xl max-w-md w-full mx-4" onClick={(e) => e.stopPropagation()}>
            <div className="flex justify-between items-center p-4 border-b border-foreground/10">
              <h2 className="text-xl font-semibold text-foreground">Register Folder</h2>
              <button
                onClick={() => setShowRegisterModal(false)}
                className="p-1 hover:bg-foreground/10 rounded transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-4">
              <p className="text-foreground/70 mb-4">Select a collection to register this folder to:</p>

              {!collections || collections.length === 0 ? (
                <div className="text-foreground/60 text-sm">
                  No collections available. Create a collection first.
                </div>
              ) : (
                <div className="space-y-2">
                  {collections.map((collection) => (
                    <label
                      key={collection.id}
                      className="flex items-center gap-3 p-3 bg-background border border-foreground/10 rounded-lg cursor-pointer hover:border-primary/50 transition-colors"
                    >
                      <input
                        type="radio"
                        name="collection"
                        value={collection.name}
                        checked={selectedCollection === collection.name}
                        onChange={(e) => setSelectedCollection(e.target.value)}
                        className="w-4 h-4"
                      />
                      <span className="font-medium text-foreground">{collection.name}</span>
                    </label>
                  ))}
                </div>
              )}
            </div>

            <div className="flex justify-end gap-2 p-4 border-t border-foreground/10">
              <button
                onClick={() => setShowRegisterModal(false)}
                className="px-4 py-2 bg-foreground/10 text-foreground rounded-lg hover:bg-foreground/20 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleRegisterConfirm}
                disabled={!selectedCollection || registerFolder.isPending}
                className="px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:opacity-90 disabled:opacity-50 transition-opacity"
              >
                {registerFolder.isPending ? 'Registering...' : 'Register'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
