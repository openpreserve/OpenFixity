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

import java.io.FileNotFoundException;
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
import org.openpreservation.fixity.core.digests.Algorithms;
import org.openpreservation.fixity.core.digests.Hasher;
import org.openpreservation.fixity.core.paths.FileScanResult;
import org.openpreservation.fixity.core.paths.PathScanner;
import org.openpreservation.fixity.core.paths.PathSummary;

import jakarta.persistence.NoResultException;

@SuppressWarnings("null")
public class FileScanRecordDAOTest {
    Path testDir;
    Path file1;
    Path file2;
    CollectionPath collectionPath;
    PathScan pathScan;

    @BeforeEach
    public void setUp() throws IOException, SQLIntegrityConstraintViolationException, NoSuchAlgorithmException {
        testDir = Files.createTempDirectory("filescanrecorddao-test");
        file1 = testDir.resolve("a.txt");
        file2 = testDir.resolve("b.txt");
        Files.writeString(file1, "content a");
        Files.writeString(file2, "content b");
        TestSessionFactory.beginTransaction();
        DataFactory df = TestSessionFactory.dataFactory();
        collectionPath = df.collectionPathDAO().create(CollectionPath.of(testDir));
        pathScan = collectionPath.createPathScan(PathSummary.of(testDir, false));
        pathScan.updateFrom(PathScanner.instance().scan(testDir, Hasher.instance(EnumSet.of(Algorithms.SHA_256)), false));
        df.pathScanDAO().create(pathScan); // cascades to persist the 2 FileScanRecords
    }

    @AfterEach
    public void tearDown() throws IOException {
        TestSessionFactory.rollback();
        Utils.deleteDirectory(testDir.toFile());
    }

    private FileScanRecord firstRecord() {
        return pathScan.getAllFiles().iterator().next();
    }

    private FileScanRecord recordFor(Path file) throws FileNotFoundException, NoSuchAlgorithmException {
        FileScanResult result = FileScanResult.of(file, Hasher.instance(EnumSet.of(Algorithms.SHA_256)));
        FileScanRecord fsr = FileScanRecord.of(pathScan, result);
        FolderScanRecord rootFolder = pathScan.getFolders().stream()
                .filter(f -> f.getRelativePath().equals(""))
                .findFirst().orElseThrow();
        fsr.setFolder(rootFolder);
        return fsr;
    }

    // --- findAll ---

    @Test
    public void testFindAllReturnsAllPersistedRecords() {
        assertEquals(2, TestSessionFactory.dataFactory().fileScanRecordDAO().findAll().size());
    }

    // --- findById ---

    @Test
    public void testFindByIdReturnsCorrectRecord() {
        FileScanRecord record = firstRecord();
        assertNotNull(record.getId());
        FileScanRecord found = TestSessionFactory.dataFactory().fileScanRecordDAO().findById(record.getId());
        assertEquals(record.getId(), found.getId());
    }

    @Test
    public void testFindByIdThrowsForNonExistentId() {
        assertThrows(NoResultException.class,
                () -> TestSessionFactory.dataFactory().fileScanRecordDAO().findById(99999L));
    }

    // --- create ---

    @Test
    public void testCreatePersistsRecord() throws IOException, SQLIntegrityConstraintViolationException, NoSuchAlgorithmException {
        Path file3 = testDir.resolve("c.txt");
        Files.writeString(file3, "content c");
        DataFactory df = TestSessionFactory.dataFactory();
        df.fileScanRecordDAO().create(recordFor(file3));
        assertEquals(3, df.fileScanRecordDAO().findAll().size());
    }

    // --- addAll ---

    @Test
    public void testAddAllPersistsMultipleRecords() throws FileNotFoundException, SQLIntegrityConstraintViolationException, NoSuchAlgorithmException, IOException {
        Path file3 = testDir.resolve("c.txt");
        Path file4 = testDir.resolve("d.txt");
        Files.writeString(file3, "content c");
        Files.writeString(file4, "content d");
        DataFactory df = TestSessionFactory.dataFactory();
        df.fileScanRecordDAO().addAll(List.of(recordFor(file3), recordFor(file4)));
        assertEquals(4, df.fileScanRecordDAO().findAll().size());
    }
}
