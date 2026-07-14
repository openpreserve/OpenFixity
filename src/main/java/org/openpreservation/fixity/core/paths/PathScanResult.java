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
package org.openpreservation.fixity.core.paths;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface PathScanResult {
    public Path getPath();
    public Folder getRootFolder();
    public long getFilesScanned();
    public long getBytesScanned();
    public PathSummary getSummary();
    public Set<@NonNull Folder> getFolders();
    public Set<@NonNull FileScanResult> getResults();

    static final class PathScanResultImpl implements PathScanResult {
        private final Path path;
        private final Folder rootFolder;
        private final long filesScanned;
        private final long bytesScanned;
        private final PathSummary summary;
        private final Set<@NonNull Folder> folders;

        private PathScanResultImpl(final Path path,
                                   final Folder rootFolder,
                                   final long filesScanned,
                                   final long bytesScanned,
                                   final PathSummary summary,
                                   final Set<@NonNull Folder> folders) {
            super();
            this.path = path;
            this.rootFolder = rootFolder;
            this.filesScanned = filesScanned;
            this.bytesScanned = bytesScanned;
            this.summary = summary;
            Set<@NonNull Folder> validFolders = Collections.unmodifiableSet(folders);
            this.folders = (validFolders != null) ? validFolders : new LinkedHashSet<>();
        }

        @Override
        @NonNull
        public Path getPath() {
            return this.path;
        }

        @Override
        @NonNull
        public Folder getRootFolder() {
            return this.rootFolder;
        }

        @Override
        public long getFilesScanned() {
            return this.filesScanned;
        }

        @Override
        public long getBytesScanned() {
            return this.bytesScanned;
        }

        @Override
        public Set<@NonNull Folder> getFolders() {
            return this.folders;
        }

        public PathSummary getSummary() {
            return this.summary;
        }

        @Override
        public Set<@NonNull FileScanResult> getResults() {
            Set<@NonNull FileScanResult> results = this.folders.stream()
                    .flatMap(folder -> folder.getFileScanResults().stream())
                    .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
            return (results != null) ? results : new LinkedHashSet<>();
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, rootFolder, filesScanned, bytesScanned, summary, folders);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof PathScanResultImpl))
                return false;
            PathScanResultImpl other = (PathScanResultImpl) obj;
            return Objects.equals(path, other.path) && Objects.equals(rootFolder, other.rootFolder)
                    && filesScanned == other.filesScanned && bytesScanned == other.bytesScanned
                    && Objects.equals(summary, other.summary) && Objects.equals(folders, other.folders);
        }

    }

    public static final class Builder {
        private Path path;
        private Folder rootFolder;
        private long filesScanned = 0L;
        private long bytesScanned = 0L;
        private PathSummary summary;
        private Set<@NonNull Folder> folders = new LinkedHashSet<>();

        private Builder(final Path path, final PathSummary summary, final Folder rootFolder) {
            this.path = path;
            this.summary = summary;
            this.rootFolder = rootFolder;
            this.folders.add(rootFolder);
        }

        public Builder withFilesScanned(final long filesScanned) {
            this.filesScanned = filesScanned;
            return this;
        }

        public Builder addFilesScanned(final long filesScanned) {
            this.filesScanned += filesScanned;
            return this;
        }

        public Builder withBytesScanned(final long bytesScanned) {
            this.bytesScanned = bytesScanned;
            return this;
        }

        public Builder addBytesScanned(final long bytesScanned) {
            this.bytesScanned += bytesScanned;
            return this;
        }

        public Builder addFolder(final Folder folder) {
            if (!folder.isRoot() && !this.folders.contains(folder.getParent())) {
                throw new IllegalArgumentException("Folder relative path must be a child of the scan path");
            }
            this.folders.add(folder);
            return this;
        }

        public PathScanResult build() {
            return new PathScanResultImpl(path, rootFolder, filesScanned, bytesScanned, summary, folders);
        }
    }

    public static Builder builder(final Path path, final PathSummary summary, final Folder rootFolder) {
        return new Builder(path, summary, rootFolder);
    }

    public static PathScanResult of(final Path path, final Folder rootFolder, final long filesScanned,
                                        final long bytesScanned, final PathSummary summary, final Set<@NonNull Folder> folders) {
        return new PathScanResultImpl(path, rootFolder, filesScanned, bytesScanned, summary, folders);
    }
}
