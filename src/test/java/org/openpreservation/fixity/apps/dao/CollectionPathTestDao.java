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

import java.nio.file.Path;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Optional;

import jakarta.persistence.NoResultException;

public final class CollectionPathTestDao extends GenericDao<Long, CollectionPath> {
    protected CollectionPathTestDao() {
        this("fixity-pu");
    }

    protected CollectionPathTestDao(final String persistenceUnitName) {
        super(persistenceUnitName);
        setClass(CollectionPath.class);
    }    

    public Optional<CollectionPath> findByRoot(final String root) {
        try {
            return Optional.of(entityManager.createNamedQuery("CollectionPath.getByRoot", CollectionPath.class)
                    .setParameter("root", root)
                    .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<CollectionPath> findByRoot(final Path root) {
        return findByRoot(root.toAbsolutePath().toString());
    }

    @Override
    public CollectionPath create(final CollectionPath toCreate) throws SQLIntegrityConstraintViolationException {
        CollectionPath existing = findByRoot(toCreate.getRoot()).orElse(null);
        if (existing != null) throw new SQLIntegrityConstraintViolationException("Duplicate root directory key.");
        return super.create(toCreate);
    }

    @Override
    protected CollectionPath update(final CollectionPath toUpdate) throws SQLIntegrityConstraintViolationException {
        if (toUpdate.getId() == null) throw new SQLIntegrityConstraintViolationException("Cannot update non-existing CollectionPath");
        CollectionPath existing = findById(toUpdate.getId()).get();
        if (!existing.getRoot().equals(toUpdate.getRoot())) {
            throw new SQLIntegrityConstraintViolationException("Cannot change the root of an existing CollectionPath");
        }
        return  super.update(toUpdate);
    }
}
