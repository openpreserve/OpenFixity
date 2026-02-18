package org.openpreservation.fixity.apps.dao;

import java.util.Optional;

import jakarta.persistence.NoResultException;

final class PathRegistrationDao extends GenericDao<Long, PathRegistration> {
    protected PathRegistrationDao() {
        this("fixity-pu");
    }

    protected PathRegistrationDao(final String persistenceUnitName) {
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
