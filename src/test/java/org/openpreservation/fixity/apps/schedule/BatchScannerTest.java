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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpreservation.fixity.Utils;
import org.openpreservation.fixity.apps.dao.FileScanRecord;
import org.openpreservation.fixity.core.digests.Algorithms;
import org.openpreservation.fixity.core.digests.Hasher;
import org.openpreservation.fixity.core.paths.FileScanStatus;

public class BatchScannerTest {
    Path testDir;
    Path file1;
    Path file2;

    @BeforeEach
    public void setUp() throws IOException {
        testDir = Files.createTempDirectory("batch-scanner-test");
        file1 = testDir.resolve("file1.txt");
        file2 = testDir.resolve("file2.txt");
        Files.writeString(file1, "content one");
        Files.writeString(file2, "content two");
    }

    @AfterEach
    public void tearDown() throws IOException {
        Utils.deleteDirectory(testDir);
    }

    @SuppressWarnings("null")
    private BatchScanner runScan(final boolean recursive) throws IOException, NoSuchAlgorithmException {
        BatchScanner scanner = new BatchScanner();
        scanner.scan(testDir, Hasher.instance(EnumSet.of(Algorithms.SHA_256)), recursive);
        return scanner;
    }

    @Test
    public void testScanCreatesScan() throws IOException, NoSuchAlgorithmException {
        assertNotNull(runScan(true).getScan());
    }

    @Test
    public void testNonRecursiveScanFileCount() throws IOException, NoSuchAlgorithmException {
        assertEquals(2, runScan(false).getScan().getAllFiles().size());
    }

    @Test
    public void testRecursiveScanFileCount() throws IOException, NoSuchAlgorithmException {
        Path subDir = Files.createDirectory(testDir.resolve("subdir"));
        Files.writeString(subDir.resolve("file3.txt"), "content three");
        assertEquals(3, runScan(true).getScan().getAllFiles().size());
    }

    @SuppressWarnings("null")
    @Test
    public void testScanResultsHaveCorrectRelativePaths() throws IOException, NoSuchAlgorithmException {
        Set<String> paths = runScan(false).getScan().getAllFiles().stream()
                .map(FileScanRecord::relativePath)
                .collect(Collectors.toSet());
        assertTrue(paths.contains("file1.txt"));
        assertTrue(paths.contains("file2.txt"));
    }

    @Test
    public void testScanResultsHaveScannedStatus() throws IOException, NoSuchAlgorithmException {
        assertTrue(runScan(false).getScan().getAllFiles().stream()
                .allMatch(r -> r.getStatus() == FileScanStatus.SCANNED));
    }

    @Test
    public void testScanResultsHaveDigests() throws IOException, NoSuchAlgorithmException {
        assertTrue(runScan(false).getScan().getAllFiles().stream()
                .allMatch(r -> !r.getDigestResults().isEmpty()));
    }

    @Test
    public void testEachResultHasExactlyOneDigest() throws IOException, NoSuchAlgorithmException {
        assertTrue(runScan(false).getScan().getAllFiles().stream()
                .allMatch(r -> r.getDigestResults().size() == 1));
    }
}
