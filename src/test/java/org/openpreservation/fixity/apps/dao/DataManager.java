package org.openpreservation.fixity.apps.dao;

public class DataManager {

    public static CollectionDao collectionDao() {
        return new CollectionDao();
    }

    public static CollectionPathDao collectionPathDao() {
        return new CollectionPathDao();
    }

    public static DigestRecordDao digestRecordDao() {
        return new DigestRecordDao();
    }

    public static FileScanRecordDao fileScanRecordDao() {
        return new FileScanRecordDao();
    }

    public static PathScanDao pathScanDao() {
        return new PathScanDao();
    }
    public static PathRegistrationDao pathRegistrationDao() {
        return new PathRegistrationDao();
    }
}
