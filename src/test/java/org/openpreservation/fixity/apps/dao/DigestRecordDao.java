package org.openpreservation.fixity.apps.dao;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Set;

import org.openpreservation.fixity.core.digests.DigestResult;

import jakarta.validation.constraints.NotNull;

class DigestRecordDao extends GenericDao<Long, @NotNull DigestRecord> {

    protected DigestRecordDao() {
        this("fixity-pu");
    }

    protected DigestRecordDao(final String persistenceUnitName) {
        super(persistenceUnitName);
        setClass(DigestRecord.class);
    }    

    @Override
    public DigestRecord create(DigestRecord record) throws SQLIntegrityConstraintViolationException {
        if (record == null) throw new NullPointerException("DigestRecord is null");
        return super.create(record);
    }

    public Set<DigestRecord> create(Set<? extends DigestResult> records) throws SQLIntegrityConstraintViolationException {
        if (records == null) throw new NullPointerException("DigestRecord set is null");
        final Set<DigestRecord> toCreate = records.stream().map(r -> DigestRecord.of(r)).collect(java.util.stream.Collectors.toSet());
        for (final DigestRecord entity : toCreate) {
            entityManager.persist(entity);
        }
        return toCreate;
    }

    @Override
    public List<DigestRecord> findAll() {
        return super.findAll();
    }
}
