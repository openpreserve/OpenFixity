/*
 * OpenFixity is an application for monitoring and reporting on the fixity of files.
 * Copyright (C) 2026 Open Preservation Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openpreservation.fixity.apps.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpreservation.fixity.Utils;
import org.openpreservation.fixity.apps.dao.CollectionPath;
import org.openpreservation.fixity.apps.dao.CollectionPathDAO;
import org.openpreservation.fixity.apps.dao.FileScanRecord;
import org.openpreservation.fixity.apps.dao.FolderScanRecord;
import org.openpreservation.fixity.apps.dao.PathAuditStatus;
import org.openpreservation.fixity.apps.dao.PathScan;
import org.openpreservation.fixity.apps.dao.TestSessionFactory;
import org.openpreservation.fixity.core.digests.Algorithms;
import org.openpreservation.fixity.core.digests.Hasher;
import org.openpreservation.fixity.core.paths.PathScanResult;
import org.quartz.JobExecutionException;

@SuppressWarnings("null")
public class ScanUpdaterDatabaseTest {
    Path testDir;
    Path file1;
    Path file2;

    @BeforeEach
    public void setUp() throws IOException, SQLIntegrityConstraintViolationException {
        testDir = Files.createTempDirectory("scan-updater-db-test");
        file1 = testDir.resolve("file1.txt");
        file2 = testDir.resolve("file2.txt");
        Files.writeString(file1, "content one");
        Files.writeString(file2, "content two");
        TestSessionFactory.beginTransaction();
        TestSessionFactory.dataFactory().collectionPathDAO().create(CollectionPath.of(testDir));
    }

    @AfterEach
    public void tearDown() throws IOException {
        TestSessionFactory.rollback();
        Utils.deleteDirectory(testDir);
    }

    private PathScan runScan() throws NoSuchAlgorithmException, IOException, JobExecutionException {
        CollectionPathDAO dao = TestSessionFactory.dataFactory().collectionPathDAO();
        BatchScanner scanner = new BatchScanner();
        PathScanResult result = scanner.scan(testDir, Hasher.instance(EnumSet.of(Algorithms.SHA_256)), false);
        PathScan scan = scanner.getScan();
        scan.updateFrom(result);
        new ScanUpdater(dao).updateDatabase(scan);
        return scan;
    }

    private Map<String, PathAuditStatus> statusMap(final PathScan scan) {
        return scan.getAllFiles().stream()
                .collect(Collectors.toMap(FileScanRecord::relativePath, FileScanRecord::getAuditStatus));
    }

    @Test
    public void testFirstScanFilesAreAdded() throws NoSuchAlgorithmException, JobExecutionException, IOException {
        // With no previous scan in the DB, files should remain ADDED.
        // Bug: addPathScan(scan) is called before getLatestScan(), so the scan
        // compares to itself and all files come out VERIFIED instead.
        PathScan scan = runScan();
        assertTrue(scan.getAllFiles().stream()
                .allMatch(r -> r.getAuditStatus() == PathAuditStatus.ADDED));
    }

    @Test
    public void testFolderRecordsPersistedWithScan() throws NoSuchAlgorithmException, JobExecutionException, IOException {
        PathScan scan = runScan();
        List<FolderScanRecord> folders = TestSessionFactory.dataFactory().folderScanRecordDAO().findByPathScan(scan);
        assertFalse(folders.isEmpty());
    }

    @Test
    public void testFileScanRecordsHaveFolderFK() throws NoSuchAlgorithmException, JobExecutionException, IOException {
        PathScan scan = runScan();
        assertTrue(scan.getAllFiles().stream().allMatch(r -> r.getFolder() != null));
    }

    @Test
    public void testRootFilesShareOneFolderRecord() throws NoSuchAlgorithmException, JobExecutionException, IOException {
        PathScan scan = runScan();
        // file1.txt and file2.txt are in the scan root — they should share one FolderScanRecord
        long distinctFolderIds = scan.getAllFiles().stream()
                .map(r -> r.getFolder().getId())
                .distinct()
                .count();
        assertEquals(1L, distinctFolderIds);
    }

    @Test
    public void testSecondScanAfterDeletionDoesNotThrow() throws NoSuchAlgorithmException, JobExecutionException, IOException, InterruptedException {
        runScan(); // first scan — establishes baseline
        Thread.sleep(10);
        Files.delete(file2); // delete a file between scans
        // Must complete without a NULL folder_id constraint violation
        PathScan latest = runScan();
        // The deleted file should appear in results with a folder assigned
        assertTrue(latest.getAllFiles().stream()
                .filter(r -> r.relativePath().equals("file2.txt"))
                .allMatch(r -> r.getFolder() != null && r.getAuditStatus() == PathAuditStatus.NOTFOUND),
                "Deleted file record must have a FolderScanRecord assigned and NOTFOUND status");
    }

    @Test
    public void testModifiedFileDetectedAsChangedViaDatabase() throws InterruptedException, NoSuchAlgorithmException, JobExecutionException, IOException {
        runScan(); // first scan — establishes baseline in DB
        Thread.sleep(10); // ensure started timestamps differ
        Files.writeString(file1, "DIFFERENT CONTENT");
        PathScan latest = runScan(); // second scan

        // Bug: getLatestScan() returns the current scan (not the previous one),
        // so updateScan compares the scan to itself and the modified file
        // appears VERIFIED instead of CHANGED.
        Map<String, PathAuditStatus> statuses = statusMap(latest);
        assertEquals(PathAuditStatus.CHANGED, statuses.get("file1.txt"));
        assertEquals(PathAuditStatus.VERIFIED, statuses.get("file2.txt"));
    }
}
