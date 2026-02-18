package org.openpreservation.fixity.apps.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.NoResultException;

final class CollectionDao extends GenericDao<Long, Collection> {
    protected CollectionDao() {
        this("fixity-pu");
    }

    protected CollectionDao(final String persistenceUnitName) {
        super(persistenceUnitName);
        setClass(Collection.class);
    }

    public Optional<Collection> findByName(final String name) {
        if (name == null) throw new NullPointerException();
        try {
            return Optional.of(entityManager.createNamedQuery("Collection.getByName", Collection.class)
                .setParameter("name", name)
                .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public CollectionPath registerCollectionPath(Collection collection, Path toRegister) throws SQLIntegrityConstraintViolationException, IOException {
        if ((collection == null) || (toRegister == null)) throw new NullPointerException();
        if (!Files.exists(toRegister) || !Files.isDirectory(toRegister) || !Files.isReadable(toRegister)) {
            throw new IOException(String.format("Path %s to register must be an existing, readable directory.", toRegister.toString()));
        }
        checkPathRegistrationNotExists(collection.getPathRegistrations(), toRegister);
        CollectionPath collectionPath = DataManager.collectionPathDao().findByRoot(toRegister).orElse(DataManager.collectionPathDao().create(CollectionPath.of(toRegister)));
        PathRegistration existingRegistration = DataManager.pathRegistrationDao().findCurrentByPath(collection, collectionPath).orElse(null);
        if (existingRegistration != null) throw new SQLIntegrityConstraintViolationException("Path is already registered to a Collection.");
        update(collection.register(DataManager.pathRegistrationDao().create(PathRegistration.of(collection, collectionPath))));
        return collectionPath;
    }

    public void deregisterCollectionPath(Collection collection, Path toDeregister) throws SQLIntegrityConstraintViolationException, IOException {
        if ((collection == null) || (toDeregister == null)) throw new NullPointerException();
        if (collection.getPathRegistrations() == null) {
            return;
        }
        for (PathRegistration pr : collection.getPathRegistrations()) {
            if (toDeregister.compareTo(pr.getCollectionPath().getRoot()) == 0 && pr.getDeregisteredAt() == null) {
                pr.deregister();
                update(collection);
                return;
            }
        }
        throw new SQLIntegrityConstraintViolationException("Path is not registered to the Collection.");
    }

    private void checkPathRegistrationNotExists(Set<PathRegistration> registrations, Path toCheck) throws SQLIntegrityConstraintViolationException, IOException {
        if (registrations == null) {
            return;
        }
        for (PathRegistration pr : registrations) {
            if (pr.getDeregisteredAt() == null && Files.exists(pr.getCollectionPath().getRoot()) && Files.isSameFile(toCheck, pr.getCollectionPath().getRoot())) {
                throw new SQLIntegrityConstraintViolationException("Path is already registered to the Collection.");
            }
        }
    }

    @Override
    public Collection create(final Collection toCreate) throws SQLIntegrityConstraintViolationException {
        if (toCreate == null) throw new NullPointerException();

        if (findByName(toCreate.getName()).isPresent()) {
            throw new SQLIntegrityConstraintViolationException("Collection with name " + toCreate.getName() + " already exists.");
        }
        return super.create(toCreate);
    }

    @Override
    protected Collection update(final Collection toUpdate) throws SQLIntegrityConstraintViolationException {
        if (toUpdate == null) throw new NullPointerException();
        if (toUpdate.getId() == null) throw new SQLIntegrityConstraintViolationException("Cannot update non-existing Collection");
        Collection existing = findById(toUpdate.getId()).orElse(null);
        if (existing != null && !existing.getName().equals(toUpdate.getName())) {
            throw new SQLIntegrityConstraintViolationException("Cannot update non-existing Collection");
        }
        return super.update(toUpdate);
    }
}
