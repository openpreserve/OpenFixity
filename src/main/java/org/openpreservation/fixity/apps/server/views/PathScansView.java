package org.openpreservation.fixity.apps.server.views;

import java.util.Collection;
import java.util.Collections;

import org.jspecify.annotations.NonNull;
import org.openpreservation.fixity.apps.dao.PathScan;

public class PathScansView extends FixityAppView {
    private final @NonNull Collection<@NonNull PathScan> scans;
    @SuppressWarnings("null")
    public PathScansView(@NonNull Collection<@NonNull PathScan> scans) {
        super("path_scans.mustache", "scans");
        this.scans = scans == null ? Collections.emptyList() : Collections.unmodifiableCollection(scans);
    }

    public @NonNull Collection<@NonNull PathScan> getScans() {
        return this.scans;
    }
}
