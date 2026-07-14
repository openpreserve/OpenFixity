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

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.openpreservation.fixity.apps.dao.CollectionPathDAO;
import org.openpreservation.fixity.apps.dao.PathScan;
import org.openpreservation.fixity.apps.server.OpenFixityServer;
import org.openpreservation.fixity.core.digests.Algorithms;
import org.openpreservation.fixity.core.digests.Hasher;
import org.openpreservation.fixity.core.paths.PathScanResult;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;

@NullMarked
public class ScanJob implements Job {
    @Override
    public void execute(@Nullable JobExecutionContext context) throws JobExecutionException {
        try {
            if (context == null) {
                throw new JobExecutionException("ScanJob: JobExecutionContext cannot be null");
            }
            JobDataMap dataMap = context.getJobDetail().getJobDataMap();
            String toScan = dataMap.getString("toScan");
            if (!dataMap.containsKey("algorithm")) {
                throw new JobExecutionException("ScanJob: 'algorithm' parameter is required");
            }
            @SuppressWarnings("null")
            Algorithms algorithm = Algorithms.fromString(dataMap.getString("algorithm"));
            if (toScan == null || toScan.isBlank() || algorithm == null) {
                throw new JobExecutionException("ScanJob: 'toScan' and 'algorithm' parameters are required and cannot be blank");
            }
            executeScan(toScan, algorithm, createScanUpdater());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new JobExecutionException("Failed to execute ScanJob", e);
        }
    }

    void executeScan(String toScan, Algorithms algorithm, ScanUpdater updater) throws IOException, NoSuchAlgorithmException, JobExecutionException {
        Path scanPath = findCollectionPathForScan(toScan);
        BatchScanner scanner = new BatchScanner();
        @SuppressWarnings("null")
        PathScanResult result = scanner.scan(scanPath, Hasher.instance(EnumSet.of(algorithm)), true);
        @NonNull PathScan scan = scanner.getScan();
        scan.updateFrom(result);
        updater.updateDatabase(scan);
    }

    Path findCollectionPathForScan(String toScan) throws JobExecutionException {
        Path scanPath = Path.of(toScan);
        if (!scanPath.toFile().exists() || !scanPath.toFile().isDirectory() || !scanPath.toFile().canRead()) {
            throw new JobExecutionException("ScanJob: Supplied path must be an existing, readable directory: " + scanPath);
        }
        return scanPath;
    }

    private ScanUpdater createScanUpdater() throws JobExecutionException {
        CollectionPathDAO cpDAO = new CollectionPathDAO(OpenFixityServer.getSessionFactory());
        @Nullable ScanUpdater updater = new UnitOfWorkAwareProxyFactory(OpenFixityServer.getHibernate())
                .create(ScanUpdater.class, CollectionPathDAO.class, cpDAO);
        return updater;
    }

}
