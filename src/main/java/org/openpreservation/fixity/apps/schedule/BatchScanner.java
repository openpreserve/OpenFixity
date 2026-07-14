/*
 * OpenFixity is an application for monitoring and reporting on the fixity of files.
 * Copyright (C) 2026 Open Preservation Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
