package org.openpreservation.fixity.apps.dao;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Collections;
import java.util.List;

import org.hibernate.SessionFactory;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

import io.dropwizard.hibernate.AbstractDAO;
import jakarta.persistence.NoResultException;

@NullMarked
public class PathScanDAO extends AbstractDAO<PathScan> {

    public PathScanDAO(final SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public PathScan findById(final Long id) throws NoResultException{
        PathScan pathScan = get(id);
        if (pathScan == null) throw new NoResultException("PathScan with id " + id + " not found.");
        return pathScan;
    }

    @SuppressWarnings("null")
    public PathScan create(PathScan record) throws SQLIntegrityConstraintViolationException {
        if(record.getId() != null) throw new SQLIntegrityConstraintViolationException("PathScan ID must be null for creation.");
        return super.persist(record);
    }

    @SuppressWarnings("null")
    public List<@NonNull PathScan> findAll() {
        List<@NonNull PathScan> pathScans = Collections.unmodifiableList(list(super.namedTypedQuery("PathScan.findAll")));
        return pathScans != null ? pathScans : List.of();
    }

    @SuppressWarnings("null")
    public List<@NonNull PathScan> findByCollectionPath(final CollectionPath collectionPath) {
        List<@NonNull PathScan> pathScans = Collections.unmodifiableList(
                list(namedTypedQuery("PathScan.findByCollectionPath").setParameter("collectionPath", collectionPath)));
        return pathScans != null ? pathScans : List.of();
    }

    @SuppressWarnings("null")
    public PathScan update(final PathScan toUpdate) throws SQLIntegrityConstraintViolationException {
        if (toUpdate.getId() == null) throw new SQLIntegrityConstraintViolationException("PathScan ID cannot be null for update.");
        try {
            findById(toUpdate.getId());
            return super.persist(toUpdate);
        } catch (NoResultException e) {
            throw new SQLIntegrityConstraintViolationException("Cannot update non-existing PathScan with id " + toUpdate.getId());
        }
    }  

}
