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
