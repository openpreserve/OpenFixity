package org.openpreservation.fixity.apps.dao;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Optional;

import org.hibernate.SessionFactory;

import io.dropwizard.hibernate.AbstractDAO;

public class CollectionDAO extends AbstractDAO<Collection> {
    public CollectionDAO(final SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Collection findById(final Long id) {
        if (id == null) throw new NullPointerException("Collection ID cannot be null.");
        return get(id);
    }

    public Optional<Collection>  findByName(final String name) {
        if (name == null) throw new NullPointerException("Collection name cannot be null.");
        if (name.isBlank()) throw new IllegalArgumentException("Collection name cannot be blank.");
        try {
            return Optional.of(namedTypedQuery("Collection.getByName")
                .setParameter("name", name)
                .getSingleResult());
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    public Collection create(final String name) throws SQLIntegrityConstraintViolationException {
        if (name == null) throw new NullPointerException("Collection name cannot be null.");
        if (name.isBlank()) throw new IllegalArgumentException("Collection name cannot be blank.");
        if (findByName(name).isPresent()) throw new SQLIntegrityConstraintViolationException("Collection with name " + name + " already exists.");
        return persist(Collection.of(name));
    }

     public Collection update(final Collection toUpdate) throws SQLIntegrityConstraintViolationException {
        if (toUpdate == null) throw new NullPointerException("Collection to update cannot be null.");
        if (toUpdate.getId() == null) throw new SQLIntegrityConstraintViolationException("Cannot update non-existing Collection.");
        Collection existing = findById(toUpdate.getId());
        if (!existing.getName().equals(toUpdate.getName())) {
            if (findByName(toUpdate.getName()).isPresent()) throw new SQLIntegrityConstraintViolationException("Collection with name " + toUpdate.getName() + " already exists.");
        }
        return super.persist(toUpdate);
    }

    public Collection delete(final Collection toDelete) throws SQLIntegrityConstraintViolationException {
        if (toDelete == null) throw new NullPointerException("Collection to delete cannot be null.");
        return delete(toDelete.getId());
    }

    public Collection delete(final Long id) throws SQLIntegrityConstraintViolationException {
        if (id == null) throw new NullPointerException("Collection to delete cannot be null.");
        Collection existing = findById(id);
        if (existing == null) throw new SQLIntegrityConstraintViolationException("Cannot delete non-existing Collection.");
        currentSession().remove(existing);
        return existing;
    }
    
    public List<Collection> findAll() {
        return list(namedTypedQuery("Collection.findAll"));
    }
}
