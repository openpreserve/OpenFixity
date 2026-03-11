package org.openpreservation.fixity.apps.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpreservation.fixity.Utils;
import org.openpreservation.fixity.apps.dao.PathAuditStatus;
import org.openpreservation.fixity.apps.schedule.BatchScanner;
import org.openpreservation.fixity.core.digests.Algorithms;
import org.openpreservation.fixity.core.digests.Hasher;
import org.openpreservation.fixity.core.paths.FileScanStatus;
import org.openpreservation.fixity.core.paths.PathScanResult;
import org.openpreservation.fixity.core.paths.PathScanner;
import org.openpreservation.fixity.core.paths.PathSummary;

@SuppressWarnings("null")
public class PathScanBehaviourTest {
    Path testDir;
    Path file1;
    Path file2;

    @BeforeEach
    public void setUp() throws IOException {
        testDir = Files.createTempDirectory("path-scan-behaviour-test");
        file1 = testDir.resolve("file1.txt");
        file2 = testDir.resolve("file2.txt");
        Files.writeString(file1, "content one");
        Files.writeString(file2, "content two");
    }

    @AfterEach
    public void tearDown() throws IOException {
        file1.toFile().setReadable(true);
        file2.toFile().setReadable(true);
        Utils.deleteDirectory(testDir.toFile());
    }

    /** Scan testDir with BatchScanner; returns the resulting (completed) PathScan. */
    private PathScan batchScan() throws IOException, NoSuchAlgorithmException {
        BatchScanner scanner = new BatchScanner();
        PathScanResult result = scanner.scan(testDir, Hasher.instance(EnumSet.of(Algorithms.SHA_256)), false);
        PathScan scan = scanner.getScan();
        scan.updateFrom(result);
        return scan;
    }

    // --- Initial state ---

    @Test
    public void testInitialStatusIsInitialised() {
        PathScan scan = CollectionPath.of(testDir).createPathScan(PathSummary.of(testDir));
        assertEquals(ScanStatus.INITIALISED, scan.getStatus());
    }

    @Test
    public void testIsCompletedFalseInitially() {
        PathScan scan = CollectionPath.of(testDir).createPathScan(PathSummary.of(testDir));
        assertFalse(scan.isCompleted());
    }

    @Test
    public void testGetStartedIsNotNull() {
        PathScan scan = CollectionPath.of(testDir).createPathScan(PathSummary.of(testDir));
        assertNotNull(scan.getStarted());
    }

    @Test
    public void testGetStoppedNullInitially() {
        PathScan scan = CollectionPath.of(testDir).createPathScan(PathSummary.of(testDir));
        assertNull(scan.getStopped());
    }

    @Test
    public void testGetDurationRunningReturnsSecondsString() {
        PathScan scan = CollectionPath.of(testDir).createPathScan(PathSummary.of(testDir));
        assertTrue(scan.getDuration().endsWith("s"));
    }

    // --- After updateFrom ---

    @Test
    public void testAfterUpdateFromStatusIsCompleted() throws IOException, NoSuchAlgorithmException {
        PathScan scan = batchScan();
        assertTrue(scan.isCompleted());
    }

    @Test
    public void testAfterUpdateFromStoppedIsSet() throws IOException, NoSuchAlgorithmException {
        PathScan scan = batchScan();
        assertNotNull(scan.getStopped());
    }

    @Test
    public void testAfterUpdateFromDurationReturnsSecondsString() throws IOException, NoSuchAlgorithmException {
        PathScan scan = batchScan();
        assertTrue(scan.getDuration().endsWith("s"));
    }

    // --- Result management ---

    @Test
    public void testGetResultCount() throws IOException, NoSuchAlgorithmException {
        PathScan scan = batchScan();
        assertEquals(2, scan.getResultCount());
    }

    @Test
    public void testAddResultIncreasesCount() throws IOException, NoSuchAlgorithmException {
        // Create an extra file and a separate scan to get a third FileScanRecord
        Path file3 = testDir.resolve("file3.txt");
        Files.writeString(file3, "content three");
        BatchScanner extra = new BatchScanner();
        extra.scan(testDir, Hasher.instance(EnumSet.of(Algorithms.SHA_256)), false);
        FileScanRecord record = extra.getScan().getAllFiles().stream()
                .filter(r -> r.relativePath().equals("file3.txt"))
                .findFirst().orElseThrow();
        Files.delete(file3);

        PathScan scan = batchScan();
        assertEquals(2, scan.getResultCount());
        scan.addFile(record);
        assertEquals(3, scan.getResultCount());
    }

    @Test
    public void testAddResultForDeletedAddsTrackingRecord() throws IOException, NoSuchAlgorithmException {
        PathScan scan = batchScan();
        FileScanRecord previous = scan.getAllFiles().iterator().next();
        int before = scan.getResultCount();
        // HashSet deduplicates — use a second scan to get a fresh record for the same file
        BatchScanner scanner2 = new BatchScanner();
        scanner2.scan(testDir, Hasher.instance(EnumSet.of(Algorithms.SHA_256)), false);
        FileScanRecord deleted = scanner2.getScan().getAllFiles().stream()
                .filter(r -> r.relativePath().equals(previous.relativePath()))
                .findFirst().orElseThrow();
        scan.addResultForDeleted(deleted);
        assertEquals(before + 1, scan.getResultCount());
    }

    // --- Initial audit status for problem files ---

    @Test
    public void testDeniedFileHasDeniedAuditStatusOnFirstScan() throws IOException, NoSuchAlgorithmException {
        // A first-scan DENIED file must have DENIED audit status, not ADDED
        file1.toFile().setReadable(false);
        PathScan scan = batchScan();
        FileScanRecord denied = scan.getAllFiles().stream()
                .filter(r -> r.getStatus() == FileScanStatus.DENIED)
                .findFirst().orElseThrow();
        assertEquals(PathAuditStatus.DENIED, denied.getAuditStatus());
    }

    // --- isDenied / getDeniedResults / getDeniedCount ---

    @Test
    public void testIsDeniedFalseWhenNoFilesAreDenied() throws IOException, NoSuchAlgorithmException {
        assertFalse(batchScan().isDenied());
    }

    @Test
    public void testIsDeniedTrueWhenFileIsDenied() throws IOException, NoSuchAlgorithmException {
        file1.toFile().setReadable(false);
        assertTrue(batchScan().isDenied());
    }

    @Test
    public void testGetDeniedCountIsCorrect() throws IOException, NoSuchAlgorithmException {
        file1.toFile().setReadable(false);
        assertEquals(1, batchScan().getDeniedCount());
    }

    @Test
    public void testGetDeniedResultsContainsDeniedFile() throws IOException, NoSuchAlgorithmException {
        file1.toFile().setReadable(false);
        PathScan scan = batchScan();
        assertEquals(1, scan.getDeniedResults().size());
        assertEquals(FileScanStatus.DENIED, scan.getDeniedResults().get(0).getStatus());
    }

    // --- isDamaged / getDamagedResults / getDamagedCount ---

    @Test
    public void testIsDamagedFalseWhenNoFilesAreDamaged() throws IOException, NoSuchAlgorithmException {
        assertFalse(batchScan().isDamaged());
    }

    @Test
    public void testGetDamagedCountZeroWithNoDAmagedFiles() throws IOException, NoSuchAlgorithmException {
        assertEquals(0, batchScan().getDamagedCount());
    }

    @Test
    public void testGetDamagedResultsEmptyWithNoDamagedFiles() throws IOException, NoSuchAlgorithmException {
        assertTrue(batchScan().getDamagedResults().isEmpty());
    }

    // --- updateFrom guards ---

    @Test
    public void testUpdateFromWrongPathThrows() throws IOException, NoSuchAlgorithmException {
        PathScan scan = batchScan();
        Path otherDir = Files.createTempDirectory("path-scan-other");
        try {
            Files.writeString(otherDir.resolve("other.txt"), "other");
            PathScanResult wrongResult = PathScanner.instance()
                    .scan(otherDir, Hasher.instance(EnumSet.of(Algorithms.SHA_256)), false);
            assertThrows(IllegalArgumentException.class, () -> scan.updateFrom(wrongResult));
        } finally {
            Utils.deleteDirectory(otherDir.toFile());
        }
    }

    @Test
    public void testUpdateFromDoesNotReplaceResultsWhenScanResultHasFewerFiles()
            throws IOException, NoSuchAlgorithmException {
        // BatchScanner populates folders via processResults().
        // Calling updateFrom() again should not re-populate (guard: folders.isEmpty()).
        PathScan scan = batchScan();
        assertEquals(2, scan.getResultCount());
        Files.delete(file2);
        PathScanResult smallerResult = PathScanner.instance()
                .scan(testDir, Hasher.instance(EnumSet.of(Algorithms.SHA_256)), false);
        assertEquals(1, smallerResult.getResults().size());
        scan.updateFrom(smallerResult);
        assertEquals(2, scan.getResultCount());
    }

    // --- Folder support ---

    @Test
    public void testGetFoldersReturnsNonNullAfterUpdateFrom() throws IOException, NoSuchAlgorithmException {
        assertNotNull(batchScan().getFolders());
    }

    @Test
    public void testGetFoldersIsNotEmptyAfterUpdateFrom() throws IOException, NoSuchAlgorithmException {
        assertFalse(batchScan().getFolders().isEmpty());
    }

    @Test
    public void testRootFilesAreInRootFolder() throws IOException, NoSuchAlgorithmException {
        PathScan scan = batchScan();
        // file1.txt and file2.txt are in the root; folder relativePath should be ""
        assertTrue(scan.getFolders().stream().anyMatch(f -> f.getRelativePath().equals("")));
    }

    @Test
    public void testEachFileHasNonNullFolder() throws IOException, NoSuchAlgorithmException {
        PathScan scan = batchScan();
        assertTrue(scan.getAllFiles().stream().allMatch(r -> r.getFolder() != null));
    }

    @Test
    public void testRootFolderExistsEvenWhenNoFilesAreInRoot() throws IOException, NoSuchAlgorithmException {
        // If the scanned directory has no files directly in it (only subdirs), a root FolderScanRecord
        // with relativePath "" must still be created so the UI can always find it
        Files.delete(file1);
        Files.delete(file2);
        Path subDir = Files.createDirectory(testDir.resolve("subdir"));
        Files.writeString(subDir.resolve("file3.txt"), "content three");
        BatchScanner scanner = new BatchScanner();
        PathScanResult result = scanner.scan(testDir, Hasher.instance(EnumSet.of(Algorithms.SHA_256)), true);
        PathScan scan = scanner.getScan();
        scan.updateFrom(result);
        assertTrue(scan.getFolders().stream().anyMatch(f -> f.getRelativePath().equals("")));
    }

    @Test
    public void testIntermediateFolderCreatedEvenWhenItHasNoDirectFiles() throws IOException, NoSuchAlgorithmException {
        // If files live in subdir/nested/ but nothing is directly in subdir/, a FolderScanRecord
        // for "subdir" must still be created so it is reachable through navigation
        Path subDir = Files.createDirectory(testDir.resolve("subdir"));
        Path nestedDir = Files.createDirectory(subDir.resolve("nested"));
        Files.writeString(nestedDir.resolve("file3.txt"), "content three");
        BatchScanner scanner = new BatchScanner();
        PathScanResult result = scanner.scan(testDir, Hasher.instance(EnumSet.of(Algorithms.SHA_256)), true);
        PathScan scan = scanner.getScan();
        scan.updateFrom(result);
        assertTrue(scan.getFolders().stream().anyMatch(f -> f.getRelativePath().equals("subdir")));
    }

    @Test
    public void testSubdirFilesAreInSubFolder() throws IOException, NoSuchAlgorithmException {
        Path subDir = Files.createDirectory(testDir.resolve("subdir"));
        Files.writeString(subDir.resolve("file3.txt"), "content three");
        // Use recursive scan so subdir/file3.txt is included
        BatchScanner scanner = new BatchScanner();
        PathScanResult result = scanner.scan(testDir, Hasher.instance(EnumSet.of(Algorithms.SHA_256)), true);
        PathScan scan = scanner.getScan();
        scan.updateFrom(result);
        assertTrue(scan.getFolders().stream().anyMatch(f -> f.getRelativePath().equals("subdir")));
    }

    @Test
    public void testFilesInSameDirShareOneFolderRecord() throws IOException, NoSuchAlgorithmException {
        PathScan scan = batchScan();
        // Both root files should reference the same FolderScanRecord instance
        FolderScanRecord rootFolder = scan.getFolders().stream()
                .filter(f -> f.getRelativePath().equals(""))
                .findFirst().orElseThrow();
        assertTrue(scan.getAllFiles().stream()
                .allMatch(r -> r.getFolder() == rootFolder));
    }

    // --- Accessors ---

    @Test
    public void testGetCollectionPath() throws IOException, NoSuchAlgorithmException {
        PathScan scan = batchScan();
        assertEquals(testDir.toAbsolutePath().normalize(), scan.getCollectionPath().getRoot());
    }

    @Test
    public void testGetSummaryReturnsNonNull() throws IOException, NoSuchAlgorithmException {
        PathScan scan = batchScan();
        assertNotNull(scan.getSummary());
    }
}
