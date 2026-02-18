package org.openpreservation.fixity.apps.server.views;

import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.openpreservation.fixity.apps.dao.Collection;
import org.openpreservation.fixity.apps.dao.PathRegistration;

public class CollectionView extends FixityAppView {
    final Collection collection;

    public CollectionView(final Collection collection) {
        super("collection.mustache", "collections");
        this.collection = collection;
    }

    public Collection getCollection() {
        return this.collection;
    }

    public Set<@NonNull PathRegistration> getRegisteredPaths() {
        return this.collection.getRegisteredPaths();
    }
}
