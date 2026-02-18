package org.openpreservation.fixity.apps.dao;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openpreservation.fixity.Utils;
import org.openpreservation.fixity.core.digests.Algorithms;
import org.openpreservation.fixity.core.digests.Hasher;
import org.openpreservation.fixity.core.paths.FileScanResult;
import org.openpreservation.fixity.core.paths.PathScanner;
import org.openpreservation.fixity.core.paths.PathSummary;

public class PathScanTest {
    Path testDirPath;
    CollectionPath collectionPath;

    @Before
    public void setUp() throws IOException, SQLIntegrityConstraintViolationException, NoSuchAlgorithmException {
        testDirPath = Utils.createTempTestPath("fixity-scan-record-test-dir");
        DataManager.collectionPathDao().create(CollectionPath.of(testDirPath));
        collectionPath = DataManager.collectionPathDao().findByRoot(testDirPath).get();
    }

    @After
    public void tearDown() throws IOException {
//        Files.deleteIfExists(testFileOnePath);
        Utils.deleteDirectory(testDirPath.toFile());
    }


    @Test
    public void testAddScan() throws IOException, SQLIntegrityConstraintViolationException, NoSuchAlgorithmException {
        final Path itemPath = testDirPath.resolve("file1.txt");
        PathScan pathScan = collectionPath.createPathScan(PathSummary.of(itemPath, false));
        FileScanResult scanResult = FileScanResult.of(itemPath, Hasher.instance(Algorithms.SHA_256));
        final int expectedRecords = DataManager.fileScanRecordDao().findAll().size() + 1;
        DataManager.pathScanDao().create(pathScan);
        DataManager.fileScanRecordDao().create(FileScanRecord.of(pathScan, scanResult));
        List<FileScanRecord> records = DataManager.fileScanRecordDao().findAll();
        assertEquals(expectedRecords, records.size());
    }

    @Test
    public void testMultipleScanItems() throws IOException, SQLIntegrityConstraintViolationException, NoSuchAlgorithmException {
        // final Path itemPath = testDirPath.resolve("file1.txt");
        final int expectedRecords = DataManager.fileScanRecordDao().findAll().size() + 3;
        PathScan pathScan = collectionPath.createPathScan(PathSummary.of(testDirPath, true));
        pathScan.updateFrom(PathScanner.instance().scan(testDirPath, Hasher.instance(Algorithms.SHA_256), true));
        DataManager.pathScanDao().create(pathScan);
        assertEquals(expectedRecords, DataManager.fileScanRecordDao().findAll().size());
    }

    @Test
    public void testMultipleScans() throws IOException, SQLIntegrityConstraintViolationException, NoSuchAlgorithmException {
        // final Path itemPath = testDirPath.resolve("file1.txt");
        final int expectedRecords = DataManager.fileScanRecordDao().findAll().size() + 3;
        final int expectedScans = DataManager.pathScanDao().findAll().size() + 2;
        PathScan pathScan = collectionPath.createPathScan(PathSummary.of(testDirPath, true));
        pathScan.updateFrom(PathScanner.instance().scan(testDirPath, Hasher.instance(Algorithms.SHA_256), true));
        DataManager.pathScanDao().create(pathScan);
        assertEquals(expectedRecords, DataManager.fileScanRecordDao().findAll().size());
        pathScan = collectionPath.createPathScan(PathSummary.of(testDirPath, true));
        pathScan.updateFrom(PathScanner.instance().scan(testDirPath, Hasher.instance(Algorithms.SHA_256), true));
        DataManager.pathScanDao().create(pathScan);
        assertEquals(expectedRecords + 3, DataManager.fileScanRecordDao().findAll().size());
        assertEquals(expectedScans, DataManager.pathScanDao().findAll().size());
    }
}
