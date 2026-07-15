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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpreservation.fixity.Utils;
import org.openpreservation.fixity.core.paths.PathSummary;

@SuppressWarnings("null")
public class PathSummaryRecordTest {
    Path testDir;

    @BeforeEach
    public void setUp() throws IOException {
        testDir = Utils.createTempTestPath("path-summary-record-test");
    }

    @AfterEach
    public void tearDown() throws IOException {
        Utils.deleteDirectory(testDir);
    }

    // --- humanReadableByteCountBin ---

    @Test
    public void testBytesBelow1024() {
        assertEquals("0 B",    PathSummaryRecord.humanReadableByteCountBin(0));
        assertEquals("1 B",    PathSummaryRecord.humanReadableByteCountBin(1));
        assertEquals("1023 B", PathSummaryRecord.humanReadableByteCountBin(1023));
    }

    @Test
    public void testKiB() {
        assertEquals("1.0 KiB", PathSummaryRecord.humanReadableByteCountBin(1024));
    }

    @Test
    public void testFractionalKiB() {
        assertEquals("1.5 KiB", PathSummaryRecord.humanReadableByteCountBin(1536));
    }

    @Test
    public void testMiB() {
        assertEquals("1.0 MiB", PathSummaryRecord.humanReadableByteCountBin(1024 * 1024));
    }

    @Test
    public void testGiB() {
        assertEquals("1.0 GiB", PathSummaryRecord.humanReadableByteCountBin(1024 * 1024 * 1024));
    }

    @Test
    public void testNegativeBytes() {
        assertEquals("-1.0 KiB", PathSummaryRecord.humanReadableByteCountBin(-1024));
    }

    @Test
    public void testLongMinValue() {
        // Long.MIN_VALUE is handled without overflow via the Math.abs guard;
        // signum(-) preserves the sign so the result is negative EiB.
        assertEquals("-8.0 EiB", PathSummaryRecord.humanReadableByteCountBin(Long.MIN_VALUE));
    }

    // --- of(PathSummary) ---

    @Test
    public void testOfPathSummaryCopiesAllFields() {
        PathSummary summary = PathSummary.of(testDir, 10L, 2048L, 2L, 1L);
        PathSummaryRecord record = PathSummaryRecord.of(summary);
        assertEquals(10L, record.getTotalFiles());
        assertEquals(2048L, record.getTotalBytes());
        assertEquals(2L,  record.getTotalUnreadableDirectories());
        assertEquals(1L,  record.getTotalUnreadableFiles());
    }

    // --- of(long, long, long, long) package-private ---

    @Test
    public void testOfLongsReturnsCorrectValues() {
        PathSummaryRecord record = PathSummaryRecord.of(5L, 1024L, 1L, 0L);
        assertEquals(5L,    record.getTotalFiles());
        assertEquals(1024L, record.getTotalBytes());
        assertEquals(1L,    record.getTotalUnreadableDirectories());
        assertEquals(0L,    record.getTotalUnreadableFiles());
    }

    // --- empty() package-private ---

    @Test
    public void testEmptyReturnsZeroValues() {
        PathSummaryRecord record = PathSummaryRecord.empty();
        assertEquals(0L, record.getTotalFiles());
        assertEquals(0L, record.getTotalBytes());
        assertEquals(0L, record.getTotalUnreadableDirectories());
        assertEquals(0L, record.getTotalUnreadableFiles());
    }

    // --- of(Path) ---

    @Test
    public void testOfPathReturnsPopulatedSummary() throws FileNotFoundException {
        PathSummaryRecord record = PathSummaryRecord.of(testDir);
        // createTempTestPath: 3 readable files (file1, file2, dir1/file3 unreadable),
        // 1 file in unreadable dir2; totalFiles = 3 (file1, file2, file3 in dir1)
        // dir2 is unreadable so 1 unreadable directory; file3 is unreadable so 1 unreadable file
        assertEquals(3L, record.getTotalFiles());
        assertEquals(1L, record.getTotalUnreadableDirectories());
        assertEquals(1L, record.getTotalUnreadableFiles());
    }

    // --- getFormattedTotalBytes ---

    @Test
    public void testGetFormattedTotalBytes() {
        PathSummaryRecord record = PathSummaryRecord.of(1L, 1024L, 0L, 0L);
        assertEquals("1.0 KiB", record.getFormattedTotalBytes());
    }

    // --- getPath throws without associated PathScan ---

    @Test
    public void testGetPathThrowsWithoutPathScan() {
        PathSummaryRecord record = PathSummaryRecord.of(1L, 0L, 0L, 0L);
        assertThrows(IllegalStateException.class, record::getPath);
    }

}
