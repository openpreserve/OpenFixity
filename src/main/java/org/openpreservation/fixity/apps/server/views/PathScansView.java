package org.openpreservation.fixity.apps.server.views;

import java.util.Collection;
import java.util.Collections;

import org.openpreservation.fixity.apps.dao.PathScan;

public class PathScansView extends FixityAppView {
    private final Collection<PathScan> scans;
    public PathScansView(Collection<PathScan> scans) {
        super("path_scans.mustache", "scans");
        this.scans = scans == null ? Collections.emptyList() : Collections.unmodifiableCollection(scans);
    }

    public Collection<PathScan> getScans() {
        return this.scans;
    }
}
