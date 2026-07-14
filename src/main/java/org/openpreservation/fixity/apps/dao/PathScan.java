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
package org.openpreservation.fixity.apps.dao;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.openpreservation.fixity.core.paths.FileScanResult;
import org.openpreservation.fixity.core.paths.FileScanStatus;
import org.openpreservation.fixity.core.paths.PathScanResult;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity(name = "PathScan")
@Table(name = "PathScan")
@NamedQueries({
    @NamedQuery(name = "PathScan.findAll", query = "SELECT ps FROM PathScan ps JOIN FETCH ps.collectionPath"),
    @NamedQuery(name = "PathScan.findByCollectionPath", query = "SELECT ps FROM PathScan ps WHERE ps.collectionPath = :collectionPath")
})
@NullMarked
public class PathScan implements Serializable {
    private static final long serialVersionUID = 4839201748392017493L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;
    @JsonBackReference
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_path_id")
    private CollectionPath collectionPath;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ScanStatus status;
    @OneToOne(cascade = CascadeType.ALL, optional = false)
    private PathSummaryRecord summary;
    @Column(nullable = false)
    private final LocalDateTime started;
    private @Nullable LocalDateTime stopped;
    @OneToMany(mappedBy = "pathScan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<@NonNull FolderScanRecord> folders;

    @SuppressWarnings("null")
    PathScan() {
        // For JPA - fields populated by reflection
        this(null, null, null, LocalDateTime.MIN, null, new HashSet<>());
    }

    private PathScan(final CollectionPath collectionPath, final ScanStatus status, final PathSummaryRecord summary,
                     final LocalDateTime started, final @Nullable LocalDateTime stopped,
                     final Set<@NonNull FolderScanRecord> folders) {
        this.collectionPath = collectionPath;
        this.status = status;
        this.summary = summary;
        this.started = started;
        this.stopped = stopped;
        this.folders = folders;
    }

    public static PathScan of(final CollectionPath collectionPath, final PathSummaryRecord summary) {
        return new PathScan(collectionPath, ScanStatus.INITIALISED, summary, LocalDateTime.now(), null, new HashSet<>());
    }

    @Nullable
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setCollectionPath(final CollectionPath collectionPath) {
        this.collectionPath = collectionPath;
    }

    public CollectionPath getCollectionPath() {
        return collectionPath;
    }

    public ScanStatus getStatus() {
        return status;
    }

    public PathSummaryRecord getSummary() {
        return summary;
    }

    public String getDuration() {
        if (stopped == null) return Duration.between(started, LocalDateTime.now()).toSeconds() + "s";
        return Duration.between(started, stopped).toSeconds() + "s";
    }

    public LocalDateTime getStarted() {
        return started;
    }

    @Nullable
    public LocalDateTime getStopped() {
        return stopped;
    }

    public Set<@NonNull FolderScanRecord> getFolders() {
        return folders;
    }

    public Set<@NonNull FileScanRecord> getAllFiles() {
        return this.folders.stream()
                .flatMap(f -> f.getFiles().stream())
                .collect(Collectors.toSet());
    }

    public int getResultCount() {
        return (int) this.folders.stream().mapToLong(f -> f.getFiles().size()).sum();
    }

    public void addFile(final FileScanRecord record) {
        String folderPath = parentFolderPath(record.relativePath());
        FolderScanRecord folder = getOrCreateFolder(folderPath);
        record.setFolder(folder);
        folder.addFile(record);
    }

    FolderScanRecord getOrCreateFolder(final String relativePath) {
        return this.folders.stream()
                .filter(f -> f.getRelativePath().equals(relativePath))
                .findFirst()
                .orElseGet(() -> {
                    if (!relativePath.isEmpty()) {
                        int lastSlash = relativePath.lastIndexOf('/');
                        String parentPath = lastSlash >= 0 ? relativePath.substring(0, lastSlash) : "";
                        getOrCreateFolder(parentPath);
                    }
                    FolderScanRecord newFolder = FolderScanRecord.of(this, relativePath);
                    this.folders.add(newFolder);
                    return newFolder;
                });
    }

    public FileScanRecord addResultForDeleted(final FileScanRecord previousResult) {
        final FileScanRecord record = FileScanRecord.deleted(this, previousResult);
        addFile(record);
        return record;
    }

    public Long hashId() {
        return this.hashCode() * 31L + (this.id != null ? this.id : 0L);
    }

    public void updateFrom(final PathScanResult result) throws IOException {
        if (!Files.isSameFile(this.collectionPath.getRoot(), result.getPath()))
            throw new IllegalArgumentException("PathScanResult path " + result.getPath() + " does not match CollectionPath root " + this.collectionPath.getRoot());
        this.status = ScanStatus.COMPLETED;
        this.stopped = LocalDateTime.now();
        if (this.folders.isEmpty() && result.getResults() != null && !result.getResults().isEmpty()) {
            for (final FileScanResult fsr : result.getResults()) {
                addFile(FileScanRecord.of(this, fsr));
            }
        }
        getOrCreateFolder(""); // ensure root folder always exists even if it has no direct files
        long damaged = getAllFiles().stream().filter(r -> r.getStatus() == FileScanStatus.DAMAGED).count();
        long denied = getAllFiles().stream().filter(r -> r.getStatus() == FileScanStatus.DENIED).count();
        this.summary = PathSummaryRecord.of(result.getSummary(), damaged, denied);
    }

    @SuppressWarnings("null")
    private static String parentFolderPath(final String relativePath) {
        Path parent = Path.of(relativePath).getParent();
        return parent != null ? parent.toString() : "";
    }

    public boolean isCompleted() {
        return this.status == ScanStatus.COMPLETED;
    }

    public boolean isDamaged() {
        return getAllFiles().stream().anyMatch(r -> r.getStatus() == FileScanStatus.DAMAGED);
    }

    public List<@NonNull FileScanRecord> getDamagedResults() {
        return Collections.unmodifiableList(getAllFiles()
                .stream().filter(r -> r.getStatus() == FileScanStatus.DAMAGED).toList());
    }

    public long getDamagedCount() {
        return this.summary.getDamagedCount();
    }

    public boolean isDenied() {
        return getAllFiles().stream().anyMatch(r -> r.getStatus() == FileScanStatus.DENIED);
    }

    public List<@NonNull FileScanRecord> getDeniedResults() {
        return Collections.unmodifiableList(getAllFiles()
                .stream().filter(r -> r.getStatus() == FileScanStatus.DENIED).toList());
    }

    public long getDeniedCount() {
        return this.summary.getDeniedCount();
    }
}
