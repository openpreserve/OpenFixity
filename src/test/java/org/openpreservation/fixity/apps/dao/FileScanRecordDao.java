package org.openpreservation.fixity.apps.dao;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

class FileScanRecordDao extends GenericDao<Long, FileScanRecord> {

    protected FileScanRecordDao() {
        this("fixity-pu");
    }

    protected FileScanRecordDao(final String persistenceUnitName) {
        super(persistenceUnitName);
        setClass(FileScanRecord.class);
    }    

    @Override
    public FileScanRecord create(FileScanRecord record) throws SQLIntegrityConstraintViolationException {
        if (record == null) throw new NullPointerException("FileSystemScanRecord is null");
        DataManager.digestRecordDao().create(record.getDigestResults());
        return super.create(record);
    }

    @Override
    public List<FileScanRecord> findAll() {
        return super.findAll();
    }

    public void addAll(final Iterable<FileScanRecord> entities) throws SQLIntegrityConstraintViolationException {
        if (entities == null) throw new NullPointerException("FileScanRecord iterator entities is null");
        for (final FileScanRecord entity : entities) {
            entityManager.persist(entity);
        }
    }
}
