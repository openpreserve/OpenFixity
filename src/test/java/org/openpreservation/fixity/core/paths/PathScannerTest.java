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
package org.openpreservation.fixity.core.paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpreservation.fixity.Utils;
import org.openpreservation.fixity.core.digests.Algorithms;
import org.openpreservation.fixity.core.digests.DigestResult;
import org.openpreservation.fixity.core.digests.Hasher;

public class PathScannerTest {
    @SuppressWarnings("null")
    @NonNull
    Path testPath;
    PathScanner scanner;

    @BeforeEach
    public void setUp() throws IOException, NoSuchAlgorithmException {
        testPath = Utils.createTempTestPath("fixity-pathsummary-tests");
        scanner = PathScanner.instance();
    }

    @AfterEach
    public void tearDown() throws IOException {
        Utils.deleteDirectory(testPath.toFile());
    }

    @Test
    public void testListDirectoriesNonRecursive() throws IOException {
        var dirs = scanner.listDirectories(this.testPath, false);
        assertEquals(2, dirs.size());
    }

    @Test
    public void testListDirectoriesRecursive() throws IOException {
        Utils.addNestedDir(this.testPath);
        var dirs = scanner.listDirectories(this.testPath, true);
        assertEquals(3, dirs.size());
    }

    @Test
    public void testListFilesNonRecursive() throws IOException {
        var files = scanner.listFiles(this.testPath, false);
        assertEquals(2, files.size());
    }

    @Test
    public void testListFilesRecursive() throws IOException {
        Utils.addNestedDir(this.testPath);
        var dirs = scanner.listFiles(this.testPath, true);
        assertEquals(4, dirs.size());
    }

    @Test
    public void testSummarisePath() throws IOException {
        PathSummary summary = scanner.summarise(this.testPath, false);
        assertEquals(PathSummary.of(this.testPath, 2, 18, 1, 0), summary);
    }

    @Test
    public void testSummarisePathRecursive() throws IOException {
        PathSummary summary = scanner.summarise(this.testPath, true);
        assertEquals(PathSummary.of(this.testPath, 3, 27, 1, 1), summary);
        Utils.addNestedDir(this.testPath);
        summary = scanner.summarise(this.testPath, true);
        assertEquals(PathSummary.of(this.testPath, 4, 36, 1, 1), summary);
    }

    @Test
    public void testScanPath() throws IOException, NoSuchAlgorithmException {
        @SuppressWarnings("null")
        PathScanResult result = scanner.scan(this.testPath, Hasher.instance(Collections.singleton(Algorithms.SHA_256)), false);
        assertEquals(PathSummary.of(this.testPath, 2, 18, 1, 0), result.getSummary());
    }

    @SuppressWarnings("null")
    @Test
    public void testScanPathRecursive() throws IOException, NoSuchAlgorithmException {
        PathScanResult result = scanner.scan(this.testPath, Hasher.instance(Collections.singleton(Algorithms.SHA_256)), true);
        assertEquals(PathSummary.of(this.testPath, 3, 27, 1, 1), result.getSummary());
        Utils.addNestedDir(this.testPath);
        result = scanner.scan(this.testPath, Hasher.instance(Collections.singleton(Algorithms.SHA_256)), true);
        assertEquals(PathSummary.of(this.testPath, 4, 36, 1, 1), result.getSummary());
    }

    @Test
    public void testScanHashRecursive() throws IOException, NoSuchAlgorithmException {
        Utils.addNestedDir(this.testPath);
        @SuppressWarnings("null")
        PathScanResult result = scanner.scan(this.testPath, Hasher.instance(Collections.singleton(Algorithms.SHA_256)), true);
        @SuppressWarnings("null")
        Hasher hasher = Hasher.instance(Collections.singleton(Algorithms.SHA_256));
        for (FileScanResult fsr : result.getResults()) {
            Path filePath = fsr.getPath();
            final String fileName = filePath.getFileName().toString();
            @SuppressWarnings("null")
            Set<DigestResult> expected = hasher.hash(fileName.getBytes(StandardCharsets.UTF_8));
            if (fsr.getStatus() != FileScanStatus.SCANNED) {
                continue;
            }
            assertEquals(expected, fsr.getDigestResults());
        }
        assertEquals(PathSummary.of(this.testPath, 4, 36, 1, 1), result.getSummary());
    }
}
