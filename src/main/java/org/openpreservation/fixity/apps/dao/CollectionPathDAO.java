package org.openpreservation.fixity.apps.dao;

import java.nio.file.Path;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.openpreservation.fixity.core.paths.Folder;

import io.dropwizard.hibernate.AbstractDAO;
import jakarta.persistence.NoResultException;

@NullMarked
public class CollectionPathDAO extends AbstractDAO<CollectionPath> {
    public CollectionPathDAO(final org.hibernate.SessionFactory sessionFactory) {
        super(sessionFactory);
     }

     public CollectionPath findById(final Long id) throws NoResultException {
        CollectionPath collectionPath = get(id);
        if (collectionPath == null) throw new NoResultException("CollectionPath with id " + id + " not found.");
        return collectionPath;
     }

    public CollectionPath findByRoot(final String root) throws NoResultException {
        CollectionPath collectionPath = namedTypedQuery("CollectionPath.getByRoot")
            .setParameter("root", root)
            .getSingleResult();
        if (collectionPath == null) throw new NoResultException("CollectionPath with root " + root + " not found.");
        return collectionPath;
    }
        
    public CollectionPath findByRoot(final Path root) {
        return findByRoot(Folder.absolutePathString(root));
    }

    @SuppressWarnings("null")
    public CollectionPath create(final CollectionPath toCreate) throws SQLIntegrityConstraintViolationException {
        if (toCreate.getId() != null) throw new SQLIntegrityConstraintViolationException("CollectionPath ID must be null for creation.");
        try {
            findByRoot(toCreate.getRoot());
            throw new SQLIntegrityConstraintViolationException("CollectionPath with root " + toCreate.getFullPath() + " already exists.");
        } catch (NoResultException e) {
            return super.persist(toCreate);
        }
    }

    @SuppressWarnings("null")
    public CollectionPath getOrCreate(final CollectionPath toCreate) throws SQLIntegrityConstraintViolationException {
        if (toCreate.getId() == null) return create(toCreate);
        try {
            return findById(toCreate.getId());
        } catch (NoResultException e) {
            // Expected, continue with creation
            return create(toCreate);
        }
    }

    @SuppressWarnings("null")
    public List<@NonNull CollectionPath> findAll() {
        List<@NonNull CollectionPath> collectionPaths = Collections.unmodifiableList(list(namedTypedQuery("CollectionPath.findAll")));
        if (collectionPaths == null) return List.of();
        return collectionPaths;
    }

    @SuppressWarnings("null")
    public CollectionPath update(final CollectionPath toUpdate) throws SQLIntegrityConstraintViolationException {
        if (toUpdate.getId() == null) throw new SQLIntegrityConstraintViolationException("Cannot update non-existing CollectionPath.");
        try {
            CollectionPath existing = findById(toUpdate.getId());
            if (!existing.getRoot().equals(toUpdate.getRoot())) {
                throw new SQLIntegrityConstraintViolationException("It's not possible to change the root folder of a Collection Path.");
            }
            return super.persist(toUpdate);
        } catch (NoResultException e) {
            throw new SQLIntegrityConstraintViolationException("Cannot update non-existing CollectionPath with id: " + toUpdate.getId());
        }
    }

    public CollectionPath delete(final Long id) throws SQLIntegrityConstraintViolationException {
        try {
            return this.delete(findById(id));
        } catch (NoResultException e) {
            throw new SQLIntegrityConstraintViolationException("Cannot delete non-existing CollectionPath with id: " + id);
        }
    }

    public CollectionPath delete(final CollectionPath toDelete) throws SQLIntegrityConstraintViolationException {
        if (toDelete.getId() == null) throw new SQLIntegrityConstraintViolationException("Cannot delete non-existing CollectionPath.");
        currentSession().remove(toDelete);
        return toDelete;
    }
}
