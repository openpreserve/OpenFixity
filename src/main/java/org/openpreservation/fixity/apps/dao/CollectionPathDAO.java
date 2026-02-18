package org.openpreservation.fixity.apps.dao;

import java.nio.file.Path;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Optional;

import io.dropwizard.hibernate.AbstractDAO;
import jakarta.persistence.NoResultException;

public class CollectionPathDAO extends AbstractDAO<CollectionPath> {
    public CollectionPathDAO(final org.hibernate.SessionFactory sessionFactory) {
        super(sessionFactory);
     }

     public Optional<CollectionPath> findById(final Long id) {
         if (id == null) throw new NullPointerException("CollectionPath ID cannot be null.");
         return Optional.ofNullable(get(id));
     }

    public Optional<CollectionPath> findByRoot(final String root) {
        try {
            return Optional.of(namedTypedQuery("CollectionPath.getByRoot")
                    .setParameter("root", root)
                    .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<CollectionPath> findByRoot(final Path root) {
        return findByRoot(root.toAbsolutePath().toString());
    }

    public CollectionPath create(final CollectionPath toCreate) throws SQLIntegrityConstraintViolationException {
        if (findByRoot(toCreate.getRoot()).orElse(null) != null) throw new SQLIntegrityConstraintViolationException("Duplicate root directory key.");
        return super.persist(toCreate);
    }

    public List<CollectionPath> findAll() {
         return list(namedTypedQuery("CollectionPath.findAll"));
    }
    public CollectionPath update(final CollectionPath toUpdate) throws SQLIntegrityConstraintViolationException {
        if (toUpdate == null) throw new NullPointerException("CollectionPath to update cannot be null.");
        if (toUpdate.getId() == null) throw new SQLIntegrityConstraintViolationException("Cannot update non-existing CollectionPath.");
        CollectionPath existing = findById(toUpdate.getId()).orElse(null);
        if (!existing.getRoot().equals(toUpdate.getRoot())) {
            if (findByRoot(toUpdate.getRoot()).isPresent()) throw new SQLIntegrityConstraintViolationException("CollectionPath with root " + toUpdate.getRoot() + " already exists.");
        }
        return super.persist(toUpdate);
    }

    public CollectionPath delete(final Long id) throws SQLIntegrityConstraintViolationException {
        if (id == null) throw new NullPointerException("CollectionPath to delete cannot be null.");
        CollectionPath toDelete = findById(id).orElse(null);
        if (toDelete == null) throw new SQLIntegrityConstraintViolationException("Cannot delete non-existing CollectionPath.");
        currentSession().remove(toDelete);
        return toDelete;
    }

    public CollectionPath delete(final CollectionPath toDelete) throws SQLIntegrityConstraintViolationException {
        if (toDelete == null) throw new NullPointerException("CollectionPath to delete cannot be null.");
        if (toDelete.getId() == null) throw new SQLIntegrityConstraintViolationException("Cannot delete non-existing CollectionPath.");
        currentSession().remove(toDelete);
        return toDelete;
    }
}
