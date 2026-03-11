package org.openpreservation.fixity.apps.dao;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

import io.dropwizard.hibernate.AbstractDAO;
import jakarta.persistence.NoResultException;

@NullMarked
public class PathRegistrationDAO extends AbstractDAO<PathRegistration> {
    public PathRegistrationDAO(final org.hibernate.SessionFactory sessionFactory) {
        super(sessionFactory);
     }

    public PathRegistration findById(final Long id) throws NoResultException {
        PathRegistration registration = get(id);
        if (registration == null) throw new NoResultException("PathRegistration with id " + id + " not found.");
        return registration;
    }

    @SuppressWarnings("null")
    public PathRegistration create(final PathRegistration toCreate) throws SQLIntegrityConstraintViolationException {
        if (toCreate.id() != null) throw new SQLIntegrityConstraintViolationException("PathRegistration ID must be null for creation.");
        return super.persist(toCreate);
    }

    @SuppressWarnings("null")
    public List<@NonNull PathRegistration> findAll() {
        List<@NonNull PathRegistration> registrations = list(namedTypedQuery("PathRegistration.findAll"));
        return registrations != null ? registrations : List.of();
    }

    public PathRegistration findCurrentByPath(final Collection collection, final CollectionPath path) throws NoResultException {
        PathRegistration registration = namedTypedQuery("PathRegistration.getByPath")
                .setParameter("pathId", path.getId())
                .setParameter("collectionId", collection.getId())
                .getSingleResult();
        if (registration == null) throw new NoResultException("No PathRegistration found for collection with ID " + collection.getId() + " and path with ID " + path.getId());
        return registration;
   }

   public PathRegistration register(final Collection collection, final CollectionPath collectionPath) throws SQLIntegrityConstraintViolationException {
        try {
            findCurrentByPath(collection, collectionPath);
            throw new SQLIntegrityConstraintViolationException("Path is already registered to a Collection.");
        } catch (NoResultException e) {
            return create(PathRegistration.of(collection, collectionPath));
        }
    }

   @SuppressWarnings("null")
public PathRegistration deregister(final Collection collection, final CollectionPath path) throws SQLIntegrityConstraintViolationException {
        try {
            PathRegistration toDeregister = findCurrentByPath(collection, path);
            toDeregister.deregister();
            return super.persist(toDeregister);
        } catch (NoResultException e) {
            throw new SQLIntegrityConstraintViolationException("No registration found for collection with ID " + collection.getId() + " and path with ID " + path.getId());
        }
    }
}
