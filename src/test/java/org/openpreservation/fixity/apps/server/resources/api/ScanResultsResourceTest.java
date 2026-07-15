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
package org.openpreservation.fixity.apps.server.resources.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpreservation.fixity.Utils;
import org.openpreservation.fixity.apps.dao.CollectionPath;
import org.openpreservation.fixity.apps.dao.FileScanRecord;
import org.openpreservation.fixity.apps.dao.FolderScanRecord;
import org.openpreservation.fixity.apps.dao.PathScan;
import org.openpreservation.fixity.apps.dao.TestSessionFactory;
import org.openpreservation.fixity.apps.schedule.BatchScanner;
import org.openpreservation.fixity.apps.schedule.ScanUpdater;
import org.openpreservation.fixity.core.digests.Algorithms;
import org.openpreservation.fixity.core.digests.Hasher;
import org.openpreservation.fixity.core.paths.PathScanResult;
import org.quartz.JobExecutionException;

import jakarta.ws.rs.NotFoundException;

@SuppressWarnings("null")
public class ScanResultsResourceTest {
    Path testDir;
    Path file1;
    Path file2;
    CollectionPath collectionPath;
    ScanResultsResource resource;

    @BeforeEach
    public void setUp() throws IOException, SQLIntegrityConstraintViolationException {
        testDir = Files.createTempDirectory("scan-results-resource-test");
        file1 = testDir.resolve("file1.txt");
        file2 = testDir.resolve("file2.txt");
        Files.writeString(file1, "content one");
        Files.writeString(file2, "content two");
        TestSessionFactory.beginTransaction();
        collectionPath = TestSessionFactory.dataFactory().collectionPathDAO().create(CollectionPath.of(testDir));
        resource = new ScanResultsResource(TestSessionFactory.dataFactory());
    }

    @AfterEach
    public void tearDown() throws IOException {
        TestSessionFactory.rollback();
        Utils.deleteDirectory(testDir);
    }

    private PathScan persistScan() throws NoSuchAlgorithmException, IOException, JobExecutionException {
        BatchScanner scanner = new BatchScanner();
        PathScanResult result = scanner.scan(testDir, Hasher.instance(EnumSet.of(Algorithms.SHA_256)), false);
        PathScan scan = scanner.getScan();
        scan.updateFrom(result);
        new ScanUpdater(TestSessionFactory.dataFactory().collectionPathDAO()).updateDatabase(scan);
        // Reload from DB so the returned scan has its generated ID assigned
        return TestSessionFactory.dataFactory().pathScanDAO()
                .findByCollectionPath(collectionPath).stream().findFirst().orElseThrow();
    }

    // --- getScansForPath ---

    @Test
    public void testGetScansForPathReturnsEmptyWhenNoScans() {
        List<PathScan> scans = resource.getScansForPath(collectionPath.getId());
        assertNotNull(scans);
        assertEquals(0, scans.size());
    }

    @Test
    public void testGetScansForPathReturnsPersistedScan() throws NoSuchAlgorithmException, JobExecutionException, IOException {
        persistScan();
        List<PathScan> scans = resource.getScansForPath(collectionPath.getId());
        assertEquals(1, scans.size());
    }

    @Test
    public void testGetScansForPathThrowsForUnknownPath() {
        assertThrows(NotFoundException.class, () -> resource.getScansForPath(99999L));
    }

    // --- getFoldersForScan ---

    @Test
    public void testGetFoldersForScanReturnsNonEmptyList() throws NoSuchAlgorithmException, JobExecutionException, IOException {
        PathScan scan = persistScan();
        List<FolderScanRecord> folders = resource.getFoldersForScan(collectionPath.getId(), scan.getId());
        assertFalse(folders.isEmpty());
    }

    @Test
    public void testGetFoldersForScanThrowsForUnknownScan() {
        assertThrows(NotFoundException.class, () -> resource.getFoldersForScan(collectionPath.getId(), 99999L));
    }

    // --- getResultsForFolder ---

    @Test
    public void testGetResultsForFolderReturnsFiles() throws NoSuchAlgorithmException, JobExecutionException, IOException {
        PathScan scan = persistScan();
        FolderScanRecord rootFolder = resource.getFoldersForScan(collectionPath.getId(), scan.getId())
                .stream().filter(f -> f.getRelativePath().equals("")).findFirst().orElseThrow();
        List<FileScanRecord> files = resource.getResultsForFolder(collectionPath.getId(), scan.getId(), rootFolder.getId());
        assertEquals(2, files.size());
    }

    @Test
    public void testGetResultsForFolderThrowsForUnknownFolder() {
        assertThrows(NotFoundException.class,
                () -> resource.getResultsForFolder(collectionPath.getId(), 99999L, 99999L));
    }

}
