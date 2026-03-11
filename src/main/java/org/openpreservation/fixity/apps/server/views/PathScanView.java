package org.openpreservation.fixity.apps.server.views;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.openpreservation.fixity.apps.dao.FolderScanRecord;
import org.openpreservation.fixity.apps.dao.PathScan;

public class PathScanView extends FixityAppView {
    private final PathScan scan;
    private final FolderScanRecord folder;
    private final List<FolderScanRecord> subfolders;
    private final @Nullable Long parentId;

    public PathScanView(final PathScan scan, final FolderScanRecord folder,
                        final List<FolderScanRecord> subfolders, final @Nullable Long parentId) {
        super("path_scan.mustache", "scans");
        this.scan = scan;
        this.folder = folder;
        this.subfolders = subfolders;
        this.parentId = parentId;
    }

    public PathScan getScan() {
        return this.scan;
    }

    public FolderScanRecord getFolder() {
        return this.folder;
    }

    public List<FolderScanRecord> getSubfolders() {
        return this.subfolders;
    }

    public boolean hasParent() {
        return parentId != null;
    }

    public @Nullable Long getParentId() {
        return parentId;
    }
}
