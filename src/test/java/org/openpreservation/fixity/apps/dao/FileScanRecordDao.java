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

class FileScanRecordDao extends GenericDao<Long, FileScanRecord> {

    protected FileScanRecordDao() {
        this("fixity-pu");
    }

    protected FileScanRecordDao(final String persistenceUnitName) {
        super(persistenceUnitName);
        setClass(FileScanRecord.class);
    }    

    @Override
    public FileScanRecord create(FileScanRecord record) throws SQLIntegrityConstraintViolationException {
        if (record == null) throw new NullPointerException("FileSystemScanRecord is null");
        DataManager.digestRecordDao().create(record.getDigestResults());
        return super.create(record);
    }

    @Override
    public List<FileScanRecord> findAll() {
        return super.findAll();
    }

    public void addAll(final Iterable<FileScanRecord> entities) throws SQLIntegrityConstraintViolationException {
        if (entities == null) throw new NullPointerException("FileScanRecord iterator entities is null");
        for (final FileScanRecord entity : entities) {
            entityManager.persist(entity);
        }
    }
}
