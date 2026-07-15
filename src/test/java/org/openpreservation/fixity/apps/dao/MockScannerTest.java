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

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpreservation.fixity.Utils;
import org.openpreservation.fixity.core.digests.Algorithms;
import org.openpreservation.fixity.core.digests.Hasher;
import org.openpreservation.fixity.core.paths.PathScanner;
import org.openpreservation.fixity.core.paths.PathSummary;

public class MockScannerTest {
    Path testDirPath;
    CollectionPath collectionPath;

    @SuppressWarnings("null")
    @BeforeEach
    public void setUp() throws IOException, SQLIntegrityConstraintViolationException {
        TestSessionFactory.beginTransaction();
        testDirPath = Utils.createTempTestPath("fixity-scan-record-test-dir");
        collectionPath = TestSessionFactory.dataFactory().collectionPathDAO().create(CollectionPath.of(testDirPath));
    }

    @AfterEach
    public void tearDown() throws IOException {
        TestSessionFactory.rollback();
        Utils.deleteDirectory(testDirPath);
    }

    @SuppressWarnings("null")
    @Test
    public void testScan() throws IOException, SQLIntegrityConstraintViolationException, NoSuchAlgorithmException {
        DataFactory df = TestSessionFactory.dataFactory();
        int expectedRecords = df.fileScanRecordDAO().findAll().size() + 2;
        PathScan scan = collectionPath.createPathScan(PathSummary.of(testDirPath, false));
        scan.updateFrom(PathScanner.instance().scan(testDirPath, Hasher.instance(Collections.singleton(Algorithms.SHA_256)), false));
        df.pathScanDAO().create(scan);
        assertEquals(expectedRecords, df.fileScanRecordDAO().findAll().size());
    }
}
