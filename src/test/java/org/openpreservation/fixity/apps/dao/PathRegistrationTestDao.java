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

import java.util.Optional;

import jakarta.persistence.NoResultException;

final class PathRegistrationTestDao extends GenericDao<Long, PathRegistration> {
    protected PathRegistrationTestDao() {
        this("fixity-pu");
    }

    protected PathRegistrationTestDao(final String persistenceUnitName) {
        super(persistenceUnitName);
        setClass(PathRegistration.class);
    }

    Optional<PathRegistration> findCurrentByPath(final Collection collection, final CollectionPath path) {
        try {
            return Optional.of(entityManager.createNamedQuery("PathRegistration.getByPath", PathRegistration.class)
                    .setParameter("pathId", path.getId())
                    .setParameter("collectionId", collection.getId())
                    .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}
