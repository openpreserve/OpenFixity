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

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Set;

import org.openpreservation.fixity.core.digests.DigestResult;

import jakarta.validation.constraints.NotNull;

class DigestRecordDao extends GenericDao<Long, @NotNull DigestRecord> {

    protected DigestRecordDao() {
        this("fixity-pu");
    }

    protected DigestRecordDao(final String persistenceUnitName) {
        super(persistenceUnitName);
        setClass(DigestRecord.class);
    }    

    @Override
    public DigestRecord create(DigestRecord record) throws SQLIntegrityConstraintViolationException {
        if (record == null) throw new NullPointerException("DigestRecord is null");
        return super.create(record);
    }

    public Set<DigestRecord> create(Set<? extends DigestResult> records) throws SQLIntegrityConstraintViolationException {
        if (records == null) throw new NullPointerException("DigestRecord set is null");
        final Set<DigestRecord> toCreate = records.stream().map(r -> DigestRecord.of(r)).collect(java.util.stream.Collectors.toSet());
        for (final DigestRecord entity : toCreate) {
            entityManager.persist(entity);
        }
        return toCreate;
    }

    @Override
    public List<DigestRecord> findAll() {
        return super.findAll();
    }
}
