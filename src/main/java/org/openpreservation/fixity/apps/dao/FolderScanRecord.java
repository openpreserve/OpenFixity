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

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity(name = "FolderScanRecord")
@Table(name = "FolderScanRecord")
@NamedQuery(name = "FolderScanRecord.findByPathScan",
        query = "SELECT f FROM FolderScanRecord f WHERE f.pathScan = :pathScan")
@NullMarked
public class FolderScanRecord implements Serializable {
    private static final long serialVersionUID = 7123456789012345678L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "path_scan_id", nullable = false)
    private PathScan pathScan;

    @Column(nullable = false, length = 65535)
    private String relativePath;

    @JsonBackReference
    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 100)
    private Set<FileScanRecord> files = new LinkedHashSet<>();

    @SuppressWarnings("null")
    FolderScanRecord() {
        // For JPA
        this(null, "");
    }

    private FolderScanRecord(final PathScan pathScan, final String relativePath) {
        this.pathScan = pathScan;
        this.relativePath = relativePath;
    }

    public static FolderScanRecord of(final PathScan pathScan, final String relativePath) {
        Objects.requireNonNull(pathScan, "pathScan must not be null");
        Objects.requireNonNull(relativePath, "relativePath must not be null");
        return new FolderScanRecord(pathScan, relativePath);
    }

    @Nullable
    public Long getId() {
        return id;
    }

    public PathScan getPathScan() {
        return pathScan;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getName() {
        if (relativePath.isEmpty()) return "[root]";
        int lastSlash = relativePath.lastIndexOf('/');
        return lastSlash >= 0 ? relativePath.substring(lastSlash + 1) : relativePath;
    }

    public Set<FileScanRecord> getFiles() {
        return Collections.unmodifiableSet(files);
    }

    void addFile(final FileScanRecord file) {
        this.files.add(file);
    }

    public static FolderScanRecord findRoot(final List<FolderScanRecord> folders) {
        return folders.stream()
                .filter(f -> f.getRelativePath().isEmpty())
                .findFirst()
                .orElseThrow(() -> new java.util.NoSuchElementException("Root folder not found."));
    }

    public static List<FolderScanRecord> directChildren(final List<FolderScanRecord> allFolders,
                                                         final String parentPath) {
        final String prefix = parentPath.isEmpty() ? "" : parentPath + "/";
        return allFolders.stream()
                .filter(f -> !f.getRelativePath().isEmpty())
                .filter(f -> {
                    if (!f.getRelativePath().startsWith(prefix)) return false;
                    String remainder = f.getRelativePath().substring(prefix.length());
                    return !remainder.isEmpty() && !remainder.contains("/");
                })
                .toList();
    }

    public static @Nullable Long parentId(final List<FolderScanRecord> allFolders,
                                           final FolderScanRecord folder) {
        String path = folder.getRelativePath();
        if (path.isEmpty()) return null;
        int lastSlash = path.lastIndexOf('/');
        String parentPath = lastSlash >= 0 ? path.substring(0, lastSlash) : "";
        return allFolders.stream()
                .filter(f -> f.getRelativePath().equals(parentPath))
                .findFirst()
                .map(FolderScanRecord::getId)
                .orElse(null);
    }
}
