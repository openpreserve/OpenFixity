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
package org.openpreservation.fixity.apps.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.EnumSet;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpreservation.fixity.Utils;
import org.openpreservation.fixity.core.digests.Algorithms;
import org.openpreservation.fixity.core.digests.Hasher;
import org.openpreservation.fixity.core.paths.PathScanner;
import org.openpreservation.fixity.core.paths.PathSummary;

import jakarta.persistence.NoResultException;

public class PathScanTest {
    @SuppressWarnings("null")
    @NonNull Path testDirPath;
    CollectionPath collectionPath;

    @BeforeEach
    public void setUp() throws IOException, SQLIntegrityConstraintViolationException {
        TestSessionFactory.beginTransaction();
        testDirPath = Utils.createTempTestPath("fixity-scan-record-test-dir");
        collectionPath = TestSessionFactory.dataFactory().collectionPathDAO().create(CollectionPath.of(testDirPath));
    }

    @AfterEach
    public void tearDown() throws IOException {
        TestSessionFactory.rollback();
        Utils.deleteDirectory(testDirPath.toFile());
    }

    @SuppressWarnings("null")
    @Test
    public void testAddScan() throws IOException, SQLIntegrityConstraintViolationException, NoSuchAlgorithmException {
        DataFactory df = TestSessionFactory.dataFactory();
        // Count BEFORE adding scan to managed collectionPath.pathScans to avoid cascade pre-persisting the scan
        int expectedRecords = df.fileScanRecordDAO().findAll().size() + 2;
        PathScan pathScan = collectionPath.createPathScan(PathSummary.of(testDirPath, false));
        pathScan.updateFrom(PathScanner.instance().scan(testDirPath, Hasher.instance(EnumSet.of(Algorithms.SHA_256)), false));
        df.pathScanDAO().create(pathScan);
        assertEquals(expectedRecords, df.fileScanRecordDAO().findAll().size());
    }

    @SuppressWarnings("null")
    @Test
    public void testMultipleScanItems() throws IOException, SQLIntegrityConstraintViolationException, NoSuchAlgorithmException {
        DataFactory df = TestSessionFactory.dataFactory();
        int expectedRecords = df.fileScanRecordDAO().findAll().size() + 3;
        PathScan pathScan = collectionPath.createPathScan(PathSummary.of(testDirPath, true));
        pathScan.updateFrom(PathScanner.instance().scan(testDirPath, Hasher.instance(EnumSet.of(Algorithms.SHA_256)), true));
        df.pathScanDAO().create(pathScan);
        assertEquals(expectedRecords, df.fileScanRecordDAO().findAll().size());
    }

    @SuppressWarnings("null")
    @Test
    public void testMultipleScans() throws IOException, SQLIntegrityConstraintViolationException, NoSuchAlgorithmException {
        DataFactory df = TestSessionFactory.dataFactory();
        int expectedRecords = df.fileScanRecordDAO().findAll().size() + 3;
        int expectedScans = df.pathScanDAO().findAll().size() + 2;
        PathScan pathScan = collectionPath.createPathScan(PathSummary.of(testDirPath, true));
        pathScan.updateFrom(PathScanner.instance().scan(testDirPath, Hasher.instance(EnumSet.of(Algorithms.SHA_256)), true));
        df.pathScanDAO().create(pathScan);
        assertEquals(expectedRecords, df.fileScanRecordDAO().findAll().size());
        pathScan = collectionPath.createPathScan(PathSummary.of(testDirPath, true));
        pathScan.updateFrom(PathScanner.instance().scan(testDirPath, Hasher.instance(EnumSet.of(Algorithms.SHA_256)), true));
        df.pathScanDAO().create(pathScan);
        assertEquals(expectedRecords + 3, df.fileScanRecordDAO().findAll().size());
        assertEquals(expectedScans, df.pathScanDAO().findAll().size());
    }

    // --- findById ---

    @SuppressWarnings("null")
    @Test
    public void testFindByIdReturnsPersistedScan() throws IOException, SQLIntegrityConstraintViolationException, NoSuchAlgorithmException {
        DataFactory df = TestSessionFactory.dataFactory();
        PathScan scan = collectionPath.createPathScan(PathSummary.of(testDirPath, false));
        df.pathScanDAO().create(scan);
        assertNotNull(scan.getId());
        PathScan found = df.pathScanDAO().findById(scan.getId());
        assertEquals(scan.getId(), found.getId());
    }

    @Test
    public void testFindByIdThrowsForNonExistentId() {
        assertThrows(NoResultException.class, () -> TestSessionFactory.dataFactory().pathScanDAO().findById(99999L));
    }

    // --- create guard ---

    @Test
    public void testCreateThrowsForNonNullId() throws IOException {
        PathScan scan = collectionPath.createPathScan(PathSummary.of(testDirPath, false));
        scan.setId(1L);
        assertThrows(SQLIntegrityConstraintViolationException.class,
                () -> TestSessionFactory.dataFactory().pathScanDAO().create(scan));
    }

    // --- update ---

    @SuppressWarnings("null")
    @Test
    public void testUpdatePersistsCompletedStatus() throws IOException, SQLIntegrityConstraintViolationException, NoSuchAlgorithmException {
        DataFactory df = TestSessionFactory.dataFactory();
        PathScan scan = collectionPath.createPathScan(PathSummary.of(testDirPath, false));
        df.pathScanDAO().create(scan);
        scan.updateFrom(PathScanner.instance().scan(testDirPath, Hasher.instance(EnumSet.of(Algorithms.SHA_256)), false));
        PathScan updated = df.pathScanDAO().update(scan);
        assertTrue(updated.isCompleted());
    }

    @Test
    public void testUpdateThrowsForNullId() throws IOException {
        PathScan scan = collectionPath.createPathScan(PathSummary.of(testDirPath, false));
        assertThrows(SQLIntegrityConstraintViolationException.class,
                () -> TestSessionFactory.dataFactory().pathScanDAO().update(scan));
    }

    @Test
    public void testUpdateThrowsForNonExistentId() throws IOException {
        PathScan scan = collectionPath.createPathScan(PathSummary.of(testDirPath, false));
        scan.setId(99999L);
        assertThrows(SQLIntegrityConstraintViolationException.class,
                () -> TestSessionFactory.dataFactory().pathScanDAO().update(scan));
    }
}
