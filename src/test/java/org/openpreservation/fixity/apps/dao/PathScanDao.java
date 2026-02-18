package org.openpreservation.fixity.apps.dao;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

class PathScanDao extends GenericDao<Long, PathScan> {

    protected PathScanDao() {
        this("fixity-pu");
    }

    protected PathScanDao(final String persistenceUnitName) {
        super(persistenceUnitName);
        setClass(PathScan.class);
    }    

    @Override
    public PathScan create(PathScan record) throws SQLIntegrityConstraintViolationException {
        if (record == null) throw new NullPointerException("PathScan is null");
        return super.create(record);
    }

    @Override
    public List<PathScan> findAll() {
        return super.findAll();
    }
}
