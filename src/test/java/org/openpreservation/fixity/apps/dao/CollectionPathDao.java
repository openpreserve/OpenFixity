package org.openpreservation.fixity.apps.dao;

import java.nio.file.Path;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Optional;

import jakarta.persistence.NoResultException;

public final class CollectionPathDao extends GenericDao<Long, CollectionPath> {
    protected CollectionPathDao() {
        this("fixity-pu");
    }

    protected CollectionPathDao(final String persistenceUnitName) {
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
