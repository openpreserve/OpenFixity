package org.openpreservation.fixity.apps.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLIntegrityConstraintViolationException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpreservation.fixity.Utils;

import jakarta.persistence.NoResultException;

@SuppressWarnings("null")
public class PathRegistrationTest {
    Path testPathOne;
    Path testPathTwo;
    Collection collection;
    CollectionPath collectionPath;

    @BeforeEach
    public void setUp() throws IOException, SQLIntegrityConstraintViolationException {
        TestSessionFactory.beginTransaction();
        testPathOne = Files.createTempDirectory("fixity-pathreg-one");
        testPathTwo = Files.createTempDirectory("fixity-pathreg-two");
        collection = TestSessionFactory.dataFactory().collectionDAO().create("Test Collection");
        collectionPath = TestSessionFactory.dataFactory().collectionPathDAO().create(CollectionPath.of(testPathOne));
    }

    @AfterEach
    public void tearDown() throws IOException {
        TestSessionFactory.rollback();
        Utils.deleteDirectory(testPathOne.toFile());
        Utils.deleteDirectory(testPathTwo.toFile());
    }

    // --- PathRegistration domain behaviour ---

    @Test
    public void testNewRegistrationIsRegistered() {
        assertTrue(PathRegistration.of(collection, collectionPath).isRegistered());
    }

    @Test
    public void testNewRegistrationIsNotDeRegistered() {
        assertFalse(PathRegistration.of(collection, collectionPath).isDeRegistered());
    }

    @Test
    public void testRegisteredAtIsNotNull() {
        assertNotNull(PathRegistration.of(collection, collectionPath).getRegisteredAt());
    }

    @Test
    public void testDeregisteredAtIsNullInitially() {
        assertNull(PathRegistration.of(collection, collectionPath).getDeregisteredAt());
    }

    @Test
    public void testDeregisterSetsDeregisteredAt() {
        PathRegistration reg = PathRegistration.of(collection, collectionPath);
        reg.deregister();
        assertNotNull(reg.getDeregisteredAt());
    }

    @Test
    public void testAfterDeregisterIsDeRegistered() {
        PathRegistration reg = PathRegistration.of(collection, collectionPath);
        reg.deregister();
        assertTrue(reg.isDeRegistered());
    }

    @Test
    public void testAfterDeregisterIsNotRegistered() {
        PathRegistration reg = PathRegistration.of(collection, collectionPath);
        reg.deregister();
        assertFalse(reg.isRegistered());
    }

    @Test
    public void testGetCollectionReturnsCorrectCollection() {
        assertEquals(collection, PathRegistration.of(collection, collectionPath).getCollection());
    }

    @Test
    public void testGetCollectionPathReturnsCorrectPath() {
        assertEquals(collectionPath, PathRegistration.of(collection, collectionPath).getCollectionPath());
    }

    // --- PathRegistrationDAO ---

    @Test
    public void testRegisterNullCollectionPath() {
        assertThrows(NullPointerException.class, () ->
            TestSessionFactory.dataFactory().pathRegistrationDAO().register(collection, (CollectionPath) null));
    }

    @Test
    public void testRegisterCollectionPath() throws SQLIntegrityConstraintViolationException {
        PathRegistrationDAO dao = TestSessionFactory.dataFactory().pathRegistrationDAO();
        int before = dao.findAll().size();
        dao.register(collection, collectionPath);
        assertEquals(before + 1, dao.findAll().size());
    }

    @Test
    public void testRegisterDuplicateCollectionPath() {
        assertThrows(SQLIntegrityConstraintViolationException.class, () -> {
            PathRegistrationDAO dao = TestSessionFactory.dataFactory().pathRegistrationDAO();
            dao.register(collection, collectionPath);
            dao.register(collection, collectionPath);
        });
    }

    @Test
    public void testFindByIdReturnsRegistration() throws SQLIntegrityConstraintViolationException {
        PathRegistrationDAO dao = TestSessionFactory.dataFactory().pathRegistrationDAO();
        PathRegistration created = dao.register(collection, collectionPath);
        assertNotNull(created.id());
        assertEquals(created.id(), dao.findById(created.id()).id());
    }

    @Test
    public void testFindByIdThrowsForNonExistentId() {
        assertThrows(NoResultException.class,
                () -> TestSessionFactory.dataFactory().pathRegistrationDAO().findById(99999L));
    }

    @Test
    public void testFindCurrentByPathReturnsActiveRegistration() throws SQLIntegrityConstraintViolationException {
        PathRegistrationDAO dao = TestSessionFactory.dataFactory().pathRegistrationDAO();
        dao.register(collection, collectionPath);
        PathRegistration found = dao.findCurrentByPath(collection, collectionPath);
        assertEquals(collection.getId(), found.getCollection().getId());
        assertEquals(collectionPath.getId(), found.getCollectionPath().getId());
    }

    @Test
    public void testDeregisterSetsDeregisteredAtInDatabase() throws SQLIntegrityConstraintViolationException {
        PathRegistrationDAO dao = TestSessionFactory.dataFactory().pathRegistrationDAO();
        dao.register(collection, collectionPath);
        PathRegistration deregistered = dao.deregister(collection, collectionPath);
        assertNotNull(deregistered.getDeregisteredAt());
        assertTrue(deregistered.isDeRegistered());
    }

    @Test
    public void testDeregisterUnregisteredPathThrows() throws IOException, SQLIntegrityConstraintViolationException {
        CollectionPath otherPath = TestSessionFactory.dataFactory().collectionPathDAO()
                .create(CollectionPath.of(testPathTwo));
        PathRegistrationDAO dao = TestSessionFactory.dataFactory().pathRegistrationDAO();
        dao.register(collection, collectionPath);
        assertThrows(SQLIntegrityConstraintViolationException.class,
                () -> dao.deregister(collection, otherPath));
    }
}
