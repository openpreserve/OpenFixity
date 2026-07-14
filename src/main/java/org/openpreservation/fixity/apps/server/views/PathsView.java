/*
 * OpenFixity is an application for monitoring and reporting on the fixity of files.
 * Copyright (C) 2026 Open Preservation Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
