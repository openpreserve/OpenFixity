package org.openpreservation.fixity.apps.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpreservation.fixity.Utils;
import org.openpreservation.fixity.core.paths.PathSummary;

import jakarta.persistence.NoResultException;

@SuppressWarnings("null")
public class CollectionPathTest {
    Path testDirPathOne;
    Path testDirPathTwo;
    Path testFile;

    @BeforeEach
    public void setUp() throws IOException {
        TestSessionFactory.beginTransaction();
        testDirPathOne = Files.createTempDirectory("fixity-col-path-one");
        testDirPathTwo = Files.createTempDirectory("fixity-col-path-two");
        testFile = testDirPathOne.resolve("file.txt");
        Files.writeString(testFile, "content");
    }

    @AfterEach
    public void tearDown() throws IOException {
        TestSessionFactory.rollback();
        Utils.deleteDirectory(testDirPathOne.toFile());
        Utils.deleteDirectory(testDirPathTwo.toFile());
    }

    // --- CollectionPath.of() factory guards ---

    @Test
    public void testOfPathRejectsFile() {
        assertThrows(IllegalArgumentException.class, () -> CollectionPath.of(testFile));
    }

    @Test
    public void testOfPathRejectsNonExistentPath() {
        assertThrows(IllegalArgumentException.class,
                () -> CollectionPath.of(testDirPathOne.resolve("no-such-dir")));
    }

    @Test
    public void testOfStringRejectsFile() {
        assertThrows(IllegalArgumentException.class, () -> CollectionPath.of(testFile.toString()));
    }

    @Test
    public void testOfStringRejectsNonExistentPath() {
        assertThrows(IllegalArgumentException.class,
                () -> CollectionPath.of(testDirPathOne.resolve("no-such-dir").toString()));
    }

    // --- Domain accessors ---

    @Test
    public void testGetRootReturnsAbsolutePath() {
        CollectionPath cp = CollectionPath.of(testDirPathOne);
        assertEquals(testDirPathOne.toAbsolutePath().normalize(), cp.getRoot());
    }

    @Test
    public void testGetNameReturnsLastPathComponent() {
        CollectionPath cp = CollectionPath.of(testDirPathOne);
        assertEquals(testDirPathOne.getFileName().toString(), cp.getName());
    }

    @Test
    public void testGetNameOnFilesystemRootDoesNotThrow() {
        // A filesystem root ("/", "C:\", a mounted volume) has no file name component and
        // Path.getFileName() returns null there. Registering removable media or a network
        // mount by its root is normal usage, so getName() must fall back to the root itself
        // rather than dereferencing null.
        Path root = testDirPathOne.getRoot();
        assertNotNull(root);
        CollectionPath cp = CollectionPath.of(root);
        assertEquals(root.toString(), cp.getName());
    }

    @Test
    public void testGetFullPathReturnsAbsoluteString() {
        CollectionPath cp = CollectionPath.of(testDirPathOne);
        assertEquals(testDirPathOne.toAbsolutePath().normalize().toString(), cp.getFullPath());
    }

    @Test
    public void testGetAddedIsToday() {
        CollectionPath cp = CollectionPath.of(testDirPathOne);
        assertEquals(LocalDate.now(), cp.getAdded());
    }

    // --- Scan management ---

    @Test
    public void testNewCollectionPathHasNoScans() {
        assertTrue(CollectionPath.of(testDirPathOne).getPathScans().isEmpty());
    }

    @Test
    public void testGetLatestScanThrowsWithNoScans() {
        assertThrows(NoResultException.class, () -> CollectionPath.of(testDirPathOne).getLatestScan());
    }

    @Test
    public void testCreatePathScanAddsToScans() {
        CollectionPath cp = CollectionPath.of(testDirPathOne);
        cp.createPathScan(PathSummary.of(testDirPathOne));
        assertEquals(1, cp.getPathScans().size());
    }

    @Test
    public void testGetLatestScanReturnsNewestScan() throws InterruptedException {
        CollectionPath cp = CollectionPath.of(testDirPathOne);
        cp.createPathScan(PathSummary.of(testDirPathOne));
        // Small sleep so the second scan has a strictly later started timestamp
        Thread.sleep(5);
        PathScan second = cp.createPathScan(PathSummary.of(testDirPathOne));
        assertEquals(second, cp.getLatestScan());
    }

    @Test
    public void testAddPathScanSetsCollectionPathOnScan() {
        CollectionPath cp = CollectionPath.of(testDirPathOne);
        PathScan scan = PathScan.of(cp, PathSummaryRecord.empty());
        // create a fresh CollectionPath to add the scan to
        CollectionPath cp2 = CollectionPath.of(testDirPathTwo);
        cp2.addPathScan(scan);
        assertEquals(cp2, scan.getCollectionPath());
        assertTrue(cp2.getPathScans().contains(scan));
    }

    // --- Registration filtering ---

    @Test
    public void testNewCollectionPathHasNoRegistrations() throws SQLIntegrityConstraintViolationException {
        CollectionPath cp = TestSessionFactory.dataFactory().collectionPathDAO().create(CollectionPath.of(testDirPathOne));
        assertTrue(cp.getPathRegistrations().isEmpty());
    }

    @Test
    public void testGetRegisteredPathsExcludesDeregistered() throws SQLIntegrityConstraintViolationException {
        // Test the in-memory filtering logic directly without relying on lazy-load reload
        CollectionPath cp = CollectionPath.of(testDirPathOne);
        Collection col = TestSessionFactory.dataFactory().collectionDAO().create("RegFilterTest");
        PathRegistration active = PathRegistration.of(col, cp);
        PathRegistration deregistered = PathRegistration.of(col, cp);
        deregistered.deregister();
        cp.register(active);
        cp.register(deregistered);
        assertEquals(1, cp.getRegisteredPaths().size());
        assertEquals(1, cp.getDeRegisteredPaths().size());
    }

    // --- CollectionPathDAO ---

    @Test
    public void testAddCollectionPath() throws SQLIntegrityConstraintViolationException {
        CollectionPathDAO dao = TestSessionFactory.dataFactory().collectionPathDAO();
        CollectionPath collectionPath = CollectionPath.of(testDirPathOne);
        CollectionPath created = dao.create(collectionPath);
        assertEquals(collectionPath, created);
    }

    @Test
    public void testAddDuplicateCollectionPath() {
        assertThrows(SQLIntegrityConstraintViolationException.class, () -> {
            CollectionPathDAO dao = TestSessionFactory.dataFactory().collectionPathDAO();
            dao.create(CollectionPath.of(testDirPathOne));
            dao.create(CollectionPath.of(testDirPathOne));
        });
    }

    @Test
    public void testCreateThrowsForNonNullId() throws SQLIntegrityConstraintViolationException {
        CollectionPath cp = TestSessionFactory.dataFactory().collectionPathDAO()
                .create(CollectionPath.of(testDirPathOne));
        assertThrows(SQLIntegrityConstraintViolationException.class,
                () -> TestSessionFactory.dataFactory().collectionPathDAO().create(cp));
    }

    @Test
    public void testFindByIdReturnsCreatedPath() throws SQLIntegrityConstraintViolationException {
        CollectionPathDAO dao = TestSessionFactory.dataFactory().collectionPathDAO();
        CollectionPath created = dao.create(CollectionPath.of(testDirPathOne));
        assertNotNull(created.getId());
        assertEquals(created.getId(), dao.findById(created.getId()).getId());
    }

    @Test
    public void testFindByIdThrowsForNonExistentId() {
        assertThrows(NoResultException.class,
                () -> TestSessionFactory.dataFactory().collectionPathDAO().findById(99999L));
    }

    @Test
    public void testFindByRootStringReturnsPath() throws SQLIntegrityConstraintViolationException {
        CollectionPathDAO dao = TestSessionFactory.dataFactory().collectionPathDAO();
        CollectionPath created = dao.create(CollectionPath.of(testDirPathOne));
        CollectionPath found = dao.findByRoot(created.getFullPath());
        assertEquals(created.getId(), found.getId());
    }

    @Test
    public void testFindByRootPathReturnsPath() throws SQLIntegrityConstraintViolationException {
        CollectionPathDAO dao = TestSessionFactory.dataFactory().collectionPathDAO();
        CollectionPath created = dao.create(CollectionPath.of(testDirPathOne));
        CollectionPath found = dao.findByRoot(testDirPathOne);
        assertEquals(created.getId(), found.getId());
    }

    @Test
    public void testGetOrCreateReturnsExistingById() throws SQLIntegrityConstraintViolationException {
        CollectionPathDAO dao = TestSessionFactory.dataFactory().collectionPathDAO();
        CollectionPath created = dao.create(CollectionPath.of(testDirPathOne));
        CollectionPath fetched = dao.getOrCreate(created);
        assertEquals(created.getId(), fetched.getId());
    }

    @Test
    public void testGetOrCreateCreatesWhenAbsent() throws SQLIntegrityConstraintViolationException {
        CollectionPathDAO dao = TestSessionFactory.dataFactory().collectionPathDAO();
        int before = dao.findAll().size();
        dao.getOrCreate(CollectionPath.of(testDirPathOne));
        assertEquals(before + 1, dao.findAll().size());
    }

    @Test
    public void testUpdateNewCollectionPaths() {
        assertThrows(SQLIntegrityConstraintViolationException.class, () ->
            TestSessionFactory.dataFactory().collectionPathDAO().update(CollectionPath.of(testDirPathOne)));
    }

    @Test
    public void testDeleteCollectionPaths() throws SQLIntegrityConstraintViolationException {
        CollectionPathDAO dao = TestSessionFactory.dataFactory().collectionPathDAO();
        dao.create(CollectionPath.of(testDirPathOne));
        CollectionPath toDelete = dao.create(CollectionPath.of(testDirPathTwo));
        int size = dao.findAll().size();
        dao.delete(toDelete.getId());
        List<CollectionPath> remaining = dao.findAll();
        assertEquals(size - 1, remaining.size());
    }

    @Test
    public void testDeleteByCollectionPathObject() throws SQLIntegrityConstraintViolationException {
        CollectionPathDAO dao = TestSessionFactory.dataFactory().collectionPathDAO();
        CollectionPath toDelete = dao.create(CollectionPath.of(testDirPathOne));
        int size = dao.findAll().size();
        dao.delete(toDelete);
        assertEquals(size - 1, dao.findAll().size());
    }

    @Test
    public void testDeleteByNonExistentIdThrows() {
        assertThrows(SQLIntegrityConstraintViolationException.class,
                () -> TestSessionFactory.dataFactory().collectionPathDAO().delete(99999L));
    }
}
