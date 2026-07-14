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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpreservation.fixity.Utils;
import org.openpreservation.fixity.core.paths.PathSummary;

public class FolderScanRecordTest {
    Path testDir;
    PathScan pathScan;

    @BeforeEach
    public void setUp() throws IOException {
        testDir = Files.createTempDirectory("folder-scan-record-test");
        pathScan = CollectionPath.of(testDir).createPathScan(PathSummary.of(testDir));
    }

    @AfterEach
    public void tearDown() throws IOException {
        Utils.deleteDirectory(testDir.toFile());
    }

    // --- FolderScanRecord.of() ---

    @Test
    public void testOfReturnsNonNull() {
        assertNotNull(FolderScanRecord.of(pathScan, "subdir"));
    }

    @Test
    public void testOfWithNullPathScanThrows() {
        assertThrows(NullPointerException.class, () -> FolderScanRecord.of(null, "subdir"));
    }

    @Test
    public void testOfWithNullRelativePathThrows() {
        assertThrows(NullPointerException.class, () -> FolderScanRecord.of(pathScan, null));
    }

    // --- getters ---

    @Test
    public void testGetRelativePathReturnsConstructedPath() {
        FolderScanRecord record = FolderScanRecord.of(pathScan, "subdir/nested");
        assertEquals("subdir/nested", record.getRelativePath());
    }

    @Test
    public void testGetRelativePathReturnsEmptyStringForRoot() {
        FolderScanRecord record = FolderScanRecord.of(pathScan, "");
        assertEquals("", record.getRelativePath());
    }

    @Test
    public void testGetPathScanReturnsConstructedPathScan() {
        FolderScanRecord record = FolderScanRecord.of(pathScan, "subdir");
        assertEquals(pathScan, record.getPathScan());
    }

    @Test
    public void testGetFilesReturnsEmptySetOnNewInstance() {
        FolderScanRecord record = FolderScanRecord.of(pathScan, "subdir");
        assertTrue(record.getFiles().isEmpty());
    }

    @Test
    public void testGetIdNullBeforePersistence() {
        FolderScanRecord record = FolderScanRecord.of(pathScan, "subdir");
        assertNull(record.getId());
    }
}
