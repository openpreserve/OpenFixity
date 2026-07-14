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
