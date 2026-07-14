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

import java.io.Serializable;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

abstract class GenericDao<K, E extends Serializable> {
    static protected EntityManagerFactory factory = jakarta.persistence.Persistence.createEntityManagerFactory("fixity-pu");
    static protected EntityManager entityManager = factory.createEntityManager();

    private Class<E> clazz;

    protected GenericDao(final String persistenceUnitName) {

    }

    protected void setClass(final Class<@NonNull E> clazz) {
        if (clazz == null) throw new NullPointerException();
        this.clazz = clazz;
    }

    protected Optional<E> findById(K id) {
        if (id == null) throw new NullPointerException();
        return Optional.ofNullable(entityManager.find(clazz, id));
    }

    protected List<E> findAll() {
        return entityManager.createQuery("SELECT e FROM " + this.clazz.getSimpleName() + " e", clazz).getResultList();
    }

    protected E refresh(E entity) {
        if (entity == null) throw new NullPointerException();
        entityManager.refresh(entity);
        return entity;
    }

    protected E create(E entity) throws SQLIntegrityConstraintViolationException {
        if (entity == null) throw new NullPointerException();
        entityManager.getTransaction().begin();
        entityManager.persist(entity);
        entityManager.getTransaction().commit();
        return entity;
    }

    protected E update(E entity) throws SQLIntegrityConstraintViolationException {
        if (entity == null) throw new NullPointerException();
        entityManager.getTransaction().begin();
        E mergedEntity = entityManager.merge(entity);
        entityManager.getTransaction().commit();
        return mergedEntity;
    }

    protected void delete(E entity) {
        if (entity == null) throw new NullPointerException();
        entityManager.getTransaction().begin();
        entityManager.remove(entity);
        entityManager.getTransaction().commit();
    }

    protected void deleteById(K id) {
        if (id == null) throw new NullPointerException();
        entityManager.getTransaction().begin();
        entityManager.remove(findById(id));
        entityManager.getTransaction().commit();
    }
}
