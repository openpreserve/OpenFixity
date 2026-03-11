package org.openpreservation.fixity.apps.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpreservation.fixity.Utils;
import org.openpreservation.fixity.apps.dao.CollectionPath;
import org.openpreservation.fixity.apps.dao.FileScanRecord;
import org.openpreservation.fixity.apps.dao.PathAuditStatus;
import org.openpreservation.fixity.apps.dao.PathScan;
import org.openpreservation.fixity.core.digests.Algorithms;
import org.openpreservation.fixity.core.digests.Hasher;
import org.openpreservation.fixity.core.paths.PathScanner;
import org.openpreservation.fixity.core.paths.PathSummary;

public class ScanUpdaterTest {
    Path testDir;
    Path file1;
    Path file2;
    CollectionPath collectionPath;

    @SuppressWarnings("null")
    @BeforeEach
    public void setUp() throws IOException {
        testDir = Files.createTempDirectory("scan-updater-test");
        file1 = testDir.resolve("file1.txt");
        file2 = testDir.resolve("file2.txt");
        Files.writeString(file1, "content one");
        Files.writeString(file2, "content two");
        collectionPath = CollectionPath.of(testDir);
    }

    @AfterEach
    public void tearDown() throws IOException {
        file1.toFile().setReadable(true);
        file2.toFile().setReadable(true);
        Utils.deleteDirectory(testDir.toFile());
    }

    @SuppressWarnings("null")
    private PathScan scan() throws FileNotFoundException, NoSuchAlgorithmException, IOException {
        PathScan pathScan = collectionPath.createPathScan(PathSummary.of(testDir, false));
        pathScan.updateFrom(PathScanner.instance().scan(testDir,
                Hasher.instance(EnumSet.of(Algorithms.SHA_256)), false));
        return pathScan;
    }

    private Map<String, PathAuditStatus> statusMap(final PathScan scan) {
        return scan.getAllFiles().stream()
                .collect(Collectors.toMap(FileScanRecord::relativePath, FileScanRecord::getAuditStatus));
    }

    @Test
    public void testUnchangedFilesAreVerified() throws FileNotFoundException, NoSuchAlgorithmException, IOException {
        PathScan previous = scan();
        PathScan latest = scan();
        new ScanUpdater(null).updateScan(latest, previous);
        assertTrue(latest.getAllFiles().stream()
                .allMatch(r -> r.getAuditStatus() == PathAuditStatus.VERIFIED));
    }

    @Test
    public void testModifiedFileDetectedAsChanged() throws FileNotFoundException, NoSuchAlgorithmException, IOException {
        PathScan previous = scan();
        Files.writeString(file1, "DIFFERENT CONTENT");
        PathScan latest = scan();
        new ScanUpdater(null).updateScan(latest, previous);

        Map<String, PathAuditStatus> statuses = statusMap(latest);
        assertEquals(PathAuditStatus.CHANGED, statuses.get("file1.txt"));
        assertEquals(PathAuditStatus.VERIFIED, statuses.get("file2.txt"));
    }

    @Test
    public void testDeletedFileTrackedInLatestScan() throws FileNotFoundException, NoSuchAlgorithmException, IOException {
        PathScan previous = scan();
        Files.delete(file2);
        PathScan latest = scan();
        new ScanUpdater(null).updateScan(latest, previous);

        // file2 was in previous but not latest: a tracking record is added to latest
        assertEquals(2, latest.getAllFiles().size());
        Map<String, PathAuditStatus> statuses = statusMap(latest);
        assertEquals(PathAuditStatus.VERIFIED, statuses.get("file1.txt"));
        assertEquals(PathAuditStatus.NOTFOUND, statuses.get("file2.txt"));
    }

    @Test
    public void testNewFileMarkedAsAdded() throws FileNotFoundException, NoSuchAlgorithmException, IOException {
        // First scan contains only file1
        Files.delete(file2);
        PathScan previous = scan();
        // Restore file2 so latest scan picks it up
        Files.writeString(file2, "content two");
        PathScan latest = scan();
        new ScanUpdater(null).updateScan(latest, previous);

        Map<String, PathAuditStatus> statuses = statusMap(latest);
        assertEquals(PathAuditStatus.VERIFIED, statuses.get("file1.txt"));
        assertEquals(PathAuditStatus.ADDED, statuses.get("file2.txt"));
    }

    @Test
    public void testDeniedFileHasDeniedAuditStatusOnSubsequentScan()
            throws FileNotFoundException, NoSuchAlgorithmException, IOException {
        // Both scans see file1 as DENIED — audit status must be DENIED, not ADDED inherited from scan 1
        file1.toFile().setReadable(false);
        PathScan previous = scan();
        PathScan latest = scan();
        new ScanUpdater(null).updateScan(latest, previous);
        assertEquals(PathAuditStatus.DENIED, statusMap(latest).get("file1.txt"));
    }

    @Test
    public void testRestoredFileMarkedAsAdded() throws FileNotFoundException, NoSuchAlgorithmException, IOException {
        // Scan 1: both files present
        PathScan first = scan();
        // Scan 2: file2 deleted — previous is NOTFOUND
        Files.delete(file2);
        PathScan withDeletion = scan();
        new ScanUpdater(null).updateScan(withDeletion, first);
        assertEquals(PathAuditStatus.NOTFOUND, statusMap(withDeletion).get("file2.txt"));

        // Scan 3: file2 restored — must be ADDED, not NOTFOUND
        Files.writeString(file2, "content two");
        PathScan withRestoration = scan();
        new ScanUpdater(null).updateScan(withRestoration, withDeletion);
        assertEquals(PathAuditStatus.ADDED, statusMap(withRestoration).get("file2.txt"));
    }
}
