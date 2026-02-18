package org.openpreservation.fixity.apps.schedule;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import org.openpreservation.fixity.apps.dao.PathScan;
import org.openpreservation.fixity.apps.server.resources.views.PathScansResource;
import org.openpreservation.fixity.core.digests.Algorithms;
import org.openpreservation.fixity.core.digests.Hasher;
import org.openpreservation.fixity.core.paths.PathScanResult;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ScanJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException{
        try {
            JobDataMap dataMap = context.getJobDetail().getJobDataMap();
            String toScan = dataMap.getString("toScan");
            Algorithms algorithm = Algorithms.fromString(dataMap.getString("algorithm")); 
            if (toScan == null || toScan.isBlank() || algorithm == null) {
                throw new JobExecutionException("ScanJob: 'toScan' and 'algorithm' parameters are required and cannot be blank");
            }
            Path scanPath = findCollectionPathForScan(toScan);
            BatchScanner scanner = new BatchScanner();
            PathScanResult result = scanner.scan(scanPath, Hasher.instance(algorithm), true);
            PathScan scan = scanner.getScan();
            scan.updateFrom(result);
            scan.setId(Long.valueOf(scan.hashCode()));
            PathScansResource.addScan(scan);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new JobExecutionException("Failed to execute ScanJob", e);
        }
    }

    private Path findCollectionPathForScan(String toScan) throws JobExecutionException {
        Path scanPath = Path.of(toScan);
        if (!scanPath.toFile().exists() || !scanPath.toFile().isDirectory() || !scanPath.toFile().canRead()) {
            throw new JobExecutionException("ScanJob: Supplied path must be an existing, readable directory: " + scanPath);
        }
        return scanPath;
    }
}
