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
import java.util.Set;
import java.util.Stack;

import org.openpreservation.fixity.apps.server.resources.api.FolderInfoResource.FolderInfo;

public class FoldersView extends FixityAppView {
    private final FolderInfo folderInfo;
    private final Set<FolderInfo> children;
    private final Stack<FolderInfo> ancestors;

    public FoldersView(final FolderInfo folderInfo,
                       final Set<FolderInfo> children,
                       final Stack<FolderInfo> ancestors) {
        super("folders.mustache", "folders");
        this.folderInfo = folderInfo;
        this.children = Collections.unmodifiableSet(children);
        this.ancestors = ancestors;
    }

    public Set<FolderInfo> getChildren() {
        return this.children;
    }

    public Stack<FolderInfo> getAncestors() {
        if (this.ancestors.isEmpty()) {
            return ancestors;
        }
        Stack<FolderInfo> reversedAncestors = new Stack<>();
        for (int i = this.ancestors.size() - 1; i >= 0; i--) {
            reversedAncestors.push(this.ancestors.get(i));
        }
        return reversedAncestors;
    }

    public FolderInfo getFolder() {
        return this.folderInfo;
    }
}
