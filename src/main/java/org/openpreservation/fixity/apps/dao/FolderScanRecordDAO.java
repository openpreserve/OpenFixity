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

import java.util.Collections;
import java.util.List;

import org.hibernate.SessionFactory;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

import io.dropwizard.hibernate.AbstractDAO;
import jakarta.persistence.NoResultException;

@NullMarked
public class FolderScanRecordDAO extends AbstractDAO<FolderScanRecord> {

    public FolderScanRecordDAO(final SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public FolderScanRecord findById(final Long id) throws NoResultException {
        FolderScanRecord record = get(id);
        if (record == null) throw new NoResultException("FolderScanRecord with id " + id + " not found.");
        return record;
    }

    @SuppressWarnings("null")
    public List<@NonNull FolderScanRecord> findByPathScan(final PathScan pathScan) {
        List<@NonNull FolderScanRecord> records = Collections.unmodifiableList(
                list(namedTypedQuery("FolderScanRecord.findByPathScan").setParameter("pathScan", pathScan)));
        return records != null ? records : List.of();
    }
}
