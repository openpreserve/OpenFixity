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

import java.util.List;

import org.hibernate.SessionFactory;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

import io.dropwizard.hibernate.AbstractDAO;
import jakarta.persistence.NoResultException;

@NullMarked
public class ScanScheduleDAO extends AbstractDAO<ScanSchedule> {

    public ScanScheduleDAO(final SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public ScanSchedule findById(final Long id) throws NoResultException {
        final ScanSchedule schedule = get(id);
        if (schedule == null) {
            throw new NoResultException("ScanSchedule with id " + id + " not found.");
        }
        return schedule;
    }

    @SuppressWarnings("null")
    public List<@NonNull ScanSchedule> findAll() {
        return list(namedTypedQuery("ScanSchedule.findAll"));
    }

    /** Enabled schedules only; used at startup to re-register schedules with Quartz. */
    @SuppressWarnings("null")
    public List<@NonNull ScanSchedule> findEnabled() {
        return list(namedTypedQuery("ScanSchedule.findEnabled"));
    }

    @SuppressWarnings("null")
    public List<@NonNull ScanSchedule> findByCollectionPath(final CollectionPath collectionPath) {
        return list(namedTypedQuery("ScanSchedule.findByCollectionPath")
                .setParameter("collectionPath", collectionPath));
    }

    public ScanSchedule create(final ScanSchedule toCreate) {
        return persist(toCreate);
    }

    public ScanSchedule update(final ScanSchedule toUpdate) {
        return persist(toUpdate);
    }

    public void delete(final ScanSchedule toDelete) {
        currentSession().remove(toDelete);
    }
}
