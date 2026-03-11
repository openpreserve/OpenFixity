package org.openpreservation.fixity.apps.schedule;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.openpreservation.fixity.apps.dao.CollectionPath;
import org.openpreservation.fixity.apps.dao.FileScanRecord;
import org.openpreservation.fixity.apps.dao.PathScan;
import org.openpreservation.fixity.core.paths.FileScanResult;
import org.openpreservation.fixity.core.paths.PathScanner;
import org.openpreservation.fixity.core.paths.PathSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class BatchScanner extends PathScanner.AbstractPathScanner {
    @SuppressWarnings("null")
    final static Logger logger = LoggerFactory.getLogger(BatchScanner.class);
    private @Nullable PathScan scan;
    
    public BatchScanner() {
        super();
    }

    @SuppressWarnings("null")
    @Override
    protected void processResults(final Set<@NonNull FileScanResult> batchResults) throws SQLIntegrityConstraintViolationException {
        if (scan == null) {
            throw new IllegalStateException("Scan must be setup before processing results");
        }
        logger.debug("Processing next batch of results: " + batchResults.size());
        for (final FileScanResult fsr : batchResults) {
            this.scan.addFile(FileScanRecord.of(scan, fsr));
        }
    }

    @Override
    protected void setupScan(Path path) throws FileNotFoundException, SQLIntegrityConstraintViolationException {
        final CollectionPath collectionPath = CollectionPath.of(path);
        scan = collectionPath.createPathScan(PathSummary.of(path, true));
    }

    @SuppressWarnings("null")
    public PathScan getScan() {
        return this.scan;
    }
}
