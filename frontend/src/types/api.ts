// API Types for OpenFixity Backend (Java)

// DateTime represented as array: [year, month, day, hour, minute, second, nano]
export type JavaLocalDateTime = [number, number, number, number, number, number, number];
// Date represented as array: [year, month, day]
export type JavaLocalDate = [number, number, number];

export interface CollectionPath {
  id: number;
  root: string;
  added: JavaLocalDate;
  name: string;
  fullPath: string;
  jobId: string;
  pathRegistrations?: PathRegistration[];
  pathScans?: PathScan[];
  registeredPaths?: PathRegistration[];
  deRegisteredPaths?: PathRegistration[];
}

export interface PathRegistration {
  id?: number;
  collectionPath?: CollectionPath;
  collection?: Collection;
  registeredAt: JavaLocalDateTime;
  deregisteredAt: JavaLocalDateTime | null;
  registered?: boolean;
  deRegistered?: boolean;
}

export interface Collection {
  id: number;
  name: string;
  created: JavaLocalDateTime;
  pathRegistrations: PathRegistration[];
  pathRegistrationsSize: number;
  registeredPaths: PathRegistration[];
  deRegisteredPaths: PathRegistration[];
  jobId: string;
}

export type ScanStatus = 'INITIALISED' | 'STARTED' | 'COMPLETED' | 'FAILED';

export type PathAuditStatus = 
  | 'DAMAGED'
  | 'DENIED'
  | 'IGNORED'
  | 'NOTFOUND'
  | 'ADDED'
  | 'CHANGED'
  | 'VERIFIED'
  | 'UNVERIFIED';

export type FileScanStatus = 'DAMAGED' | 'DENIED' | 'IGNORED' | 'NOTFOUND' | 'SCANNED';

export interface DigestResult {
  algorithm: string;
  hexString?: string;  // Full hex digest (the correct field to use)
  value?: string;
  hex?: string;
  digestBytes?: string;  // base64 encoded digest (deprecated, use hexString)
  messageLength?: number;
  shortenedDigest?: string;
  digestLength?: number;
}

export interface FileScanResult {
  path: string;  // Full file:// URI
  auditStatus: PathAuditStatus;
  length: number;
  created: JavaLocalDateTime;
  modified: JavaLocalDateTime;
  scanned: JavaLocalDateTime;
  status: FileScanStatus;
  digestResults: DigestResult[];
  digestRecord?: {
    algorithm: string;
    shortenedDigest: string;
    digestLength: number;
  };
}

export interface PathSummary {
  totalFiles: number;
  totalBytes: number;
  formattedTotalBytes: string;
  path?: string;  // The path URI that was scanned
}

export interface PathScan {
  id: number;
  collectionPath?: {
    id: number;
    root: string;
    name: string;
    fullPath: string;
  };
  status: ScanStatus;
  started: JavaLocalDateTime;
  stopped?: JavaLocalDateTime | null;
  duration: string; // e.g., "135s"
  summary: PathSummary;
  summaryRecord?: PathSummary;  // Java backend also includes this field
  damagedCount: number;
  deniedCount: number;
  resultCount: number;
  addedCount?: number;
  changedCount?: number;
  verifiedCount?: number;
  notFoundCount?: number;
  ignoredCount?: number;
  unverifiedCount?: number;
  results?: FileScanResult[];
  allFiles?: FileScanResult[];  // Java backend returns all files in scan
  damaged?: boolean;  // Java backend includes these boolean flags
  denied?: boolean;
  completed?: boolean;
  damagedResults?: FileScanResult[];
  deniedResults?: FileScanResult[];
}

// Extended scan with path information (for flattened view)
export interface PathScanWithPath extends PathScan {
  pathId: number;
  pathName: string;
  pathRoot: string;
}

export interface CollectionPath {
  id: number;
  root: string;
  added: JavaLocalDate;
  name: string;
  fullPath: string;
  jobId: string;
  pathRegistrations?: PathRegistration[];
  pathScans?: PathScan[];
  registeredPaths?: PathRegistration[];
  deRegisteredPaths?: PathRegistration[];
}

// Helper to convert Java LocalDate to JS Date
export function javaLocalDateToDate(dt: JavaLocalDate | null): Date | null {
  if (!dt) return null;
  const [year, month, day] = dt;
  return new Date(year, month - 1, day);
}

// Helper to convert Java LocalDateTime to JS Date.
// The backend records times with LocalDateTime.now() — server-LOCAL wall-clock, no timezone —
// and serializes them as bare [y,m,d,h,m,s,nano] components. So interpret them in the viewer's
// local timezone (which, for the desktop app, is the same machine as the server). Do NOT use
// Date.UTC here: that treats local wall-clock as UTC and shifts every time by the UTC offset
// (e.g. "in about 2 hours" in the future at UTC+2).
export function javaDateTimeToDate(dt: JavaLocalDateTime | null): Date | null {
  if (!dt) return null;
  const [year, month, day, hour = 0, minute = 0, second = 0, nano = 0] = dt;
  return new Date(year, month - 1, day, hour, minute, second, Math.floor(nano / 1000000));
}

// Helper to format Java LocalDateTime
export function formatJavaDateTime(dt: JavaLocalDateTime | JavaLocalDate | number[] | null): string {
  if (!dt) return 'N/A';
  // If only date parts provided (length 3), use LocalDate formatter
  if (Array.isArray(dt) && dt.length === 3) {
    const date = javaLocalDateToDate(dt as JavaLocalDate);
    if (!date || isNaN(date.getTime())) return 'N/A';
    return date.toLocaleDateString();
  }
  // Otherwise use LocalDateTime formatter
  const date = javaDateTimeToDate(dt as JavaLocalDateTime);
  if (!date || isNaN(date.getTime())) return 'N/A';
  return date.toLocaleString();
}

export interface FolderInfo {
  id: number;
  hasParent: boolean;
  parentId: number;
  name: string;
  path?: string;
  isDir?: boolean;
  isReadable: boolean;
  isHidden: boolean;
}

export interface AlgorithmInfo {
  id: string;
  displayName: string;
}

export interface SettingsMap {
  [key: string]: string;
}

export interface AppInfo {
  appName: string;
  version: string;
  javaVersion: string;
  javaVendor: string;
  osName: string;
  osArch: string;
  osVersion: string;
  dropwizardVersion: string;
  uptimeMillis: number;
}

export interface FileRecheckComparison {
  algorithm: string;
  storedDigest: string;
  currentDigest?: string;
  matched: boolean;
  error?: string;
}

export interface FileRecheckResult {
  scanId: number;
  path: string;
  absolutePath: string;
  status: string;
  matchedAll: boolean;
  comparisons: FileRecheckComparison[];
}

export interface ScanRequest {
  algorithm?: string;
  algorithms?: string[];
}

export interface JobDetail {
  jobId: string;
  triggerKey: string;
  nextFireTime: JavaLocalDateTime | null;
  previousFireTime: JavaLocalDateTime | null;
  status: string;
  targetType?: 'path' | 'collection';
  pathId?: number | null;
  pathName?: string;
  pathRoot?: string;
  collectionName?: string;
  algorithm?: string;
  cron?: string;
  everySeconds?: number;
  enabled?: boolean;
}

export interface ApiError {
  message: string;
  path?: string;
  timestamp?: string;
}

export type Frequency = 'HOURLY' | 'DAILY' | 'WEEKLY';

/** A persisted recurring scan schedule, as returned by GET /api/schedules. */
export interface ScanSchedule {
  id: number;
  pathId: number | null;
  pathName: string;
  pathRoot: string;
  frequency: Frequency;
  minute: number;
  hour: number;
  dayOfWeek: number;   // 1 (Sun) to 7 (Sat)
  algorithm: string;
  cron: string;        // the derived Quartz cron expression
  description: string; // human sentence built by the server (e.g. "Every day at 02:00")
  enabled: boolean;
  created: JavaLocalDateTime;
}

/** Body for POST /api/schedules. Supply a preset (frequency + time) or a raw cron. */
export interface ScheduleRequest {
  pathId: number;
  frequency?: Frequency;
  minute?: number;
  hour?: number;
  dayOfWeek?: number;
  algorithm?: string;
  cron?: string;
}
