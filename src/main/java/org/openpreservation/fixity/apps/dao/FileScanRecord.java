package org.openpreservation.fixity.apps.dao;

import java.io.Serializable;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.openpreservation.fixity.core.digests.DigestResult;
import org.openpreservation.fixity.core.paths.FileScanResult;
import org.openpreservation.fixity.core.paths.FileScanStatus;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity()
@Table()
public final class FileScanRecord implements org.openpreservation.fixity.core.paths.FileScanResult, Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "path_scan_id", nullable = false)
    private final PathScan execution;
    @Column(nullable = false)
    private final String relativePath;
    @Column(nullable = true)
    private final Long length;
    @Column(nullable = true)
    private final LocalDateTime created;
    @Column(nullable = true)
    private final LocalDateTime modified;
    @Column(nullable = false)
    private final LocalDateTime scanned;
    @Column(nullable = false)
    private final FileScanStatus status;
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "digest_calculation",
        joinColumns = @jakarta.persistence.JoinColumn(name = "file_content_result_id"),
        inverseJoinColumns = @jakarta.persistence.JoinColumn(name = "digest_result_id")   
    )
    private final Set<DigestRecord> digestResults = new LinkedHashSet<>();
    private FileScanRecord() {
        // For JPA
        this(null, null, null, null, null, null, null, null);
    }

    private FileScanRecord(final PathScan execution, final FileScanResult result) { 
        this(execution,
             relativizePath(execution.getCollectionPath().getRoot().toString(), result.getPath() ),
             result.getLength(),
             result.getCreated(),
             result.getModified(),
             result.getScanned(),
             result.getStatus(),
             result.getDigestResults().stream().map(DigestRecord::of).collect(Collectors.toSet()));
        }

    private FileScanRecord(final PathScan execution,
                           final String relativePath,
                           final Long length,
                           final LocalDateTime created,
                           final LocalDateTime modified,
                           final LocalDateTime scanned,
                           final FileScanStatus status,
                           final Set<DigestRecord> digestResults) {
        super();
        this.execution = execution;
        this.relativePath = relativePath;
        this.created = created;
        this.modified = modified;
        this.length = length;
        this.scanned = scanned;
        this.status = status;
        if (digestResults != null) this.digestResults.addAll(digestResults);
    }

    public static final FileScanRecord of(final PathScan execution, final FileScanResult result) {
        if ((result == null) || (execution == null)) throw new NullPointerException("FileScanResult or PathScan is null");
        if (!result.getPath().startsWith(execution.getCollectionPath().getRoot()))
            throw new IllegalArgumentException("Path " + result.getPath() + " is not under root " + execution.getCollectionPath().getRoot());
        if (result.getDigestResults() == null) throw new IllegalArgumentException("FileScanResult digestResults is null");
        return new FileScanRecord(execution, result);
    }

    public static final FileScanRecord deleted(final PathScan execution, final FileScanRecord previous) {
        if ((previous == null) || (execution == null)) throw new NullPointerException("FileScanResult or PathScan is null");
        if (!previous.getPath().startsWith(execution.getCollectionPath().getRoot()))
            throw new IllegalArgumentException("Path " + previous.getPath() + " is not under root " + execution.getCollectionPath().getRoot());
        return new FileScanRecord(execution, previous);
    }

    PathScan execution() {
        return this.execution;
    }

    public String relativePath() {
        return this.relativePath;
    }

    public DigestRecord getDigestRecord() {
        return this.digestResults.stream().findFirst().orElse(null);
    }

    @Override
    public Path getPath() {
        return this.execution.getCollectionPath().getRoot().resolve(this.relativePath);
    }

    public FileScanStatus getStatus() {
        return this.status;
    }

    private static String relativizePath(final String root, final Path relative) {
        return Path.of(root).relativize(relative).toString();
    }

    @Override
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
    public LocalDateTime getModified() {
        return this.modified;
    }

    @Override
    public LocalDateTime getScanned() {
        return this.scanned;
    }
}
