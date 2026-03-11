package org.openpreservation.fixity.apps.dao;

import java.util.Collections;
import java.util.List;

import org.hibernate.SessionFactory;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

import io.dropwizard.hibernate.AbstractDAO;
import jakarta.persistence.NoResultException;

@NullMarked
public class FolderScanRecordDAO extends AbstractDAO<FolderScanRecord> {

    public FolderScanRecordDAO(final SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public FolderScanRecord findById(final Long id) throws NoResultException {
        FolderScanRecord record = get(id);
        if (record == null) throw new NoResultException("FolderScanRecord with id " + id + " not found.");
        return record;
    }

    @SuppressWarnings("null")
    public List<@NonNull FolderScanRecord> findByPathScan(final PathScan pathScan) {
        List<@NonNull FolderScanRecord> records = Collections.unmodifiableList(
                list(namedTypedQuery("FolderScanRecord.findByPathScan").setParameter("pathScan", pathScan)));
        return records != null ? records : List.of();
    }
}
