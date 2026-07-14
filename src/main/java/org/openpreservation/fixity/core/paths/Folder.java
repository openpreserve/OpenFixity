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
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;


@NullMarked
public interface Folder {
    public Path getRelativePath();

    @Nullable
    public Folder getParent();

    public boolean isRoot();

    public boolean isReadable();

    public Set<@NonNull FileScanResult> getFileScanResults();

    public void addFileScanResult(FileScanResult fileScanResult);

    class FolderImpl implements Folder {
        private final @Nullable Folder parent;
        private final Path relativePath;
        private final boolean isReadable;
        private final Set<@NonNull FileScanResult> fileScanResults = new HashSet<>();

        private FolderImpl(final @Nullable Folder parent, final Path relativePath, final boolean isReadable, final Set<@NonNull FileScanResult> fileScanResults) {
            super();
            this.parent = parent;
            this.relativePath = relativePath;
            this.isReadable = isReadable;
            this.fileScanResults.addAll(fileScanResults);
        }

        @Override
        public Path getRelativePath() {
            return this.relativePath;
        }

        @Override
        @Nullable
        public Folder getParent() {
            return this.parent;
        }

        @Override
        public boolean isReadable() {
            return this.isReadable;
        }

        @Override
        public boolean isRoot() {
            return this.parent == null;
        }

        @Override
        public Set<@NonNull FileScanResult> getFileScanResults() {
            return this.fileScanResults;
        }

        @Override
        public void addFileScanResult(final FileScanResult fileScanResult) {
            // getParent() is null for a path with no parent component. Such a path cannot be a
            // child of this folder, so reject it rather than dereferencing null.
            final Path parentPath = fileScanResult.getPath().getParent();
            if (parentPath == null || !parentPath.toString().equals(relativePath.toString())) {
                throw new IllegalArgumentException("FileScanResult path must be a child of the folder's relative path");
            }
            this.fileScanResults.add(fileScanResult);
        }

        @Override
        public int hashCode() {
            return Objects.hash(parent, relativePath, fileScanResults);
        }

        @Override
        public boolean equals(final @Nullable Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof FolderImpl))
                return false;
            final FolderImpl other = (FolderImpl) obj;
            return Objects.equals(parent, other.parent) && Objects.equals(relativePath, other.relativePath)
                    && Objects.equals(fileScanResults, other.fileScanResults);
        }
    }

    @SuppressWarnings("null")
    public static Folder unreadable(final @Nullable Folder parent, final Path relativePath) {
        return new FolderImpl(parent, relativePath, false, Set.of());
    }

    public static Folder readable(final @Nullable Folder parent, Path relativePath) {
        return new FolderImpl(parent, relativePath, true, new HashSet<>());
    }

    public static Folder of(final @Nullable Folder parent, Path relativePath, final Set<@NonNull FileScanResult> fileScanResults) {
        return new FolderImpl(parent, relativePath, true, fileScanResults);
    }

    @SuppressWarnings("null")
    public static Path resolve(final Path root, final Path relative) {
        return Optional.of(root.resolve(relative))
            .orElseThrow( () ->
                new IllegalArgumentException(String.format("Path '%s' cannot be resolved against root '%s'", relative, root)));
    }

    @SuppressWarnings("null")
    public static String relativise(final String root, final Path relative) {
        return Optional.of(Path.of(root).relativize(relative).toString())
            .orElseThrow( () ->
                new IllegalArgumentException(String.format("Path '%s' cannot be relativized against root '%s'", relative, root)));
    }

    @SuppressWarnings("null")
    public static String absolutePathString(final Path path) {
        return Optional.of(path.toAbsolutePath().normalize().toString())
            .orElseThrow(() -> 
                new IllegalArgumentException(String.format("Path '%s' cannot be resolved to an absolute path", path)));
    }

    @SuppressWarnings("null")
    public static Path getFileName(final Path path) {
        return Optional.ofNullable(path.getFileName())
            .orElseThrow(() -> 
                new IllegalArgumentException(String.format("Path '%s' does not have a file name", path)));
    }

    @SuppressWarnings("null")
    public static Path absolutePath(final Path path) {
        return Optional.of(path.toAbsolutePath().normalize())
            .orElseThrow(() -> 
                new IllegalArgumentException(String.format("Path '%s' cannot be resolved to an absolute path", path)));
    }
}
