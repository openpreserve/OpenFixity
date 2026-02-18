package org.openpreservation.fixity.apps.server.views;

import org.openpreservation.fixity.apps.dao.PathScan;

public class PathScanView extends FixityAppView {
    private final PathScan scan;
    public PathScanView(PathScan scan) {
        super("path_scan.mustache", "scans");
        this.scan = scan;
    }

    public PathScan getScan() {
        return this.scan;
    }
}
