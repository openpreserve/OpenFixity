package org.openpreservation.fixity.apps.server.resources.views;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;
import org.openpreservation.fixity.apps.dao.CollectionPath;
import org.openpreservation.fixity.apps.dao.DataFactory;
import org.openpreservation.fixity.apps.dao.FileScanRecord;
import org.openpreservation.fixity.apps.dao.PathScan;
import org.openpreservation.fixity.apps.server.views.PathScanView;
import org.openpreservation.fixity.apps.server.views.PathScansView;

import io.dropwizard.hibernate.UnitOfWork;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("/scans")
public class PathScansResource {
    private final DataFactory dataFactory;
    private static final Map<Long,PathScan> scanCache = new ConcurrentHashMap<>();
    public PathScansResource(final DataFactory dataFactory) {
        super();
        this.dataFactory = dataFactory;
    }

    @UnitOfWork
    @GET
    public PathScansView getScans() {
        return new PathScansView(dataFactory.pathScanDAO().findAll());
    }

    @UnitOfWork
    @POST
    public Response clearScanCache() {
        for (PathScan scan : scanCache.values()) {
            try {
                scanCache.remove(scan.getId());
                scan.setId(null);
                CollectionPath collectionPath = dataFactory.collectionPathDAO().findByRoot(scan.getCollectionPath().getRoot()).orElseThrow(() -> new NotFoundException("CollectionPath with ID " + scan.getCollectionPath().getId() + " not found."));
                Optional<@NonNull PathScan> previousScan = collectionPath.getLatestScan();
                scan.setCollectionPath(collectionPath);
                updateScan(scan, previousScan.get());
                dataFactory.pathScanDAO().create(scan);
            } catch (SQLIntegrityConstraintViolationException e) {
                throw new BadRequestException("error clearing cache", e) ;
            }
        }
        return Response.ok().build();
    }

    @UnitOfWork
    @GET
    @Path("/{scanId}/")
    public PathScanView getScan(@PathParam("scanId") Long scanId) {
        PathScan scan = dataFactory.pathScanDAO().findById(scanId).orElseThrow(() -> new NotFoundException("No scan found with id: " + scanId));
        return new PathScanView(scan);
    }

    public static Long addScan(PathScan scan) {
        scanCache.putIfAbsent(scan.getId(), scan);
        return scan.getId();
    }

    private void updateScan(final PathScan latest, final PathScan previous) {
        for (FileScanRecord result : latest.getResults()) {
            FileScanRecord previousResult = getMatching(result, previous);
            // result.updateStatus(previousResult);
        }
        if (previous == null) return;
        for (FileScanRecord prevResult : previous.getResults()) {
            FileScanRecord latestResult = getMatching(prevResult, latest);
            if (latestResult == null) {
                latest.addResultForDeleted(prevResult);
            }
            
        }
    }

    private static FileScanRecord getMatching(FileScanRecord result, PathScan previous) {
        if (previous == null) return null;
        for (FileScanRecord previousResult : previous.getResults()) {
            if (previousResult.getPath().equals(result.getPath())) {
                return previousResult;
            }
        }
        return null;
    }

}
