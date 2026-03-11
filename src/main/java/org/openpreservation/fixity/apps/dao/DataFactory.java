package org.openpreservation.fixity.apps.dao;

import org.hibernate.SessionFactory;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DataFactory {
    private final CollectionDAO collectionDAO;
    private final CollectionPathDAO collectionPathDAO;
    private final PathRegistrationDAO pathRegistrationDAO;
    private final FileScanRecordDAO fileScanRecordDAO;
    private final PathScanDAO pathScanDAO;
    private final FolderScanRecordDAO folderScanRecordDAO;

    public DataFactory(SessionFactory sessionFactory) {
        this.collectionDAO = new CollectionDAO(sessionFactory);
        this.collectionPathDAO = new CollectionPathDAO(sessionFactory);
        this.pathRegistrationDAO = new PathRegistrationDAO(sessionFactory);
        this.fileScanRecordDAO = new FileScanRecordDAO(sessionFactory);
        this.pathScanDAO = new PathScanDAO(sessionFactory);
        this.folderScanRecordDAO = new FolderScanRecordDAO(sessionFactory);
    }
    public CollectionDAO collectionDAO() {
        return this.collectionDAO;
    }
    public CollectionPathDAO collectionPathDAO() {
        return this.collectionPathDAO;
    }
    public PathRegistrationDAO pathRegistrationDAO() {
        return this.pathRegistrationDAO;
    }
    public FileScanRecordDAO fileScanRecordDAO() {
        return this.fileScanRecordDAO;
    }
    public PathScanDAO pathScanDAO() {
        return this.pathScanDAO;
    }
    public FolderScanRecordDAO folderScanRecordDAO() {
        return this.folderScanRecordDAO;
    }
}
