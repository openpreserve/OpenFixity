package org.openpreservation.fixity.apps.dao;

public class DataFactory {
    private final CollectionDAO collectionDAO;
    private final CollectionPathDAO collectionPathDAO;
    private final PathRegistrationDAO pathRegistrationDAO;
    private final FileScanRecordDAO fileScanRecordDAO;
    private final PathScanDAO pathScanDAO;

    public DataFactory(CollectionDAO collectionDAO,
                       CollectionPathDAO collectionPathDAO,
                       PathRegistrationDAO pathRegistrationDAO,
                       FileScanRecordDAO fileScanRecordDAO,
                       PathScanDAO pathScanDAO) {
        this.collectionDAO = collectionDAO;
        this.collectionPathDAO = collectionPathDAO;
        this.pathRegistrationDAO = pathRegistrationDAO;
        this.fileScanRecordDAO = fileScanRecordDAO;
        this.pathScanDAO = pathScanDAO;
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
}
