package org.openpreservation.fixity.apps.server.views;

import java.util.Collections;
import java.util.List;

import org.openpreservation.fixity.apps.dao.Collection;

public class CollectionsView extends FixityAppView {
    private final List<Collection> collections;
    public CollectionsView(List<Collection> collections) {
        super("collections.mustache", "collections");
        this.collections = Collections.unmodifiableList(collections);
    }

    public List<Collection> getCollections() {
        return this.collections;
    }
}
