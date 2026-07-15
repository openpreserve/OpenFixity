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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLIntegrityConstraintViolationException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpreservation.fixity.Utils;
import org.openpreservation.fixity.apps.dao.CollectionPath;
import org.openpreservation.fixity.apps.dao.CollectionPathDAO;
import org.openpreservation.fixity.apps.dao.TestSessionFactory;
import org.openpreservation.fixity.core.digests.Algorithms;
import org.quartz.JobExecutionException;

@SuppressWarnings("null")
public class ScanJobTest {
    Path testDir;

    @BeforeEach
    public void setUp() throws IOException, SQLIntegrityConstraintViolationException {
        testDir = Files.createTempDirectory("scan-job-test");
        Files.writeString(testDir.resolve("file1.txt"), "content one");
        Files.writeString(testDir.resolve("file2.txt"), "content two");
        TestSessionFactory.beginTransaction();
        TestSessionFactory.dataFactory().collectionPathDAO().create(CollectionPath.of(testDir));
    }

    @AfterEach
    public void tearDown() throws IOException {
        TestSessionFactory.rollback();
        Utils.deleteDirectory(testDir);
    }

    @Test
    public void testFindPathForNonExistentDir() {
        assertThrows(JobExecutionException.class, () ->
                new ScanJob().findCollectionPathForScan("/nonexistent/path/that/does/not/exist"));
    }

    @Test
    public void testFindPathForFile() {
        assertThrows(JobExecutionException.class, () ->
                new ScanJob().findCollectionPathForScan(testDir.resolve("file1.txt").toString()));
    }

    @Test
    public void testFindPathForValidDir() throws JobExecutionException {
        Path result = new ScanJob().findCollectionPathForScan(testDir.toString());
        assertEquals(testDir, result);
    }

    @Test
    public void testExecuteScanCreatesPathScanWithResults() throws IOException, NoSuchAlgorithmException, JobExecutionException {
        CollectionPathDAO dao = TestSessionFactory.dataFactory().collectionPathDAO();
        ScanUpdater updater = new ScanUpdater(dao);
        new ScanJob().executeScan(testDir.toString(), Algorithms.SHA_256, updater);

        CollectionPath cp = dao.findByRoot(testDir);
        assertEquals(1, cp.getPathScans().size());
        assertEquals(2, cp.getLatestScan().getAllFiles().size());
    }
}
