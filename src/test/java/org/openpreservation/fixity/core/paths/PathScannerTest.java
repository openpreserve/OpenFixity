package org.openpreservation.fixity.core.paths;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openpreservation.fixity.Utils;
import org.openpreservation.fixity.core.digests.Algorithms;
import org.openpreservation.fixity.core.digests.DigestResult;
import org.openpreservation.fixity.core.digests.Hasher;

public class PathScannerTest {
    Path testPath;
    PathScanner scanner;

    @Before
    public void setUp() throws IOException, NoSuchAlgorithmException {
        testPath = Utils.createTempTestPath("fixity-pathsummary-tests");
        scanner = PathScanner.instance();
    }

    @After
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
        PathScanResult result = scanner.scan(this.testPath, Hasher.instance(Algorithms.SHA_256), false);
        assertEquals(PathSummary.of(this.testPath, 2, 18, 1, 0), result.getSummary());
    }

    @Test
    public void testScanPathRecursive() throws IOException, NoSuchAlgorithmException {
        PathScanResult result = scanner.scan(this.testPath, Hasher.instance(Algorithms.SHA_256), true);
        assertEquals(PathSummary.of(this.testPath, 3, 27, 1, 1), result.getSummary());
        Utils.addNestedDir(this.testPath);
        result = scanner.scan(this.testPath, Hasher.instance(Algorithms.SHA_256), true);
        assertEquals(PathSummary.of(this.testPath, 4, 36, 1, 1), result.getSummary());
    }

    @Test
    public void testScanHashRecursive() throws IOException, NoSuchAlgorithmException {
        Utils.addNestedDir(this.testPath);
        PathScanResult result = scanner.scan(this.testPath, Hasher.instance(Algorithms.SHA_256), true);
        Hasher hasher = Hasher.instance(Algorithms.SHA_256);
        for (FileScanResult fsr : result.getResults()) {
            if (fsr == null) continue;
            Path filePath = fsr.getPath();
            final String fileName = filePath.getFileName().toString();
            DigestResult expected = hasher.hash(fileName.getBytes(StandardCharsets.UTF_8));
            if (fsr.getStatus() != FileScanStatus.SCANNED) {
                continue;
            }
            assertEquals(expected, fsr.getDigestResults().iterator().next());
        }
        assertEquals(PathSummary.of(this.testPath, 4, 36, 1, 1), result.getSummary());
    }
}
