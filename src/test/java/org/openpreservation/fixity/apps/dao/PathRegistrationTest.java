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

public class PathRegistrationTest {
    Path testPathOne;
    Path testPathTwo;
    Collection collection;

    @Before
    public void setUp() throws IOException, SQLIntegrityConstraintViolationException {
        collection = DataManager.collectionDao().findByName("Test Collection").orElse(null);
        if (collection == null) collection = DataManager.collectionDao().create(Collection.of("Test Collection"));
        testPathOne = Files.createTempDirectory("fixity-pathreg-one");
        testPathTwo = Files.createTempDirectory("fixity-pathreg-two");
    }

    @After
    public void tearDown() throws IOException {
        if (testPathOne != null)  Utils.deleteDirectory(testPathOne.toFile());
        if (testPathTwo != null)  Utils.deleteDirectory(testPathTwo.toFile());
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterNullCollectionPath() throws IOException, SQLIntegrityConstraintViolationException {
        CollectionDao collectionDao = DataManager.collectionDao();
        collectionDao.registerCollectionPath(collection, null);
    }

    @Test
    public void testRegisterCollectionPath() throws IOException, SQLIntegrityConstraintViolationException {
        CollectionDao collectionDao = DataManager.collectionDao();
        int expectedSize = collection.getPathRegistrationsSize();
        collectionDao.registerCollectionPath(collection, testPathOne);
        Collection foundCollection = collectionDao.findById(this.collection.getId()).get();
        assertEquals(expectedSize + 1, foundCollection.getPathRegistrationsSize());
    }

    @Test(expected = SQLIntegrityConstraintViolationException.class)
    public void testRegisterDuplicateCollectionPath() throws IOException, SQLIntegrityConstraintViolationException {
        CollectionDao collectionDao = DataManager.collectionDao();
        int expectedSize = collection.getPathRegistrationsSize();
        collectionDao.registerCollectionPath(collection, testPathOne);
        assertEquals(expectedSize + 1, collection.getPathRegistrationsSize());
        collectionDao.registerCollectionPath(collection, testPathOne);
    }

    @Test(expected = SQLIntegrityConstraintViolationException.class)
    public void testDeregisterCollectionPath() throws SQLIntegrityConstraintViolationException, IOException {
        CollectionDao collectionDao = DataManager.collectionDao();
        final int expectedSize = collection.getPathRegistrationsSize() + 1;
        collectionDao.registerCollectionPath(collection, testPathOne);
        assertEquals(expectedSize, collection.getPathRegistrationsSize());
        collectionDao.deregisterCollectionPath(collection, testPathTwo);
        assertEquals(expectedSize, collection.getPathRegistrationsSize());
    }

    @Test(expected = SQLIntegrityConstraintViolationException.class)
    public void testAddDuplicateCollectionPath() throws SQLIntegrityConstraintViolationException {
        CollectionPathDao dao = DataManager.collectionPathDao();
        dao.create(CollectionPath.of(testPathOne));
        dao.create(CollectionPath.of(testPathOne));
    }

   @Test(expected = SQLIntegrityConstraintViolationException.class)
    public void testUpdateNewCollectionPaths() throws SQLIntegrityConstraintViolationException {
        DataManager.collectionPathDao().update(CollectionPath.of(testPathOne));
    }

    @Test
    public void testDeleteCollectionPaths() throws SQLIntegrityConstraintViolationException {
        CollectionPathDao dao = DataManager.collectionPathDao();
        dao.create(CollectionPath.of(testPathOne));
        CollectionPath toDelete = dao.create(CollectionPath.of(testPathTwo));
        List<CollectionPath> collections = dao.findAll();
        int expectedSize = collections.size();
        dao.delete(toDelete);
        collections = dao.findAll();
        assertEquals(expectedSize - 1, collections.size());
    }
}
