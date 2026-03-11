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
        Utils.deleteDirectory(testDirPath.toFile());
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
