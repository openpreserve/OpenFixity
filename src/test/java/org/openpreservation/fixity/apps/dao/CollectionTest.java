package org.openpreservation.fixity.apps.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Random;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openpreservation.fixity.Utils;

import nl.jqno.equalsverifier.EqualsVerifier;

public class CollectionTest {
    static Path testPath;

    @BeforeClass
    public static void setUp() throws IOException {
        testPath = Files.createTempDirectory("fixity-coll-path");
    }

    @AfterClass
    public static void tearDown() throws IOException {
        Utils.deleteDirectory(testPath.toFile());
    }

    @SuppressWarnings("null")
    @Test
    public void testEquals() {
        EqualsVerifier.forClass(Collection.class).withIgnoredFields("pathRegistrations")
            .withPrefabValues(PathRegistration.class,
                              PathRegistration.of(Collection.of("Test Collection"),
                                                  CollectionPath.of(testPath)),
                              PathRegistration.of(Collection.of("Test Collection 2"),
                                                  CollectionPath.of(testPath)))
            .verify();
    }

    @Test
    public void testAddCollection() throws SQLIntegrityConstraintViolationException {
        Collection collection = DataManager.collectionDao().findByName("A simple collection").orElse(null);
        if (collection != null) {
            DataManager.collectionDao().update(collection);
            DataManager.collectionDao().delete(collection);
        }
        collection = Collection.of("A simple collection");
        Collection createdCollection = DataManager.collectionDao().create(collection);
        assertEquals(collection, createdCollection);
    }

    @Test(expected = SQLIntegrityConstraintViolationException.class)
    public void testAddDuplicateCollection() throws SQLIntegrityConstraintViolationException {
        CollectionDao dao = DataManager.collectionDao();
        dao.create(Collection.of("Test Collection"));
        dao.create(Collection.of("Test Collection"));
    }

    @Test(expected = SQLIntegrityConstraintViolationException.class)
    public void testUpdateNewCollections() throws SQLIntegrityConstraintViolationException {
        CollectionDao dao = DataManager.collectionDao();
        dao.update(Collection.of("Test Collection"));
    }

    @Test
    public void testDeleteCollections() throws SQLIntegrityConstraintViolationException, IOException {
        CollectionDao dao = DataManager.collectionDao();
        @SuppressWarnings("null")
        String name = new Random().ints(97, 123)
            .limit(40)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
        dao.create(Collection.of(name));
        Collection toDelete = dao.findByName(name).orElseThrow();;
        dao.registerCollectionPath(toDelete, testPath);
        assertTrue(toDelete.getPathRegistrationsSize() == 1);
        int initialSize = dao.findAll().size();
        dao.delete(dao.findByName(name).orElseThrow());
        assertEquals(initialSize - 1, dao.findAll().size());
    }
}
