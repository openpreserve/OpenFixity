package org.openpreservation.fixity.apps.server.resources.views;

import java.util.List;

import org.openpreservation.fixity.apps.dao.DataFactory;
import org.openpreservation.fixity.apps.dao.FolderScanRecord;
import org.openpreservation.fixity.apps.dao.PathScan;
import org.openpreservation.fixity.apps.server.exceptions.OpenFixityException;
import org.openpreservation.fixity.apps.server.views.PathScanView;
import org.openpreservation.fixity.apps.server.views.PathScansView;

import io.dropwizard.hibernate.UnitOfWork;
import jakarta.persistence.NoResultException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/scans")
public class PathScansResource {
    private final DataFactory dataFactory;

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
    @GET
    @Path("/{scanId}/")
    public PathScanView getScan(@PathParam("scanId") Long scanId) {
        if (scanId == null) {
            throw OpenFixityException.of(new BadRequestException("PathScan ID can not be null."), "PathScan.id: " + scanId);
        }
        try {
            PathScan scan = dataFactory.pathScanDAO().findById(scanId);
            List<FolderScanRecord> allFolders = dataFactory.folderScanRecordDAO().findByPathScan(scan);
            FolderScanRecord rootFolder = FolderScanRecord.findRoot(allFolders);
            List<FolderScanRecord> subfolders = FolderScanRecord.directChildren(allFolders, "");
            return new PathScanView(scan, rootFolder, subfolders, null);
        } catch (NoResultException e) {
            throw OpenFixityException.of(new NotFoundException("PathScan with id " + scanId + " not found.", e), "PathScan.id: " + scanId);
        }
    }

    @UnitOfWork
    @GET
    @Path("/{scanId}/folders/{folderId}/")
    public PathScanView getScanFolder(@PathParam("scanId") Long scanId,
                                      @PathParam("folderId") Long folderId) {
        if (scanId == null) {
            throw OpenFixityException.of(new BadRequestException("PathScan ID can not be null."), "PathScan.id: " + scanId);
        }
        try {
            PathScan scan = dataFactory.pathScanDAO().findById(scanId);
            FolderScanRecord folder = dataFactory.folderScanRecordDAO().findById(folderId);
            List<FolderScanRecord> allFolders = dataFactory.folderScanRecordDAO().findByPathScan(scan);
            List<FolderScanRecord> subfolders = FolderScanRecord.directChildren(allFolders, folder.getRelativePath());
            Long parentId = FolderScanRecord.parentId(allFolders, folder);
            return new PathScanView(scan, folder, subfolders, parentId);
        } catch (NoResultException e) {
            throw OpenFixityException.of(new NotFoundException("Folder or scan not found.", e), "scanId: " + scanId + ", folderId: " + folderId);
        }
    }
}
