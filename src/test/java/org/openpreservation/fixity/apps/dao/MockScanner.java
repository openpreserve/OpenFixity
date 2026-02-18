package org.openpreservation.fixity.apps.dao;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Set;

import org.openpreservation.fixity.core.paths.FileScanResult;
import org.openpreservation.fixity.core.paths.PathScanner;
import org.openpreservation.fixity.core.paths.PathSummary;

public class MockScanner extends PathScanner.AbstractPathScanner {
    private PathScan scan;

    @Override
    protected void processResults(final Set<FileScanResult> batchResults) throws SQLIntegrityConstraintViolationException {
        for (final FileScanRecord record : batchResults.stream().map(
                fsr -> FileScanRecord.of(scan, fsr)).toList()) {
            DataManager.fileScanRecordDao().create(record);
            this.scan.addResult(record);
        }
        DataManager.pathScanDao().update(this.scan);
    }

    @Override
    protected void setupScan(Path path) throws FileNotFoundException, SQLIntegrityConstraintViolationException {
        final CollectionPath collectionPath = DataManager.collectionPathDao().findByRoot(path).orElse(null);
        if (collectionPath == null) {
            throw new FileNotFoundException("No CollectionPath found for root: " + path.toString());
        }
        scan = collectionPath.createPathScan(PathSummary.of(path, false));
        DataManager.pathScanDao().create(scan);
    }

    public PathScan getScan() {
        return this.scan;
    }
}
