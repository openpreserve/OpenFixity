package org.openpreservation.fixity.apps.dao;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openpreservation.fixity.Utils;

public class CollectionPathTest {
    Path testDirPathOne;
    Path testDirPathTwo;
    Path testFilePathOne;
    Path testFilePathTwo;

    @Before
    public void setUp() throws IOException {
        testDirPathOne = Files.createTempDirectory("fixity-col-path-one");
        testFilePathOne = Files.createTempFile(testDirPathOne, "file1", ".txt");
        testDirPathTwo = Files.createTempDirectory("fixity-col-path-two");
        testFilePathTwo = Files.createTempFile(testDirPathTwo, "file2", ".txt");
    }

    @After
    public void tearDown() throws IOException {
        Utils.deleteDirectory(testDirPathOne.toFile());
        Utils.deleteDirectory(testDirPathTwo.toFile());
    }

    @Test
    public void testAddCollectionPath() throws IOException, SQLIntegrityConstraintViolationException {
        CollectionPath collectionPath = CollectionPath.of(testDirPathOne);
        CollectionPathDao dao = DataManager.collectionPathDao();
        CollectionPath createdPath = dao.create(collectionPath);
        assertEquals(collectionPath, createdPath);
    }

    @Test(expected = SQLIntegrityConstraintViolationException.class)
    public void testAddDuplicateCollectionPath() throws SQLIntegrityConstraintViolationException {
        CollectionPathDao dao = DataManager.collectionPathDao();
        dao.create(CollectionPath.of(testDirPathOne));
        dao.create(CollectionPath.of(testDirPathOne));
    }

    @Test(expected = SQLIntegrityConstraintViolationException.class)
    public void testUpdateNewCollectionPaths() throws SQLIntegrityConstraintViolationException {
        CollectionPathDao dao = DataManager.collectionPathDao();
        dao.update(CollectionPath.of(testDirPathOne));
    }

    @Test
    public void testDeleteCollectionPaths() throws SQLIntegrityConstraintViolationException {
        CollectionPathDao dao = DataManager.collectionPathDao();
        dao.create(CollectionPath.of(testDirPathOne));
        CollectionPath toDelete = dao.create(CollectionPath.of(testDirPathTwo));
        List<CollectionPath> collections = dao.findAll();
        int size = collections.size();
        dao.delete(toDelete);
        collections = dao.findAll();
        assertEquals(size - 1, collections.size());
    }
}
