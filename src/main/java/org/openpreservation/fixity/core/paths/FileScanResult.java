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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.openpreservation.fixity.core.digests.DigestResult;
import org.openpreservation.fixity.core.digests.Hasher;

@NullMarked
public interface FileScanResult {
    public static final String UNKNOWN_CONTENT_TYPE = "application/octet-stream";
    /**
     * The path that was scanned.
     * @return
     */
    public Path getPath();
    public long getLength();
    public @Nullable LocalDateTime getCreated();
    public @Nullable LocalDateTime getModified();
    public Set<? extends DigestResult> getDigestResults();
    public FileScanStatus getStatus();
    public LocalDateTime getScanned();

    static final class FileScanResultImpl implements FileScanResult {
        private final Path path;
        private final long length;
        private final @Nullable LocalDateTime created;
        private final @Nullable LocalDateTime modified;
        private final Set<@NonNull DigestResult> digestResults;
        private final LocalDateTime scanned;
        private final FileScanStatus status;

        @SuppressWarnings("null")
        private FileScanResultImpl(final Path path,
                                   final long length,
                                   final @Nullable LocalDateTime created,
                                   final @Nullable LocalDateTime modified,
                                   final Set<@NonNull DigestResult> digestResults,
                                   final LocalDateTime scanned,
                                   final FileScanStatus status) {
            this.path = path;
            this.length = length;
            this.created = created;
            this.modified = modified;
            this.digestResults = Collections.unmodifiableSet(digestResults);
            this.scanned = scanned;
            this.status = status;
        }
        @Override
        public Path getPath() {
            return this.path;
        }
        @Override
        public long getLength() {
            return this.length;
        }
        @Override
        @Nullable 
        public LocalDateTime getCreated() {
            return this.created;
        }
        @Override
        @Nullable 
        public LocalDateTime getModified() {
            return this.modified;
        }
        @Override
        public Set<@NonNull ? extends DigestResult> getDigestResults() {
            return this.digestResults;
        }
        @Override
        public FileScanStatus getStatus() {
            return this.status;
        }
        @Override
        public LocalDateTime getScanned() {
            return this.scanned;
        }
        @Override
        public int hashCode() {
            return Objects.hash(path, length, created, modified, digestResults, scanned, status);
        }
        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof FileScanResultImpl))
                return false;
            FileScanResultImpl other = (FileScanResultImpl) obj;
            return Objects.equals(path, other.path) && length == other.length && Objects.equals(created, other.created)
                    && Objects.equals(modified, other.modified) && Objects.equals(digestResults, other.digestResults)
                    && Objects.equals(scanned, other.scanned) && status == other.status;
        }

        static final class Builder {
            private Path path;
            private long length = -1L;
            private @Nullable LocalDateTime created = null;
            private @Nullable LocalDateTime modified = null;
            private Set<@NonNull DigestResult> digestResults = new HashSet<>();
            @SuppressWarnings("null")
            private LocalDateTime scanned = LocalDateTime.now();
            private FileScanStatus status = FileScanStatus.IGNORED;
            private Builder(final Path path) { this.path = path; }
            static Builder of(final Path path) { return new Builder(path); } 
            Builder withPath(final Path path) { this.path = path; return this; }
            Builder withLength(final long length) { this.length = length; return this; }
            Builder withCreated(final @Nullable LocalDateTime created) { this.created = created; return this; }
            Builder withModified(final @Nullable LocalDateTime modified) { this.modified = modified; return this; }
            Builder withDigestResults(final Set<@NonNull DigestResult> digestResults) { this.digestResults = digestResults; return this; }
            Builder addDigestResult(final DigestResult digestResult) {
                this.digestResults.add(digestResult);
                return this;
            }
            Builder withScanned(final LocalDateTime scanned) { this.scanned = scanned; return this; }
            Builder withStatus(final FileScanStatus status) { this.status = status; return this; }
            FileScanResultImpl build() {
                return new FileScanResultImpl(this.path, this.length, this.created, this.modified, this.digestResults, this.scanned, this.status);
            }
        }

        static FileScanResult of(final Path toScan, final Hasher hasher) throws FileNotFoundException {
            if (Files.isDirectory(toScan)) throw new IllegalArgumentException("Path to scan cannot be a directory: " + toScan.toString());
            FileScanResultImpl.Builder builder = FileScanResultImpl.Builder.of(toScan);
            if (Files.notExists(toScan)) return builder.withStatus(FileScanStatus.NOTFOUND).build();
            if (!Files.isReadable(toScan)) return builder.withStatus(FileScanStatus.DENIED).build();
            try {
                builder.withLength(Files.size(toScan));
                @SuppressWarnings("null")
                final BasicFileAttributes attrs = Files.readAttributes(toScan, BasicFileAttributes.class);
                if (attrs.creationTime() != null) {
                    builder.withCreated(attrs.creationTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                }
                if (attrs.lastModifiedTime() != null) {
                    builder.withModified(attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                }
                try (final FileInputStream fis = new FileInputStream(toScan.toFile())) {
                    builder.withDigestResults(hasher.hash(fis));
                }
                return builder.withStatus(FileScanStatus.SCANNED).build();
            } catch (IOException e) {
                return builder.withStatus(FileScanStatus.DAMAGED).build();
            }
        }
    }

    public static FileScanResult of(final Path toScan, final Hasher hasher) throws FileNotFoundException {
        return FileScanResultImpl.of(toScan, hasher);
    }
}