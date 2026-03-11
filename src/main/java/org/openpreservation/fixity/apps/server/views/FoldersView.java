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
