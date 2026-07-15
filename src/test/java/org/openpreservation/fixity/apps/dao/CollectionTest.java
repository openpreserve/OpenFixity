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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Random;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpreservation.fixity.Utils;

import jakarta.persistence.NoResultException;
import nl.jqno.equalsverifier.EqualsVerifier;

@SuppressWarnings("null")
public class CollectionTest {
    static Path testPath;

    @BeforeAll
    public static void setUp() throws IOException {
        testPath = Files.createTempDirectory("fixity-coll-path");
    }

    @AfterAll
    public static void tearDown() throws IOException {
        Utils.deleteDirectory(testPath);
    }

    @BeforeEach
    public void beginTx() {
        TestSessionFactory.beginTransaction();
    }

    @AfterEach
    public void rollback() {
        TestSessionFactory.rollback();
    }

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
        CollectionDAO dao = TestSessionFactory.dataFactory().collectionDAO();
        try {
            dao.delete(dao.findByName("A simple collection").getId());
        } catch (NoResultException e) {
            // No existing collection, so we can proceed with the test
        }
        Collection created = dao.create("A simple collection");
        assertEquals("A simple collection", created.getName());
    }

    @Test
    public void testAddDuplicateCollection() {
        assertThrows(SQLIntegrityConstraintViolationException.class, () -> {
            CollectionDAO dao = TestSessionFactory.dataFactory().collectionDAO();
            dao.create("Test Collection");
            dao.create("Test Collection");
        });
    }

    @Test
    public void testUpdateNewCollections() {
        assertThrows(SQLIntegrityConstraintViolationException.class, () -> {
            CollectionDAO dao = TestSessionFactory.dataFactory().collectionDAO();
            dao.update(Collection.of("Test Collection"));
        });
    }

    @Test
    public void testDeleteCollectionByObject() throws SQLIntegrityConstraintViolationException {
        CollectionDAO dao = TestSessionFactory.dataFactory().collectionDAO();
        Collection toDelete = dao.create("DeleteByObjectTest");
        int initialSize = dao.findAll().size();
        dao.delete(toDelete);
        assertEquals(initialSize - 1, dao.findAll().size());
    }

    @Test
    public void testDeleteCollectionByObjectWithRegistrations() throws SQLIntegrityConstraintViolationException, IOException {
        CollectionDAO collectionDAO = TestSessionFactory.dataFactory().collectionDAO();
        CollectionPathDAO collectionPathDAO = TestSessionFactory.dataFactory().collectionPathDAO();
        PathRegistrationDAO pathRegistrationDAO = TestSessionFactory.dataFactory().pathRegistrationDAO();
        Collection toDelete = collectionDAO.create("DeleteByObjectWithRegsTest");
        CollectionPath cp = collectionPathDAO.create(CollectionPath.of(testPath));
        pathRegistrationDAO.register(toDelete, cp);
        int initialSize = collectionDAO.findAll().size();
        collectionDAO.delete(toDelete);
        assertEquals(initialSize - 1, collectionDAO.findAll().size());
    }

    @Test
    public void testDeleteCollections() throws SQLIntegrityConstraintViolationException, IOException {
        CollectionDAO collectionDAO = TestSessionFactory.dataFactory().collectionDAO();
        CollectionPathDAO collectionPathDAO = TestSessionFactory.dataFactory().collectionPathDAO();
        PathRegistrationDAO pathRegistrationDAO = TestSessionFactory.dataFactory().pathRegistrationDAO();
        String name = new Random().ints(97, 123)
            .limit(40)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
        Collection toDelete = collectionDAO.create(name);
        CollectionPath cp = collectionPathDAO.create(CollectionPath.of(testPath));
        pathRegistrationDAO.register(toDelete, cp);
        int initialSize = collectionDAO.findAll().size();
        collectionDAO.delete(toDelete.getId());
        assertEquals(initialSize - 1, collectionDAO.findAll().size());
    }
}
