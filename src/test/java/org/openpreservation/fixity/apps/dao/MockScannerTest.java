package org.openpreservation.fixity.apps.dao;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openpreservation.fixity.Utils;
import org.openpreservation.fixity.core.digests.Algorithms;
import org.openpreservation.fixity.core.digests.Hasher;

public class MockScannerTest {
    static Path testDirPath;
    static CollectionPath collectionPath;

    @BeforeClass
    public static void setUp() throws IOException, SQLIntegrityConstraintViolationException, NoSuchAlgorithmException {
        testDirPath = Utils.createTempTestPath("fixity-scan-record-test-dir");
        DataManager.collectionPathDao().create(CollectionPath.of(testDirPath));
        collectionPath = DataManager.collectionPathDao().findByRoot(testDirPath).get();
    }


    @AfterClass
    public static void tearDown() throws IOException {
        Utils.deleteDirectory(testDirPath.toFile());
    }


    @Test
    public void testScan() throws IOException, SQLIntegrityConstraintViolationException, NoSuchAlgorithmException {
        MockScanner scanner = new MockScanner();
        final int expectedRecords = DataManager.fileScanRecordDao().findAll().size() + 2;
        scanner.scan(testDirPath, Hasher.instance(Algorithms.SHA_256), false);
        List<FileScanRecord> records = DataManager.fileScanRecordDao().findAll();
        assertEquals(expectedRecords, records.size());
    }
}
