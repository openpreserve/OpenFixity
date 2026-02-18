package org.openpreservation.fixity.apps.dao;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Optional;

import org.hibernate.SessionFactory;

import io.dropwizard.hibernate.AbstractDAO;

public class PathScanDAO extends AbstractDAO<PathScan> {

    public PathScanDAO(final SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Optional<PathScan> findById(final Long id) {
        if (id == null) throw new NullPointerException("PathScan ID cannot be null.");
        return Optional.ofNullable(super.get(id));
    }

    public PathScan create(PathScan record) throws SQLIntegrityConstraintViolationException {
        if (record == null) throw new NullPointerException("PathScan is null");
        return super.persist(record);
    }

    public List<PathScan> findAll() {
        return list(super.namedTypedQuery("PathScan.findAll"));
    }

    public PathScan update(final PathScan toUpdate) throws SQLIntegrityConstraintViolationException {
        if (toUpdate == null) throw new NullPointerException("PathScan to update cannot be null.");
        if (toUpdate.getId() == null) throw new SQLIntegrityConstraintViolationException("Cannot update non-existing PathScan.");
        return super.persist(toUpdate);
    }  

}
