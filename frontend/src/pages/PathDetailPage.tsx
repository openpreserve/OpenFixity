import { useParams, Link } from 'react-router-dom';
import { usePath, useCollections, useScanPath, useDeregisterPath, useAlgorithms, usePathScans } from '@/hooks/api';
import { Play, RefreshCw, X } from 'lucide-react';
import { formatJavaDateTime, type PathScan } from '@/types/api';
import { defaultAlgorithm, AlgorithmHint } from '@/lib/algorithm';
import { SchedulePanel } from '@/components/SchedulePanel';
import { useState, useEffect } from 'react';

export default function PathDetailPage() {
  const { id } = useParams<{ id: string }>();
  const pathId = id ? parseInt(id, 10) : 0;
  
  const { data: path, isLoading: pathLoading, error: pathError } = usePath(pathId);
  const { data: pathScans } = usePathScans(pathId);
  const { data: collections, error: collectionsError } = useCollections();
  const { data: algorithms } = useAlgorithms();
  const scanPath = useScanPath();
  const deregisterPath = useDeregisterPath();
  const [selectedAlgorithm, setSelectedAlgorithm] = useState('');
  const [showDeregisterModal, setShowDeregisterModal] = useState(false);
  const [selectedCollection, setSelectedCollection] = useState<string | null>(null);
  
  useEffect(() => {
    if (path) {
      document.title = `${path.root.replace('file://', '')} - Path | OpenFixity`;
    } else {
      document.title = 'Path Details | OpenFixity';
    }
  }, [path]);

  // Default the picker to the backend default algorithm once the list loads.
  useEffect(() => {
    if (!selectedAlgorithm && algorithms?.length) {
      setSelectedAlgorithm(defaultAlgorithm(algorithms));
    }
  }, [algorithms, selectedAlgorithm]);

  // Set algorithm to match the most recent scan for this path (for apples-to-apples rescans)
  useEffect(() => {
    if (pathScans && Array.isArray(pathScans) && pathScans.length > 0) {
      // Find most recent completed scan
      const completedScans = pathScans.filter((scan: PathScan) => scan.status === 'COMPLETED');
      if (completedScans.length > 0) {
        const mostRecent = completedScans.reduce((latest: PathScan, current: PathScan) => {
          return current.id > latest.id ? current : latest;
        }, completedScans[0]);
        
        // Get algorithm from first file in scan
        if (mostRecent.allFiles && mostRecent.allFiles.length > 0) {
          const firstFile = mostRecent.allFiles[0];
          if (firstFile.digestResults && firstFile.digestResults.length > 0) {
            const scanAlgorithm = firstFile.digestResults[0].algorithm;
            if (scanAlgorithm) {
              setSelectedAlgorithm(scanAlgorithm);
            }
          }
        }
      }
    }
  }, [pathScans]);
  

  if (pathLoading) {
    return (
      <div className="text-foreground p-6 flex items-center gap-2">
        <RefreshCw className="w-5 h-5 animate-spin" />
        <span>Loading path...</span>
      </div>
    );
  }

  if (pathError || !path) {
    return (
      <div className="text-foreground p-6">
        <p>Path not found</p>
        <Link to="/paths" className="text-accent hover:underline">
          Back to Paths
        </Link>
      </div>
    );
  }

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

  // Match path registrations to collections via timestamps
  // This is a workaround since the backend doesn't include collection info in path.registeredPaths
  const pathCollections = collectionsError ? [] : (collections || []).filter(collection => 
    collection.pathRegistrations?.some(collectionReg =>
      path.registeredPaths?.some(pathReg =>
        timestampsMatch(pathReg.registeredAt, collectionReg.registeredAt)
      )
    )
  ).map(collection => {
    // Find the matching registration to get the timestamp
    const matchingReg = path.registeredPaths?.find(pathReg =>
      collection.pathRegistrations.some(collectionReg =>
        timestampsMatch(pathReg.registeredAt, collectionReg.registeredAt)
      )
    );
    return {
      collection,
      registeredAt: matchingReg?.registeredAt
    };
  });

  const handleScan = () => {
    scanPath.mutate({ pathId, algorithm: selectedAlgorithm });
  };

  const handleDeregisterClick = (collectionName: string) => {
    setSelectedCollection(collectionName);
    setShowDeregisterModal(true);
  };

  const handleDeregisterConfirm = () => {
    if (selectedCollection) {
      deregisterPath.mutate({ collectionName: selectedCollection, pathId });
    }
    setShowDeregisterModal(false);
    setSelectedCollection(null);
  };

  return (
    <div className="text-foreground p-6">
      <h1 className="text-3xl font-bold text-accent mb-2">
        {path.root.replace('file://', '')}
      </h1>
      <p className="text-foreground/60 mb-6">
        Scan this path and check on collections it's registered to and scheduling.
      </p>

      <div className="mb-6">
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
            onClick={handleScan}
            disabled={scanPath.isPending}
            className="inline-flex items-center gap-2 px-4 py-2 bg-accent text-accent-foreground rounded-lg hover:opacity-90 disabled:opacity-50 transition-opacity"
          >
            <Play className="w-4 h-4" />
            <span>Scan Now</span>
          </button>
        </div>
        <AlgorithmHint className="mt-2" />
      </div>

      <div className="mb-6">
        <p className="text-foreground/70">Schedule:</p>
      </div>

      <h2 className="text-2xl font-semibold text-accent mb-4">Path Registrations</h2>

      {pathCollections.length === 0 ? (
        <div className="bg-card border border-foreground/10 rounded-lg p-8 text-center">
          <p className="text-foreground/60 mb-4">
            This path is not registered to any collections
          </p>
          <Link
            to="/collections"
            className="inline-block px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:opacity-90 transition-opacity"
          >
            View Collections
          </Link>
        </div>
      ) : (
        <div className="bg-card border border-foreground/10 rounded-lg overflow-hidden">
          <table className="w-full">
            <thead className="bg-primary/10">
              <tr>
                <th className="text-left p-4 font-semibold text-foreground">Collection</th>
                <th className="text-left p-4 font-semibold text-foreground">Path</th>
                <th className="text-left p-4 font-semibold text-foreground">Registered</th>
                <th className="text-right p-4 font-semibold text-foreground">Scan</th>
              </tr>
            </thead>
            <tbody>
              {pathCollections.map(({ collection, registeredAt }, index) => (
                <tr 
                  key={collection.id}
                  className={`${index !== pathCollections.length - 1 ? 'border-b border-foreground/10' : ''} hover:bg-foreground/5 transition-colors`}
                >
                  <td className="p-4">
                    <Link 
                      to={`/collections/${encodeURIComponent(collection.name)}`}
                      className="text-accent hover:underline font-medium"
                    >
                      {collection.name}
                    </Link>
                  </td>
                  <td className="p-4">
                    <Link 
                      to={`/paths/${path.id}`}
                      className="text-accent hover:underline"
                    >
                      {path.root.replace('file://', '')}
                    </Link>
                  </td>
                  <td className="p-4 text-foreground/70">
                    {registeredAt ? formatJavaDateTime(registeredAt) : '-'}
                  </td>
                  <td className="p-4 text-right">
                    <button
                      onClick={() => handleDeregisterClick(collection.name)}
                      className="px-4 py-2 bg-red-500/10 text-red-600 dark:text-red-400 rounded-lg hover:bg-red-500/20 transition-colors"
                    >
                      Deregister
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Recurring scan schedules for this path */}
      <SchedulePanel pathId={path.id} algorithm={selectedAlgorithm || 'SHA-256'} />

      {/* Deregister Modal */}
      {showDeregisterModal && selectedCollection && (
        <div 
          className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" 
          onClick={() => setShowDeregisterModal(false)}
        >
          <div 
            className="bg-card border border-foreground/10 rounded-lg shadow-xl max-w-md w-full mx-4" 
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex justify-between items-center p-4 border-b border-foreground/10">
              <h2 className="text-xl font-semibold text-foreground">Confirm Deregister</h2>
              <button
                onClick={() => setShowDeregisterModal(false)}
                className="p-1 hover:bg-foreground/10 rounded transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            
            <div className="p-4">
              <p className="text-foreground/70 mb-4">
                Are you sure you want to deregister this path from collection "{selectedCollection}"?
              </p>
            </div>
            
            <div className="flex justify-end gap-2 p-4 border-t border-foreground/10">
              <button
                onClick={() => setShowDeregisterModal(false)}
                className="px-4 py-2 bg-foreground/10 text-foreground rounded-lg hover:bg-foreground/20 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleDeregisterConfirm}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
              >
                Deregister
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
