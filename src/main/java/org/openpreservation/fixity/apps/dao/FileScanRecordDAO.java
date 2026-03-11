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
public class FileScanRecordDAO extends AbstractDAO<FileScanRecord> {

    public FileScanRecordDAO(final SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public FileScanRecord findById(final Long id) throws NoResultException {
        FileScanRecord record = get(id);
        if (record == null) throw new NoResultException("FileScanRecord with id " + id + " not found.");
        return record;
    }

    @SuppressWarnings("null")
    public FileScanRecord create(FileScanRecord record) throws SQLIntegrityConstraintViolationException {
        return super.persist(record);
    }

    @SuppressWarnings("null")
    public List<@NonNull FileScanRecord> findAll() {
        final List<@NonNull FileScanRecord> records = Collections.unmodifiableList(list(super.namedTypedQuery("FileScanRecord.findAll")));
        return records != null ? records : Collections.emptyList();
    }

    public void addAll(final Iterable<FileScanRecord> entities) throws SQLIntegrityConstraintViolationException {
        for (final FileScanRecord entity : entities) {
            super.persist(entity);
        }
    }
}
