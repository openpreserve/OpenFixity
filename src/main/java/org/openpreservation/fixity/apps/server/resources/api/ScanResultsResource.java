package org.openpreservation.fixity.apps.server.resources.api;

import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.openpreservation.fixity.apps.dao.CollectionPath;
import org.openpreservation.fixity.apps.dao.DataFactory;
import org.openpreservation.fixity.apps.dao.FileScanRecord;
import org.openpreservation.fixity.apps.dao.FolderScanRecord;
import org.openpreservation.fixity.apps.dao.PathScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.dropwizard.hibernate.UnitOfWork;
import jakarta.persistence.NoResultException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@jakarta.ws.rs.Path("/api/paths")
public class ScanResultsResource {
    private static final Logger logger = LoggerFactory.getLogger(ScanResultsResource.class);
    private final DataFactory dataFactory;

    public ScanResultsResource(final DataFactory dataFactory) {
        this.dataFactory = dataFactory;
    }

    @UnitOfWork
    @GET
    @Produces("application/json")
    @jakarta.ws.rs.Path("/{pathId}/scans/")
    public List<@NonNull PathScan> getScansForPath(@PathParam("pathId") final Long pathId) {
        CollectionPath cp = findCollectionPath(pathId);
        return dataFactory.pathScanDAO().findByCollectionPath(cp);
    }

    @UnitOfWork
    @GET
    @Produces("application/json")
    @jakarta.ws.rs.Path("/{pathId}/scans/{scanId}/folders/")
    public List<@NonNull FolderScanRecord> getFoldersForScan(
            @PathParam("pathId") final Long pathId,
            @PathParam("scanId") final Long scanId) {
        findCollectionPath(pathId);
        PathScan scan = findScan(scanId);
        return dataFactory.folderScanRecordDAO().findByPathScan(scan);
    }

    @UnitOfWork
    @GET
    @Produces("application/json")
    @jakarta.ws.rs.Path("/{pathId}/scans/{scanId}/folders/{folderId}/results/")
    public List<@NonNull FileScanRecord> getResultsForFolder(
            @PathParam("pathId") final Long pathId,
            @PathParam("scanId") final Long scanId,
            @PathParam("folderId") final Long folderId) {
        findCollectionPath(pathId);
        findScan(scanId);
        FolderScanRecord folder = find("FolderScanRecord", folderId, () -> dataFactory.folderScanRecordDAO().findById(folderId));
        return folder.getFiles().stream().toList();
    }

    private CollectionPath findCollectionPath(final @NonNull Long pathId) {
        return find("CollectionPath", pathId, () -> dataFactory.collectionPathDAO().findById(pathId));
    }

    private PathScan findScan(final @NonNull Long scanId) {
        return find("PathScan", scanId, () -> dataFactory.pathScanDAO().findById(scanId));
    }

    private <T> T find(final String entityName, final Long id, final Supplier<T> lookup) {
        try {
            return lookup.get();
        } catch (NoResultException e) {
            logger.warn("{} with id {} not found", entityName, id, e);
            throw new NotFoundException(entityName + " with id " + id + " not found.");
        }
    }
}
