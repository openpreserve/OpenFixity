package org.openpreservation.fixity.apps.server.views;

import java.util.Collections;
import java.util.List;

import org.openpreservation.fixity.apps.dao.CollectionPath;
import org.openpreservation.fixity.core.digests.Algorithms;

public class PathsView extends FixityAppView {
    private final List<CollectionPath> collectionPaths;
    public PathsView(List<CollectionPath> collectionPaths) {
        super("paths.mustache", "paths");
        this.collectionPaths = collectionPaths == null ? Collections.emptyList() : Collections.unmodifiableList(collectionPaths);
    }

    public List<CollectionPath> getCollectionPaths() {
        return this.collectionPaths.stream().filter(cp -> !cp.getRegisteredPaths().isEmpty()).toList();
    }

    public String getDefaultAlgorithm() {
        return Algorithms.DEFAULT.getName();
    }
}
