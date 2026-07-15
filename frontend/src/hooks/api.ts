import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { toast } from 'sonner';
import type { ScheduleRequest } from '@/types/api';

// Query keys
export const queryKeys = {
  collections: ['collections'] as const,
  collection: (name: string) => ['collections', name] as const,
  paths: ['paths'] as const,
  path: (id: number) => ['paths', id] as const,
  folders: ['folders'] as const,
  scans: ['scans'] as const,
  scan: (id: number) => ['scans', id] as const,
  scheduler: ['scheduler'] as const,
  jobs: ['scheduler', 'jobs'] as const,
  schedules: ['schedules'] as const,
  algorithms: ['algorithms'] as const,
  settings: ['settings'] as const,
  appInfo: ['appInfo'] as const,
};

// Collections
export function useCollections() {
  return useQuery({
    queryKey: queryKeys.collections,
    queryFn: () => apiClient.getCollections(),
  });
}

export function useCollection(name: string) {
  return useQuery({
    queryKey: queryKeys.collection(name),
    queryFn: () => apiClient.getCollection(name),
    enabled: !!name,
  });
}

export function useCreateCollection() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (name: string) => apiClient.createCollection(name),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.collections });
      toast.success('Collection created successfully');
    },
    onError: (error: Error) => {
      toast.error(`Failed to create collection: ${error.message}`);
    },
  });
}

export function useScanCollection() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ name, algorithm }: { name: string; algorithm?: string }) => {
      return await apiClient.scanCollection(name, algorithm);
    },
    onSuccess: (_, { name }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.collection(name) });
      queryClient.invalidateQueries({ queryKey: queryKeys.scans });
      queryClient.invalidateQueries({ queryKey: queryKeys.paths });
      toast.success('Collection scan started');
    },
    onError: (error: Error) => {
      toast.error(`Failed to start scan: ${error.message}`);
    },
  });
}

export function useRegisterFolder() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ collectionName, folderId }: { collectionName: string; folderId: number }) =>
      apiClient.registerFolderToCollection(collectionName, folderId),
    onSuccess: (_, { collectionName }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.collection(collectionName) });
      queryClient.invalidateQueries({ queryKey: queryKeys.collections });
      queryClient.invalidateQueries({ queryKey: queryKeys.paths });
      toast.success('Path registered to collection');
    },
    onError: (error: Error) => {
      toast.error(`Failed to register path: ${error.message}`);
    },
  });
}

export function useDeregisterPath() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ collectionName, pathId }: { collectionName: string; pathId: number }) =>
      apiClient.deregisterPathFromCollection(collectionName, pathId),
    onSuccess: (_, { collectionName }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.collection(collectionName) });
      queryClient.invalidateQueries({ queryKey: queryKeys.collections });
      queryClient.invalidateQueries({ queryKey: queryKeys.paths });
      toast.success('Path deregistered from collection');
    },
    onError: (error: Error) => {
      toast.error(`Failed to deregister path: ${error.message}`);
    },
  });
}

export function useScanPathInCollection() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ collectionName, pathId, algorithm }: { collectionName: string; pathId: number; algorithm?: string }) => {
      return await apiClient.scanPathInCollection(collectionName, pathId, algorithm);
    },
    onSuccess: (_, { collectionName }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.collection(collectionName) });
      queryClient.invalidateQueries({ queryKey: queryKeys.scans });
      queryClient.invalidateQueries({ queryKey: queryKeys.paths });
      toast.success('Path scan started');
    },
    onError: (error: Error) => {
      toast.error(`Failed to start path scan: ${error.message}`);
    },
  });
}

// Paths
export function usePaths() {
  return useQuery({
    queryKey: queryKeys.paths,
    queryFn: () => apiClient.getPaths(),
  });
}

export function usePath(id: number) {
  return useQuery({
    queryKey: queryKeys.path(id),
    queryFn: () => apiClient.getPath(id),
    enabled: id > 0,
  });
}

export function useCreatePath() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (folderId: number) => apiClient.createPath(folderId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.paths });
      toast.success('Path created successfully');
    },
    onError: (error: Error) => {
      toast.error(`Failed to create path: ${error.message}`);
    },
  });
}

export function useScanPath() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ pathId, algorithm }: { pathId: number; algorithm: string }) => {
      return await apiClient.scanPath(pathId, algorithm);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.scans });
      queryClient.invalidateQueries({ queryKey: queryKeys.paths });
      toast.success('Path scan started');
    },
    onError: (error: Error) => {
      toast.error(`Failed to start path scan: ${error.message}`);
    },
  });
}

// Folders
export function useFolderRoots() {
  return useQuery({
    queryKey: queryKeys.folders,
    queryFn: () => apiClient.getFolderRoots(),
  });
}

export function useHomeFolder() {
  return useQuery({
    queryKey: ['folders', 'home'],
    queryFn: () => apiClient.getHomeFolder(),
  });
}

export function useFolder(folderId: number) {
  return useQuery({
    queryKey: ['folders', folderId],
    queryFn: () => apiClient.getFolder(folderId),
    enabled: folderId !== 0,
  });
}

export function useFolderChildren(folderId: number, showFiles = false) {
  return useQuery({
    queryKey: ['folders', folderId, 'children', showFiles],
    queryFn: () => apiClient.getFolderChildren(folderId, showFiles),
    enabled: folderId !== 0,
  });
}

export function useFolderParents(folderId: number) {
  return useQuery({
    queryKey: ['folders', folderId, 'parents'],
    queryFn: () => apiClient.getFolderParents(folderId),
    enabled: folderId !== 0,
  });
}

export function useDefaultRoot() {
  return useQuery({
    queryKey: ['folders', 'default-root'],
    queryFn: () => apiClient.getDefaultRoot(),
  });
}

// Scans
export function useScans() {
  return useQuery({
    queryKey: queryKeys.scans,
    queryFn: () => apiClient.getScans(),
  });
}

export function usePathScans(pathId: number) {
  return useQuery({
    queryKey: ['path-scans', pathId],
    queryFn: () => apiClient.getPathScans(pathId),
    enabled: pathId > 0,
    retry: false,
  });
}

export function useScan(id: number) {
  return useQuery({
    queryKey: queryKeys.scan(id),
    queryFn: () => apiClient.getScan(id),
    enabled: id > 0,
    retry: false,
  });
}

export function useRecheckScanFile() {
  return useMutation({
    mutationFn: ({ scanId, path }: { scanId: number; path: string }) => apiClient.recheckScanFile(scanId, path),
  });
}

export function usePauseScheduler() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => apiClient.pauseScheduler(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.scheduler });
      toast.success('Scheduler paused');
    },
    onError: (error: Error) => {
      toast.error(`Failed to pause scheduler: ${error.message}`);
    },
  });
}

export function useResumeScheduler() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => apiClient.resumeScheduler(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.scheduler });
      toast.success('Scheduler resumed');
    },
    onError: (error: Error) => {
      toast.error(`Failed to resume scheduler: ${error.message}`);
    },
  });
}

export function useSchedulerStatus() {
  return useQuery({
    queryKey: queryKeys.scheduler,
    queryFn: () => apiClient.getSchedulerStatus(),
  });
}

export function useJobs() {
  return useQuery({
    queryKey: queryKeys.jobs,
    queryFn: () => apiClient.getJobDetails(),
  });
}

export function useScheduleJob() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (jobData: any) => apiClient.scheduleJob(jobData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.jobs });
      toast.success('Job scheduled successfully');
    },
    onError: (error: Error) => {
      toast.error(`Failed to schedule job: ${error.message}`);
    },
  });
}

export function useStopScheduledJob() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (triggerKey: string) => apiClient.stopScheduledJob(triggerKey),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.jobs });
      queryClient.invalidateQueries({ queryKey: queryKeys.scheduler });
      toast.success('Job stopped');
    },
    onError: (error: Error) => {
      toast.error(`Failed to stop job: ${error.message}`);
    },
  });
}

export function useStartScheduledJob() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (triggerKey: string) => apiClient.startScheduledJob(triggerKey),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.jobs });
      queryClient.invalidateQueries({ queryKey: queryKeys.scheduler });
      toast.success('Job started');
    },
    onError: (error: Error) => {
      toast.error(`Failed to start job: ${error.message}`);
    },
  });
}

export function useDeleteScheduledJob() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (triggerKey: string) => apiClient.deleteScheduledJob(triggerKey),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.jobs });
      queryClient.invalidateQueries({ queryKey: queryKeys.scheduler });
      toast.success('Job deleted');
    },
    onError: (error: Error) => {
      toast.error(`Failed to delete job: ${error.message}`);
    },
  });
}

export function useAlgorithms() {
  return useQuery({
    queryKey: queryKeys.algorithms,
    queryFn: () => apiClient.getAlgorithms(),
  });
}

export function useSettings() {
  return useQuery({
    queryKey: queryKeys.settings,
    queryFn: () => apiClient.getSettings(),
  });
}

export function useUpdateSettings() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: Record<string, string>) => apiClient.updateSettings(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.settings });
      toast.success('Settings saved');
    },
    onError: (error: Error) => {
      toast.error(`Failed to save settings: ${error.message}`);
    },
  });
}

export function useAppInfo() {
  return useQuery({
    queryKey: queryKeys.appInfo,
    queryFn: () => apiClient.getAppInfo(),
    staleTime: 5 * 60 * 1000,
  });
}

// Recurring scan schedules
export function useSchedules() {
  return useQuery({
    queryKey: queryKeys.schedules,
    queryFn: () => apiClient.listSchedules(),
  });
}

export function useCreateSchedule() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: ScheduleRequest) => apiClient.createSchedule(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.schedules });
      toast.success('Schedule created');
    },
    onError: (error: Error) => {
      toast.error(`Failed to create schedule: ${error.message}`);
    },
  });
}

export function useDeleteSchedule() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => apiClient.deleteSchedule(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.schedules });
      toast.success('Schedule removed');
    },
    onError: (error: Error) => {
      toast.error(`Failed to remove schedule: ${error.message}`);
    },
  });
}
