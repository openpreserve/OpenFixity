import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useCollections, useCreateCollection, useScanCollection } from '@/hooks/api';
import { formatJavaDateTime } from '@/types/api';
import { Plus, RefreshCw, Folder, Play, ChevronRight } from 'lucide-react';
import { useTutorial } from '@/lib/tutorial';

export default function CollectionsPage() {
  const tutorial = useTutorial();
  useEffect(() => {
    document.title = 'Collections | OpenFixity';
  }, []);

  const { data: collections, isLoading, error } = useCollections();
  const createCollection = useCreateCollection();
  const scanCollection = useScanCollection();
  const [newCollectionName, setNewCollectionName] = useState('');
  const [showCreateForm, setShowCreateForm] = useState(false);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCollectionName.trim()) return;

    const createdName = newCollectionName.trim();

    await createCollection.mutateAsync(createdName);

    if (tutorial.enabled && tutorial.status === 'running') {
      tutorial.completeStep('createCollection', { collectionName: createdName });
    }

    setNewCollectionName('');
    setShowCreateForm(false);
  };

  const handleScan = (name: string) => {
    // Collection scans run the backend default algorithm (the API has no per-collection override).
    scanCollection.mutate({ name });
  };

  if (isLoading) {
    return (
      <div className="text-foreground p-6">
        <div className="flex items-center gap-2">
          <RefreshCw className="w-5 h-5 animate-spin" />
          <span>Loading collections...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-foreground p-6">
        <div className="bg-red-500/10 border border-red-500/50 rounded-lg p-4">
          <p className="text-red-600 dark:text-red-400">Error: {error.message}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="text-foreground p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold">Collections</h1>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowCreateForm(!showCreateForm)}
            className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:opacity-90 transition-opacity"
          >
            <Plus className="w-4 h-4" />
            New Collection
          </button>
        </div>
      </div>

      {tutorial.enabled && tutorial.status === 'running' && tutorial.currentStep === 'createCollection' && (
        <div className="bg-primary/10 border border-primary/30 rounded-lg p-4 mb-6">
          <p className="text-sm text-foreground">
            Tutorial: Create your first collection using <strong>New Collection</strong>.
          </p>
        </div>
      )}

      {showCreateForm && (
        <div className="bg-card border border-foreground/10 rounded-lg p-4 mb-6">
          <h2 className="text-lg font-semibold mb-3">Create New Collection</h2>
          <form onSubmit={handleCreate} className="flex gap-2">
            <input
              type="text"
              value={newCollectionName}
              onChange={(e) => setNewCollectionName(e.target.value)}
              placeholder="Collection name"
              className="flex-1 px-3 py-2 bg-background border border-foreground/20 rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
              autoFocus
            />
            <button
              type="submit"
              disabled={createCollection.isPending || !newCollectionName.trim()}
              className="px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:opacity-90 disabled:opacity-50 transition-opacity"
            >
              {createCollection.isPending ? 'Creating...' : 'Create'}
            </button>
            <button
              type="button"
              onClick={() => {
                setShowCreateForm(false);
                setNewCollectionName('');
              }}
              className="px-4 py-2 bg-foreground/10 text-foreground rounded-lg hover:bg-foreground/20 transition-colors"
            >
              Cancel
            </button>
          </form>
        </div>
      )}

      {!collections || collections.length === 0 ? (
        <div className="bg-card border border-foreground/10 rounded-lg p-8 text-center">
          <Folder className="w-16 h-16 mx-auto mb-4 text-foreground/30" />
          <p className="text-foreground/60 mb-4">No collections yet</p>
          <button
            onClick={() => setShowCreateForm(true)}
            className="px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:opacity-90 transition-opacity"
          >
            Create your first collection
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {collections.map((collection) => (
            <div
              key={collection.id}
              className="bg-card border border-foreground/10 rounded-lg hover:border-primary/50 transition-colors group"
            >
              <Link
                to={`/collections/${encodeURIComponent(collection.name)}`}
                className="block p-5"
              >
                <div className="flex justify-between items-start mb-3">
                  <div className="flex items-center gap-2 flex-1">
                    <h3 className="text-lg font-semibold text-foreground group-hover:text-primary transition-colors">
                      {collection.name}
                    </h3>
                    <ChevronRight className="w-4 h-4 text-foreground/30 group-hover:text-primary transition-colors" />
                  </div>
                  <button
                    onClick={(e) => {
                      e.preventDefault();
                      handleScan(collection.name);
                    }}
                    disabled={scanCollection.isPending}
                    className="p-2 bg-accent text-accent-foreground rounded hover:opacity-90 disabled:opacity-50 transition-opacity flex-shrink-0"
                    title="Start scan"
                  >
                    <Play className="w-4 h-4" />
                  </button>
                </div>

                <div className="space-y-2 text-sm text-foreground/70">
                  <div className="flex justify-between">
                    <span>ID:</span>
                    <span className="font-mono">{collection.id}</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Created:</span>
                    <span className="font-mono text-xs">{formatJavaDateTime(collection.created)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Registered Paths:</span>
                    <span className="font-semibold text-foreground">{collection.registeredPaths?.length || 0}</span>
                  </div>
                </div>
              </Link>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

