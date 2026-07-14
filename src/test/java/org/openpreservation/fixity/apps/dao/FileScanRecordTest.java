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
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpreservation.fixity.Utils;
import org.openpreservation.fixity.apps.schedule.BatchScanner;
import org.openpreservation.fixity.core.digests.Algorithms;
import org.openpreservation.fixity.core.digests.DigestResult;
import org.openpreservation.fixity.core.digests.Hasher;
import org.openpreservation.fixity.core.paths.FileScanResult;
import org.openpreservation.fixity.core.paths.FileScanStatus;

@SuppressWarnings("null")
public class FileScanRecordTest {
    Path testDir;
    Path file;

    @BeforeEach
    public void setUp() throws IOException {
        testDir = Files.createTempDirectory("filescanrecord-test");
        file = testDir.resolve("file.txt");
        Files.writeString(file, "original content");
    }

    @AfterEach
    public void tearDown() throws IOException {
        file.toFile().setReadable(true);
        Utils.deleteDirectory(testDir.toFile());
    }

    /** Scan testDir with the given algorithms; return the FileScanRecord for file.txt. */
    private FileScanRecord scanWith(final EnumSet<Algorithms> algorithms) throws IOException, NoSuchAlgorithmException {
        BatchScanner scanner = new BatchScanner();
        scanner.scan(testDir, Hasher.instance(algorithms), false);
        return scanner.getScan().getAllFiles().stream()
                .filter(r -> r.relativePath().equals("file.txt"))
                .findFirst().orElseThrow();
    }

    private FileScanRecord scan256() throws IOException, NoSuchAlgorithmException {
        return scanWith(EnumSet.of(Algorithms.SHA_256));
    }

    @Test
    public void testUnchangedFileIsVerified() throws IOException, NoSuchAlgorithmException {
        // SCANNED/SCANNED, matching digest → VERIFIED
        FileScanRecord baseline = scan256();
        FileScanRecord latest = scan256();
        assertEquals(PathAuditStatus.VERIFIED, latest.updateStatus(baseline));
    }

    @Test
    public void testModifiedFileIsChanged() throws IOException, NoSuchAlgorithmException {
        // SCANNED/SCANNED, digest mismatch → CHANGED
        FileScanRecord baseline = scan256();
        Files.writeString(file, "modified content");
        FileScanRecord latest = scan256();
        assertEquals(PathAuditStatus.CHANGED, latest.updateStatus(baseline));
    }

    @Test
    public void testMismatchedAlgorithmsIsUnverified() throws IOException, NoSuchAlgorithmException {
        // SCANNED/SCANNED, no matching algorithm between scans → UNVERIFIED
        FileScanRecord baseline = scanWith(EnumSet.of(Algorithms.SHA_512));
        FileScanRecord latest = scan256();
        assertEquals(PathAuditStatus.UNVERIFIED, latest.updateStatus(baseline));
    }

    @Test
    public void testPreviousChangedStatusPropagates() throws IOException, NoSuchAlgorithmException {
        // When previous.auditStatus is CHANGED, processIdenticalStatus returns CHANGED
        // without re-checking digests, even if the current digest matches the previous.
        FileScanRecord original = scan256();
        Files.writeString(file, "modified content");
        FileScanRecord changed = scan256();
        changed.updateStatus(original); // sets changed.auditStatus = CHANGED

        // Rescan with the same (modified) content — digests match 'changed', but
        // processIdenticalStatus short-circuits because previous.auditStatus == CHANGED.
        FileScanRecord rescan = scan256();
        assertEquals(PathAuditStatus.CHANGED, rescan.updateStatus(changed));
    }

    @Test
    public void testDeniedFileIsDenied() throws IOException, NoSuchAlgorithmException {
        // DENIED/SCANNED — different FileScanStatus → noPreviousStatus(DENIED) → DENIED
        FileScanRecord baseline = scan256();
        file.toFile().setReadable(false);
        FileScanRecord latest = scan256();
        assertEquals(FileScanStatus.DENIED, latest.getStatus());
        assertEquals(PathAuditStatus.DENIED, latest.updateStatus(baseline));
    }

    @Test
    public void testDamagedFileHasDamagedAuditStatusInitially() throws IOException, NoSuchAlgorithmException {
        // A first-scan record with DAMAGED FileScanStatus must start with DAMAGED audit status, not ADDED
        FileScanRecord baseline = scan256();
        PathScan pathScan = baseline.execution();
        FileScanResult damagedResult = damagedResult(file);
        FileScanRecord damaged = FileScanRecord.of(pathScan, damagedResult);
        assertEquals(PathAuditStatus.DAMAGED, damaged.getAuditStatus());
    }

    @Test
    public void testDamagedFileRemainsDAMAGEDOnSubsequentScan() throws IOException, NoSuchAlgorithmException {
        // If previous scan had DAMAGED audit status, the next scan's DAMAGED record must inherit DAMAGED not ADDED
        FileScanRecord baseline = scan256();
        PathScan pathScan = baseline.execution();
        FileScanResult damagedResult = damagedResult(file);
        FileScanRecord previous = FileScanRecord.of(pathScan, damagedResult);
        FileScanRecord latest = FileScanRecord.of(pathScan, damagedResult);
        assertEquals(PathAuditStatus.DAMAGED, latest.updateStatus(previous));
    }

    private static FileScanResult damagedResult(final Path path) {
        return damagedResult(path, Set.of());
    }

    private static FileScanResult damagedResult(final Path path, final Set<? extends DigestResult> digests) {
        return new FileScanResult() {
            @Override public Path getPath() { return path; }
            @Override public long getLength() { return 0L; }
            @Override public LocalDateTime getCreated() { return null; }
            @Override public LocalDateTime getModified() { return null; }
            @Override public Set<? extends DigestResult> getDigestResults() { return digests; }
            @Override public FileScanStatus getStatus() { return FileScanStatus.DAMAGED; }
            @SuppressWarnings("null")
            @Override public LocalDateTime getScanned() { return LocalDateTime.now(); }
        };
    }

    @Test
    public void testRecoveredDamagedFileIsNotReportedAsAdded() throws IOException, NoSuchAlgorithmException {
        // DAMAGED → SCANNED. The file was unreadable and is readable again. It is not a new
        // file, so it must not be reported as ADDED. A damaged record carries no digests, so
        // there is no baseline to compare against and UNVERIFIED is the honest answer.
        FileScanRecord baseline = scan256();
        FileScanRecord damaged = FileScanRecord.of(baseline.execution(), damagedResult(file));
        FileScanRecord latest = scan256();
        assertEquals(PathAuditStatus.UNVERIFIED, latest.updateStatus(damaged));
    }

    @Test
    public void testRecoveredDamagedFileWithRetainedDigestIsVerified() throws IOException, NoSuchAlgorithmException {
        // DAMAGED → SCANNED, where the damaged record retained a digest. Content is unchanged,
        // so the digest comparison must be reached and must yield VERIFIED, not ADDED.
        FileScanRecord good = scan256();
        FileScanRecord damaged = FileScanRecord.of(good.execution(), damagedResult(file, good.getDigestResults()));
        FileScanRecord latest = scan256();
        assertEquals(PathAuditStatus.VERIFIED, latest.updateStatus(damaged));
    }

    @Test
    public void testFileChangedWhileDamagedIsReportedAsChanged() throws IOException, NoSuchAlgorithmException {
        // The case the old code got wrong. A file was damaged, its content changed, and it is
        // readable again. The old fallthrough reported ADDED, masking a real change as a new
        // file. It must be reported as CHANGED.
        FileScanRecord good = scan256();
        FileScanRecord damaged = FileScanRecord.of(good.execution(), damagedResult(file, good.getDigestResults()));
        Files.writeString(file, "content altered while the file was unreadable");
        FileScanRecord latest = scan256();
        assertEquals(PathAuditStatus.CHANGED, latest.updateStatus(damaged));
    }

    @Test
    public void testAbsentThenPresentFileIsStillAdded() throws IOException, NoSuchAlgorithmException {
        // NOTFOUND → SCANNED. The file genuinely did not exist before, so ADDED is correct and
        // must survive the fallthrough change above.
        FileScanRecord baseline = scan256();
        PathScan scan = baseline.execution();
        Files.delete(file);
        FileScanResult notFound = FileScanResult.of(file, Hasher.instance(EnumSet.of(Algorithms.SHA_256)));
        FileScanRecord previous = FileScanRecord.of(scan, notFound);

        Files.writeString(file, "original content");
        FileScanRecord latest = scan256();
        assertEquals(PathAuditStatus.ADDED, latest.updateStatus(previous));
    }

    @Test
    public void testNotFoundFileIsNotFound() throws IOException, NoSuchAlgorithmException {
        // NOTFOUND/SCANNED — different FileScanStatus → noPreviousStatus(NOTFOUND) → NOTFOUND
        // BatchScanner only scans existing files, so a NOTFOUND record must be constructed
        // manually using FileScanResult.of() on a non-existent path.
        FileScanRecord baseline = scan256();
        PathScan scan = new BatchScanner().getScan(); // reuse scan context after scanning above
        // Actually we need a valid PathScan — get it from the baseline scan
        scan = baseline.execution();

        Files.delete(file);
        FileScanResult notFoundResult = FileScanResult.of(file, Hasher.instance(EnumSet.of(Algorithms.SHA_256)));
        assertEquals(FileScanStatus.NOTFOUND, notFoundResult.getStatus());

        FileScanRecord notFoundRecord = FileScanRecord.of(scan, notFoundResult);
        assertEquals(PathAuditStatus.NOTFOUND, notFoundRecord.updateStatus(baseline));
    }
}
