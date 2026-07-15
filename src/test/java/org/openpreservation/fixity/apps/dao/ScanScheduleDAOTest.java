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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpreservation.fixity.Utils;
import org.openpreservation.fixity.core.digests.Algorithms;

@SuppressWarnings("null")
public class ScanScheduleDAOTest {
    Path testDir;
    CollectionPath collectionPath;

    @BeforeEach
    public void setUp() throws IOException, SQLIntegrityConstraintViolationException {
        TestSessionFactory.beginTransaction();
        testDir = Files.createTempDirectory("fixity-schedule-test");
        collectionPath = TestSessionFactory.dataFactory().collectionPathDAO().create(CollectionPath.of(testDir));
    }

    @AfterEach
    public void tearDown() throws IOException {
        TestSessionFactory.rollback();
        Utils.deleteDirectory(testDir);
    }

    @Test
    public void testCreateAndFindById() {
        ScanScheduleDAO dao = TestSessionFactory.dataFactory().scanScheduleDAO();
        ScanSchedule saved = dao.create(
                ScanSchedule.of(collectionPath, Frequency.DAILY, 30, 2, 1, Algorithms.SHA_256));
        assertNotNull(saved.getId());

        ScanSchedule found = dao.findById(saved.getId());
        assertEquals(Frequency.DAILY, found.getFrequency());
        assertEquals(Algorithms.SHA_256, found.getAlgorithm());
        assertEquals("0 30 2 * * ?", found.toCron());
        assertTrue(found.isEnabled());
    }

    @Test
    public void testFindEnabledExcludesDisabled() {
        ScanScheduleDAO dao = TestSessionFactory.dataFactory().scanScheduleDAO();
        ScanSchedule enabled = dao.create(
                ScanSchedule.of(collectionPath, Frequency.HOURLY, 0, 0, 1, Algorithms.SHA_256));
        ScanSchedule disabled = ScanSchedule.of(collectionPath, Frequency.WEEKLY, 0, 3, 2, Algorithms.SHA_512);
        disabled.setEnabled(false);
        dao.create(disabled);

        List<ScanSchedule> enabledList = dao.findEnabled();
        assertTrue(enabledList.stream().anyMatch(s -> s.getId().equals(enabled.getId())));
        assertFalse(enabledList.stream().anyMatch(s -> s.getId().equals(disabled.getId())));
    }

    @Test
    public void testCronOverrideWins() {
        ScanScheduleDAO dao = TestSessionFactory.dataFactory().scanScheduleDAO();
        ScanSchedule saved = dao.create(
                ScanSchedule.ofCron(collectionPath, "0 0 4 ? * 1", Algorithms.SHA_256));
        assertEquals("0 0 4 ? * 1", dao.findById(saved.getId()).toCron());
    }

    @Test
    public void testDeleteRemovesSchedule() {
        ScanScheduleDAO dao = TestSessionFactory.dataFactory().scanScheduleDAO();
        ScanSchedule saved = dao.create(
                ScanSchedule.of(collectionPath, Frequency.DAILY, 0, 1, 1, Algorithms.SHA_256));
        Long id = saved.getId();
        dao.delete(saved);
        assertFalse(dao.findAll().stream().anyMatch(s -> id.equals(s.getId())));
    }

    @Test
    public void testScheduleCarriesPathInfoForTheFrontend() {
        ScanScheduleDAO dao = TestSessionFactory.dataFactory().scanScheduleDAO();
        ScanSchedule saved = dao.create(
                ScanSchedule.of(collectionPath, Frequency.DAILY, 0, 1, 1, Algorithms.SHA_256));
        ScanSchedule found = dao.findById(saved.getId());
        assertEquals(collectionPath.getId(), found.getPathId());
        assertEquals(collectionPath.getFullPath(), found.getPathRoot());
    }
}
