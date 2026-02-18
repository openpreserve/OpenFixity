package org.openpreservation.fixity.apps.schedule;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Set;

import org.openpreservation.fixity.apps.dao.CollectionPath;
import org.openpreservation.fixity.apps.dao.FileScanRecord;
import org.openpreservation.fixity.apps.dao.PathScan;
import org.openpreservation.fixity.core.paths.FileScanResult;
import org.openpreservation.fixity.core.paths.PathScanner;
import org.openpreservation.fixity.core.paths.PathSummary;

public class BatchScanner extends PathScanner.AbstractPathScanner {
    private PathScan scan;
    
    public BatchScanner() {
        super();
    }

    @Override
    protected void processResults(final Set<FileScanResult> batchResults) throws SQLIntegrityConstraintViolationException {
        for (final FileScanResult result : batchResults.stream().map(
                fsr -> FileScanRecord.of(scan, fsr)).toList()) {
            final FileScanRecord record = FileScanRecord.of(scan, result);
            this.scan.addResult(record);
        }
    }

    @Override
    protected void setupScan(Path path) throws FileNotFoundException, SQLIntegrityConstraintViolationException {
        final CollectionPath collectionPath = CollectionPath.of(path);
        scan = collectionPath.createPathScan(PathSummary.of(path, false));
    }

    public PathScan getScan() {
        return this.scan;
    }
}
