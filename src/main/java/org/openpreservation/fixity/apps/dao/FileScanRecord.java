package org.openpreservation.fixity.apps.dao;

import java.io.Serializable;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.openpreservation.fixity.core.digests.DigestResult;
import org.openpreservation.fixity.core.paths.FileScanResult;
import org.openpreservation.fixity.core.paths.FileScanStatus;
import org.openpreservation.fixity.core.paths.Folder;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity()
@Table()
@NamedQuery(name = "FileScanRecord.findAll", query = "SELECT fsr FROM FileScanRecord fsr")
@NullMarked
public final class FileScanRecord implements FileScanResult, Serializable {
    private static final long serialVersionUID = 8359274652193847123L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "path_scan_id", nullable = false)
    private final PathScan execution;
    @Column(nullable = false, length = 65535)
    private final String relativePath;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PathAuditStatus auditStatus;
    @Column(nullable = true)
    private final @Nullable Long length;
    @Column(nullable = true)
    private final @Nullable LocalDateTime created;
    @Column(nullable = true)
    private final @Nullable LocalDateTime modified;
    @Column(nullable = false)
    private final LocalDateTime scanned;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private final FileScanStatus status;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private FolderScanRecord folder;
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 50)
    @JoinTable(name = "digest_calculation",
        joinColumns = @jakarta.persistence.JoinColumn(name = "file_content_result_id"),
        inverseJoinColumns = @jakarta.persistence.JoinColumn(name = "digest_result_id")
    )
    private Set<DigestRecord> digestResults = new LinkedHashSet<>();

    @SuppressWarnings("null")
    private FileScanRecord() {
        // For JPA
        this(null, null, null, null, null, null, null, null, null);
    }

    @SuppressWarnings("null")
    private FileScanRecord(final PathScan execution, final FileScanResult result) {
        this(execution,
             Folder.relativise(execution.getCollectionPath().getRoot().toString(), result.getPath()),
             noPreviousStatus(result.getStatus()),
             result.getLength(),
             result.getCreated(),
             result.getModified(),
             result.getScanned(),
             result.getStatus(),
             result.getDigestResults().stream().map(DigestRecord::of).collect(Collectors.toSet()));
    }

    private FileScanRecord(final PathScan execution,
                           final String relativePath,
                           final PathAuditStatus auditStatus,
                           final @Nullable Long length,
                           final @Nullable LocalDateTime created,
                           final @Nullable LocalDateTime modified,
                           final LocalDateTime scanned,
                           final FileScanStatus status,
                           final Set<DigestRecord> digestResults) {
        super();
        this.execution = execution;
        this.relativePath = relativePath;
        this.auditStatus = auditStatus;
        this.created = created;
        this.modified = modified;
        this.length = length;
        this.scanned = scanned;
        this.status = status;
        if (digestResults != null) this.digestResults.addAll(digestResults);
    }

    public static final FileScanRecord of(final PathScan execution, final FileScanResult result) {
        if (!result.getPath().startsWith(execution.getCollectionPath().getRoot()))
            throw new IllegalArgumentException("Path " + result.getPath() + " is not under root " + execution.getCollectionPath().getRoot());
        return new FileScanRecord(execution, result);
    }

    public static final FileScanRecord deleted(final PathScan execution, final FileScanRecord previous) {
        if (!previous.getPath().startsWith(execution.getCollectionPath().getRoot()))
            throw new IllegalArgumentException("Path " + previous.getPath() + " is not under root " + execution.getCollectionPath().getRoot());
        return new FileScanRecord(execution,
                Folder.relativise(execution.getCollectionPath().getRoot().toString(), previous.getPath()),
                PathAuditStatus.NOTFOUND,
                previous.length,
                previous.created,
                previous.modified,
                previous.scanned,
                previous.status,
                previous.digestResults);
    }

    public PathAuditStatus updateStatus(final FileScanRecord previous) {
        if (this.status == previous.status) return this.auditStatus = processIdenticalStatus(this, previous);
        return this.auditStatus = processChangedStatus(this, previous);
    }

    private static PathAuditStatus processChangedStatus(final FileScanRecord latest, final FileScanRecord previous) {
        // The file was absent at the previous scan and is present now, so it really is new.
        if (previous.status == FileScanStatus.NOTFOUND) {
            return PathAuditStatus.ADDED;
        }
        // The file is readable again after being damaged, denied or ignored. It is not a
        // new file, so compare it against its last known digest rather than reporting
        // ADDED. checkDigests yields UNVERIFIED when there is no digest to compare against.
        if (latest.status == FileScanStatus.SCANNED) {
            return checkDigests(latest, previous);
        }
        // Otherwise the file has become unreadable since the last scan. Report why.
        return noPreviousStatus(latest.status);
    }

    private static final PathAuditStatus noPreviousStatus(FileScanStatus status) {
        switch (status) {
            case NOTFOUND: return PathAuditStatus.NOTFOUND;
            case DAMAGED: return PathAuditStatus.DAMAGED;
            case DENIED: return PathAuditStatus.DENIED;
            case IGNORED: return PathAuditStatus.IGNORED;
            case SCANNED: return PathAuditStatus.ADDED;
        }
        return PathAuditStatus.ADDED;
    }

    private static PathAuditStatus processIdenticalStatus(final FileScanRecord latest, final FileScanRecord previous) {
        if (latest.status == FileScanStatus.SCANNED && previous.auditStatus == PathAuditStatus.NOTFOUND) {
            return PathAuditStatus.ADDED;
        }
        if (latest.status == FileScanStatus.SCANNED && (previous.auditStatus == PathAuditStatus.VERIFIED || previous.auditStatus == PathAuditStatus.ADDED)) {
            return checkDigests(latest, previous);
        }
        return previous.getAuditStatus();
    }

    private static PathAuditStatus checkDigests(final FileScanResult latest, final FileScanResult previous) {
        for (final DigestResult result : latest.getDigestResults()) {
            for (final DigestResult previousResult : previous.getDigestResults()) {
                if (result.getAlgorithm().equals(previousResult.getAlgorithm())) {
                    return result.toHexString().equals(previousResult.toHexString()) ?
                            PathAuditStatus.VERIFIED : PathAuditStatus.CHANGED;
                }
            }
        }
        return PathAuditStatus.UNVERIFIED;
    }

    @Nullable
    public Long getId() {
        return this.id;
    }

    public FolderScanRecord getFolder() {
        return this.folder;
    }

    void setFolder(final FolderScanRecord folder) {
        this.folder = folder;
    }

    PathScan execution() {
        return this.execution;
    }

    public String relativePath() {
        return this.relativePath;
    }

    @Nullable
    public DigestRecord getDigestRecord() {
        return this.digestResults.stream().findFirst().orElse(null);
    }

    @Override
    public Path getPath() {
        return this.execution.getCollectionPath().getRoot().resolve(this.relativePath);
    }

    @Override
    public FileScanStatus getStatus() {
        return this.status;
    }

    public PathAuditStatus getAuditStatus() {
        return this.auditStatus;
    }

    @Override
    @Nullable
    public LocalDateTime getCreated() {
        return this.created;
    }

    @Override
    public Set<? extends DigestResult> getDigestResults() {
        return Collections.unmodifiableSet(this.digestResults);
    }

    @Override
    public long getLength() {
        return (this.length == null) ? 0L : this.length;
    }

    @Override
    @Nullable
    public LocalDateTime getModified() {
        return this.modified;
    }

    @Override
    public LocalDateTime getScanned() {
        return this.scanned;
    }
}
