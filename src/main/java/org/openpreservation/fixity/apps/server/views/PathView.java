package org.openpreservation.fixity.apps.server.views;

import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.openpreservation.fixity.apps.dao.CollectionPath;
import org.openpreservation.fixity.apps.dao.PathRegistration;
import org.openpreservation.fixity.core.digests.Algorithms;

public class PathView extends FixityAppView {
    private final CollectionPath collectionPath;
    public PathView(CollectionPath collectionPath) {
        super("path.mustache", "paths");
        this.collectionPath = collectionPath;
    }

    public CollectionPath getCollectionPath() {
        return this.collectionPath;
    }

    public Set<@NonNull PathRegistration> getRegisteredPaths() {
        return this.collectionPath.getRegisteredPaths();
    }

    public String getDefaultAlgorithm() {
        return Algorithms.DEFAULT.getName();
    }
}
