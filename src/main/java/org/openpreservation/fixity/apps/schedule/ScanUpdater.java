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

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.openpreservation.fixity.apps.dao.CollectionPath;
import org.openpreservation.fixity.apps.dao.CollectionPathDAO;
import org.openpreservation.fixity.apps.dao.FileScanRecord;
import org.openpreservation.fixity.apps.dao.FolderScanRecord;
import org.openpreservation.fixity.apps.dao.PathScan;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.dropwizard.hibernate.UnitOfWork;
import jakarta.persistence.NoResultException;

public class ScanUpdater {
    private static final Logger logger = LoggerFactory.getLogger(ScanUpdater.class);
    private final CollectionPathDAO collectionPathDAO;

    public ScanUpdater(CollectionPathDAO collectionPathDAO) {
        this.collectionPathDAO = collectionPathDAO;
    }

    @UnitOfWork
    public void updateDatabase(PathScan scan) throws JobExecutionException {
        logger.info("Updating database with results of scan for path: " + scan.getCollectionPath().getFullPath());
        CollectionPath collectionPath = getCollectionPathForScan(scan);
        logger.info("CollectionPath found for scan: " + collectionPath.getFullPath());
        updateFromPreviousScan(collectionPath, scan);
        collectionPath.addPathScan(scan);
        try {
            logger.info("Committing scan results to database for path: " + scan.getCollectionPath().getFullPath());
            collectionPathDAO.update(collectionPath);
            logger.info("Committed scan results to database for path: " + scan.getCollectionPath().getFullPath());
        } catch (SQLIntegrityConstraintViolationException e) {
            logger.error("ScanJob failed: CollectionPath with root " + scan.getCollectionPath().getRoot() + " not found.", e);
            throw new JobExecutionException("ScanJob failed: CollectionPath with root " + scan.getCollectionPath().getRoot() + " not found.", e);
        }
    }

    private CollectionPath getCollectionPathForScan(PathScan scan) throws JobExecutionException {
        CollectionPath collectionPath;
        try {
            collectionPath =
                collectionPathDAO.findByRoot(scan.getCollectionPath().getRoot());
        } catch (NoResultException e) {
            logger.error("ScanJob failed: CollectionPath with root " + scan.getCollectionPath().getRoot() + " not found.", e);
            throw new JobExecutionException("ScanJob failed: CollectionPath with root " + scan.getCollectionPath().getRoot() + " not found.", e);
        }
        return collectionPath;
    }

    private void updateFromPreviousScan(CollectionPath collectionPath, PathScan scan) {
        try {
            PathScan previousScan = collectionPath.getLatestScan();
            logger.info("Updating scan with results from previous scan for path: " + scan.getCollectionPath().getFullPath());
            updateScan(scan, previousScan);
        } catch (NoResultException e) {
            logger.info("No previous scan for path: " + scan.getCollectionPath().getFullPath());
        }
    }

    void updateScan(final PathScan latest, final PathScan previousScan) {
        logger.debug("Checking for matching results between latest scan and previous scan for path: " + latest.getCollectionPath().getFullPath());
        Map<@NonNull String, @NonNull FolderScanRecord> previousFolderMap = previousScan.getFolders().stream()
                .collect(Collectors.toMap(FolderScanRecord::getRelativePath, f -> f));

        Set<@NonNull String> currentFolderPaths = new HashSet<>();
        for (FolderScanRecord currentFolder : latest.getFolders()) {
            currentFolderPaths.add(currentFolder.getRelativePath());
            FolderScanRecord previousFolder = previousFolderMap.get(currentFolder.getRelativePath());
            if (previousFolder != null) {
                compareFolder(latest, currentFolder, previousFolder);
            }
            // else: all files in this folder are new → ADDED by default
        }

        // Folders in previous but not in current → all files become NOTFOUND
        for (FolderScanRecord previousFolder : previousScan.getFolders()) {
            if (!currentFolderPaths.contains(previousFolder.getRelativePath())) {
                for (FileScanRecord prevFile : previousFolder.getFiles()) {
                    latest.addResultForDeleted(prevFile);
                }
            }
        }
        logger.info("Finished updating scan with results from previous scan for path: " + latest.getCollectionPath().getFullPath());
    }

    private void compareFolder(final PathScan latest, final FolderScanRecord currentFolder, final FolderScanRecord previousFolder) {
        Map<@NonNull String, @NonNull FileScanRecord> previousFileMap = previousFolder.getFiles().stream()
                .collect(Collectors.toMap(FileScanRecord::relativePath, f -> f));

        Set<@NonNull String> currentFilePaths = new HashSet<>();
        for (FileScanRecord currentFile : currentFolder.getFiles()) {
            currentFilePaths.add(currentFile.relativePath());
            FileScanRecord previousFile = previousFileMap.get(currentFile.relativePath());
            if (previousFile != null) {
                currentFile.updateStatus(previousFile);
            }
            // else: file is new in this folder → ADDED by default
        }

        // Files in previous folder but not in current → NOTFOUND
        for (FileScanRecord prevFile : previousFolder.getFiles()) {
            if (!currentFilePaths.contains(prevFile.relativePath())) {
                latest.addResultForDeleted(prevFile);
            }
        }
    }
}
