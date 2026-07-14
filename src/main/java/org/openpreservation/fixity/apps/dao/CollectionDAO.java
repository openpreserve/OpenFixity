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

import org.hibernate.SessionFactory;
import org.jspecify.annotations.NullMarked;

import io.dropwizard.hibernate.AbstractDAO;
import jakarta.persistence.NoResultException;

@NullMarked
public class CollectionDAO extends AbstractDAO<Collection> {
    public CollectionDAO(final SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Collection findById(final Long id) throws NoResultException {
        Collection collection = get(id);
        if (collection == null) throw new NoResultException("Collection with id " + id + " not found.");
        return collection;
    }

    public Collection findByName(final String name) throws NoResultException {
        if (name.isBlank()) throw new IllegalArgumentException("Collection name cannot be blank.");
        Collection collection = namedTypedQuery("Collection.getByName")
            .setParameter("name", name)
            .getSingleResult();
        if (collection == null) throw new NoResultException("Collection with name " + name + " not found.");
        return collection;
    }

    public Collection getOrCreate(final String name) throws IllegalArgumentException, SQLIntegrityConstraintViolationException {
        if (name.isBlank()) throw new IllegalArgumentException("Collection name cannot be blank.");
        try {
            return findByName(name);
        } catch (NoResultException e) {
            return create(name);
        }
    }

    @SuppressWarnings("null")
    public Collection create(final String name) throws IllegalArgumentException, SQLIntegrityConstraintViolationException {
        if (name.isBlank()) throw new IllegalArgumentException("Collection name cannot be blank.");
        try {
            findByName(name);
            throw new SQLIntegrityConstraintViolationException("Collection with name " + name + " already exists.");
        } catch (NoResultException e) {
            return persist(Collection.of(name));
        }
    }

    @SuppressWarnings("null")
     public Collection update(final Collection toUpdate) throws SQLIntegrityConstraintViolationException {
        if (toUpdate.getId() == null) throw new SQLIntegrityConstraintViolationException("Cannot update non-existing Collection.");
        try {
            Collection existing = findById(toUpdate.getId());
            if (!existing.getName().equals(toUpdate.getName())) {
                throw new SQLIntegrityConstraintViolationException("Cannot change name of existing Collection. Existing name: " + existing.getName() + ", new name: " + toUpdate.getName());
            }
        } catch (NoResultException e) {
            throw new SQLIntegrityConstraintViolationException("Cannot update non-existing Collection with id: " + toUpdate.getId());
        }
        return super.persist(toUpdate);
    }

    @SuppressWarnings("null")
    public Collection delete(final Collection toDelete) throws SQLIntegrityConstraintViolationException {
        if (toDelete.getId() == null) throw new SQLIntegrityConstraintViolationException("Cannot delete non-existing Collection.");
        try {
            Collection existing = findById(toDelete.getId());
            return delete(existing.getId());
        } catch (NoResultException e) {
            throw new SQLIntegrityConstraintViolationException("Cannot delete non-existing Collection with id: " + toDelete.getId());
        }
    }

    public Collection delete(final Long id) throws SQLIntegrityConstraintViolationException {
        try {
            Collection existing = findById(id);
            currentSession().createQuery("SELECT pr FROM PathRegistration pr WHERE pr.collection = :collection", PathRegistration.class)
                .setParameter("collection", existing)
                .getResultList()
                .forEach(pr -> currentSession().remove(pr));
            currentSession().remove(existing);
            return existing;
        } catch (NoResultException e) {
            throw new SQLIntegrityConstraintViolationException("Cannot delete non-existing Collection with id: " + id);
        }
    }
    
    @SuppressWarnings("null")
    public List<Collection> findAll() {
        return list(namedTypedQuery("Collection.findAll"));
    }
}
