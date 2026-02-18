package org.openpreservation.fixity.apps.dao;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Optional;

import io.dropwizard.hibernate.AbstractDAO;
import jakarta.persistence.NoResultException;

public class PathRegistrationDAO extends AbstractDAO<PathRegistration> {
    public PathRegistrationDAO(final org.hibernate.SessionFactory sessionFactory) {
        super(sessionFactory);
     }

    public Optional<PathRegistration> findById(final Long id) {
        if (id == null) return Optional.empty();
        try {
            return Optional.of(get(id));
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public PathRegistration create(final PathRegistration toCreate) throws SQLIntegrityConstraintViolationException {
        if (findById(toCreate.id()).orElse(null) != null) throw new SQLIntegrityConstraintViolationException("Duplicate ID   key.");
        return super.persist(toCreate);
    }

    public List<PathRegistration> findAll() {
         return list(namedTypedQuery("PathRegistration.findAll"));
    }

    public Optional<PathRegistration> findCurrentByPath(final Collection collection, final CollectionPath path) {
        try {
            return Optional.of(namedTypedQuery("PathRegistration.getByPath")
                    .setParameter("pathId", path.getId())
                    .setParameter("collectionId", collection.getId())
                    .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
   }

   public PathRegistration register(final Collection collection, final CollectionPath collectionPath) throws SQLIntegrityConstraintViolationException {
        PathRegistration existingRegistration = findCurrentByPath(collection, collectionPath).orElse(null);
        if (existingRegistration != null) throw new SQLIntegrityConstraintViolationException("Path is already registered to a Collection.");
        return create(PathRegistration.of(collection, collectionPath));
    }

   public PathRegistration deregister(final Collection collection, final CollectionPath path) throws SQLIntegrityConstraintViolationException {
        PathRegistration toDeregister = findCurrentByPath(collection, path).orElse(null);
        if (toDeregister == null) throw new SQLIntegrityConstraintViolationException("No registration found for collection with ID " + collection.getId() + " and path with ID " + path.getId());
        toDeregister.deregister();
        return super.persist(toDeregister);
    }
}
