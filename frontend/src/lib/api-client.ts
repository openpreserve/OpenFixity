import type {
  Collection,
  CollectionPath,
  PathScan,
  JobDetail,
  ApiError,
  FolderInfo,
  AlgorithmInfo,
  SettingsMap,
  AppInfo,
  FileRecheckResult,
  ScanSchedule,
  ScheduleRequest,
} from '@/types/api';

const API_BASE = '/api';

class ApiClient {
  private async request<T>(endpoint: string, options?: RequestInit): Promise<T> {
    const response = await fetch(`${API_BASE}${endpoint}`, {
      headers: {
        'Content-Type': 'application/json',
        ...options?.headers,
      },
      ...options,
    });

    if (!response.ok) {
      const contentType = response.headers.get('content-type') || '';
      const error: ApiError = contentType.includes('application/json')
        ? await response.json().catch(() => ({
            message: `HTTP ${response.status}: ${response.statusText}`,
          }))
        : {
            message: (await response.text()) || `HTTP ${response.status}: ${response.statusText}`,
          };
      throw new Error(error.message || 'An error occurred');
    }

    if (response.status === 204) {
      return undefined as T;
    }

    const contentType = response.headers.get('content-type') || '';
    if (!contentType.includes('application/json')) {
      return undefined as T;
    }

    return response.json() as Promise<T>;
  }

  // Collections
  async getCollections(): Promise<Collection[]> {
    return this.request<Collection[]>('/collections');
  }

  async getCollection(name: string): Promise<Collection> {
    return this.request<Collection>(`/collections/${encodeURIComponent(name)}/`);
  }

  async createCollection(name: string): Promise<Collection> {
    return this.request<Collection>(`/collections/${encodeURIComponent(name)}/`, {
      method: 'POST',
    });
  }

  async scanCollection(name: string, _algorithm?: string): Promise<JobDetail[]> {
    // Java backend returns Set<JobDetail>, takes no request body for algorithm
    // Algorithm is specified per-path when scanning
    return this.request<JobDetail[]>(`/collections/${encodeURIComponent(name)}/scan`, {
      method: 'POST',
    });
  }

  async registerFolderToCollection(collectionName: string, folderId: number): Promise<Collection> {
    return this.request<Collection>(
      `/collections/${encodeURIComponent(collectionName)}/folder/${folderId}/`,
      { method: 'POST' }
    );
  }

  async deregisterPathFromCollection(collectionName: string, pathId: number): Promise<void> {
    // Java: DELETE /collections/{name}/paths/{folderId}/ returns PathRegistration but we ignore it
    return this.request<void>(
      `/collections/${encodeURIComponent(collectionName)}/paths/${pathId}/`,
      { method: 'DELETE' }
    );
  }

  async scanPathInCollection(collectionName: string, pathId: number, _algorithm?: string): Promise<JobDetail> {
    // Java: POST /collections/{name}/paths/{pathId}/scan
    return this.request<JobDetail>(
      `/collections/${encodeURIComponent(collectionName)}/paths/${pathId}/scan`,
      { method: 'POST' }
    );
  }

  // Paths
  async getPaths(): Promise<CollectionPath[]> {
    return this.request<CollectionPath[]>('/paths');
  }

  async getPath(pathId: number): Promise<CollectionPath> {
    return this.request<CollectionPath>(`/paths/${pathId}/`);
  }

  async createPath(folderId: number): Promise<CollectionPath> {
    return this.request<CollectionPath>(`/paths/${folderId}/`, {
      method: 'POST',
    });
  }

  async scanPath(pathId: number, algorithm: string): Promise<JobDetail> {
    // Java: POST /paths/{pathId}/scan/{algorithm}/
    // Special handling for SHA-512 variants: POST /paths/{pathId}/scan/SHA-512/{algorithm}/
    const encodedAlgorithm = encodeURIComponent(algorithm);
    if (algorithm.startsWith('SHA-512/')) {
      const variant = algorithm.replace('SHA-512/', '');
      return this.request<JobDetail>(`/paths/${pathId}/scan/SHA-512/${encodeURIComponent(variant)}/`, {
        method: 'POST',
      });
    }
    return this.request<JobDetail>(`/paths/${pathId}/scan/${encodedAlgorithm}/`, {
      method: 'POST',
    });
  }

  // Folders (file system browsing)
  async getFolderRoots(): Promise<FolderInfo[]> {
    return this.request<FolderInfo[]>('/folders');
  }

  async getHomeFolder(): Promise<FolderInfo> {
    return this.request<FolderInfo>('/folders/home');
  }

  async getFolder(folderId: number): Promise<FolderInfo> {
    return this.request<FolderInfo>(`/folders/${folderId}/`);
  }

  async getFolderChildren(folderId: number, _showFiles = false): Promise<FolderInfo[]> {
    // Java doesn't support showFiles parameter currently
    return this.request<FolderInfo[]>(`/folders/${folderId}/children`);
  }

  async getFolderParents(folderId: number): Promise<FolderInfo[]> {
    return this.request<FolderInfo[]>(`/folders/${folderId}/parents`);
  }

  async getDefaultRoot(): Promise<FolderInfo> {
    return this.request<FolderInfo>('/folders/roots/default');
  }

  async getAlgorithms(): Promise<AlgorithmInfo[]> {
    // Java backend uses /digests/algorithms/ and returns Set<Algorithms> enum
    // The enum has id, name, displayName properties
    const result = await this.request<any[]>('/digests/algorithms/');
    // Transform enum objects to AlgorithmInfo format
    return result.map((alg: any) => ({
      id: alg.id || alg.name || alg,
      displayName: alg.displayName || alg.name || alg
    }));
  }

  async getSettings(): Promise<SettingsMap> {
    // Java backend doesn't have /settings endpoint yet
    // Return empty settings for now
    return {};
  }

  async getAppInfo(): Promise<AppInfo> {
    return this.request<AppInfo>('/info');
  }

  async updateSettings(payload: SettingsMap): Promise<SettingsMap> {
    // Java backend doesn't have /settings endpoint yet
    return payload;
  }

  getFileUrl(path: string): string {
    // Java backend doesn't have file serving endpoint
    return `${API_BASE}/file?path=${encodeURIComponent(path)}`;
  }

  // Scans
  async getScans(): Promise<PathScan[]> {
    return this.request<PathScan[]>('/scans');
  }

  async clearScanCache(): Promise<void> {
    // Not applicable for Java backend - scans are persisted to database immediately
    return Promise.resolve();
  }

  async getScan(scanId: number): Promise<PathScan> {
    return this.request<PathScan>(`/scans/${scanId}/`);
  }

  async getPathScans(pathId: number): Promise<PathScan[]> {
    return this.request<PathScan[]>(`/paths/${pathId}/scans/`);
  }

  async recheckScanFile(_scanId: number, _path: string): Promise<FileRecheckResult> {
    // Java backend doesn't have file recheck endpoint yet
    throw new Error('File recheck not supported by Java backend');
  }

  // Scheduler - Java backend has limited scheduler API
  async pauseScheduler(): Promise<boolean> {
    return this.request<boolean>('/scheduler/pause', {
      method: 'POST',
    });
  }

  async resumeScheduler(): Promise<boolean> {
    return this.request<boolean>('/scheduler/resume', {
      method: 'POST',
    });
  }

  async isSchedulerRunning(): Promise<boolean> {
    return this.request<boolean>('/scheduler/isRunning');
  }

  async isSchedulerPaused(): Promise<boolean> {
    return this.request<boolean>('/scheduler/isPaused');
  }

  async getSchedulerStatus(): Promise<any> {
    // Java backend doesn't have /scheduler endpoint, simulate it with isRunning/isPaused
    try {
      const [isRunning, isPaused] = await Promise.all([
        this.isSchedulerRunning(),
        this.isSchedulerPaused()
      ]);
      return {
        // Standby (paused) reports isRunning=false, so check paused first or it reads "Stopped".
        status: isPaused ? 'Paused' : (isRunning ? 'Running' : 'Stopped'),
        running: isRunning,
        paused: isPaused,
        jobCount: 0 // Java backend doesn't provide this endpoint
      };
    } catch (error) {
      // Fallback if endpoints don't exist
      return {
        status: 'Unknown',
        running: false,
        paused: false,
        jobCount: 0
      };
    }
  }

  async getJobDetails(): Promise<JobDetail[]> {
    // Java backend doesn't have /scheduler/jobs endpoint
    // Return empty array to prevent errors
    return [];
  }

  // Recurring scan schedules (backed by /api/schedules on the Java server).
  async listSchedules(): Promise<ScanSchedule[]> {
    return this.request<ScanSchedule[]>('/schedules');
  }

  async createSchedule(request: ScheduleRequest): Promise<ScanSchedule> {
    return this.request<ScanSchedule>('/schedules', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async deleteSchedule(id: number): Promise<void> {
    return this.request<void>(`/schedules/${id}/`, { method: 'DELETE' });
  }
}

export const apiClient = new ApiClient();
