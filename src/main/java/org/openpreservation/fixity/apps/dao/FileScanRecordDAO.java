package org.openpreservation.fixity.apps.dao;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

import org.hibernate.SessionFactory;

import io.dropwizard.hibernate.AbstractDAO;

public class FileScanRecordDAO extends AbstractDAO<FileScanRecord> {

    public FileScanRecordDAO(final SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public FileScanRecord findById(final Long id) {
        if (id == null) throw new NullPointerException("FileScanRecord ID cannot be null.");
        return get(id);
    }

    public FileScanRecord create(FileScanRecord record) throws SQLIntegrityConstraintViolationException {
        if (record == null) throw new NullPointerException("FileSystemScanRecord is null");
        return super.persist(record);
    }

    public List<FileScanRecord> findAll() {
        return list(super.namedTypedQuery("FileScanRecord.findAll"));
    }

    public void addAll(final Iterable<FileScanRecord> entities) throws SQLIntegrityConstraintViolationException {
        if (entities == null) throw new NullPointerException("FileScanRecord iterator entities is null");
        for (final FileScanRecord entity : entities) {
            super.persist(entity);
        }
    }
}
