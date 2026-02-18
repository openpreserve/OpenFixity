package org.openpreservation.fixity.apps.dao;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.openpreservation.fixity.core.paths.FileScanStatus;
import org.openpreservation.fixity.core.paths.PathScanResult;
import org.openpreservation.fixity.core.paths.PathSummary;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity(name = "PathScan")
@Table(name = "PathScan")
@NamedQuery(name = "PathScan.findAll",query = "SELECT ps FROM PathScan ps")
public class PathScan implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonBackReference
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_path_id")
    private CollectionPath collectionPath;
    @Column(nullable = false)
    private ScanStatus status;
    @OneToOne(cascade = CascadeType.ALL, optional = false)
    private PathSummaryRecord summary;
    @Column(nullable = false)
    private final LocalDateTime started;
    private LocalDateTime stopped;
    @JsonManagedReference
    @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final Set<FileScanRecord> results;

    PathScan() {
        this(null);
    }

    private PathScan(final CollectionPath collectionPath) {
        this(collectionPath, ScanStatus.STARTED);
    }

    private PathScan(final CollectionPath collectionPath, final ScanStatus status) {
        this(collectionPath, status, null);
    }

    private PathScan(final CollectionPath collectionPath, final ScanStatus status, final PathSummaryRecord summary) {
        this(collectionPath, status, summary, LocalDateTime.now());
    }

    private PathScan(final CollectionPath collectionPath, final ScanStatus status, final PathSummaryRecord summary, final LocalDateTime started) {
        this(collectionPath, status, summary, started, null);
    }

    private PathScan(final CollectionPath collectionPath, final ScanStatus status, final PathSummaryRecord summary, final LocalDateTime started, final LocalDateTime stopped) {
        this(collectionPath, status, summary, started, stopped, new HashSet<>());
    }

    private PathScan(final CollectionPath collectionPath, final ScanStatus status, final PathSummaryRecord summary, final LocalDateTime started, final LocalDateTime stopped, final Set<FileScanRecord> results) {
        this.collectionPath = collectionPath;
        this.status = status;
        this.summary = summary;
        this.started = started;
        this.stopped = stopped;
        this.results = results;
    }

    public static final @NonNull PathScan of(final CollectionPath collectionPath, final PathSummaryRecord summary) {
        if ((collectionPath == null) || (summary == null)) throw new NullPointerException("CollectionPath or PathSummaryRecord is null");
        return new PathScan(collectionPath, ScanStatus.INITIALISED, summary);
    }

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

    public PathSummary getSummary() {
        return summary;
    }
    public PathSummaryRecord getSummaryRecord() {
        return summary;
    }

    public String getDuration() {
        if (started == null) return null;
        if (stopped == null) return Duration.between(started, LocalDateTime.now()).toSeconds() + "s";
        return Duration.between(started, stopped).toSeconds() + "s";
    }
    public LocalDateTime getStarted() {
        return started;
    }
    public LocalDateTime getStopped() {
        return stopped;
    }
    public Set<FileScanRecord> getResults() {
        return results;
    }
    public int getResultCount() {
        return this.results.size();
    }
    public FileScanRecord addResultForDeleted(final FileScanRecord previousResult) {
        if (previousResult == null) throw new NullPointerException("Previous result is null");
        final FileScanRecord record = FileScanRecord.of(this, previousResult);
        this.results.add(record);
        return record;
    }

    public void updateFrom(final PathScanResult result) throws IOException {
        if (result == null) throw new NullPointerException("PathScanResult is null");
        if (!Files.isSameFile(this.collectionPath.getRoot(), result.getPath()))
            throw new IllegalArgumentException("PathScanResult path " + result.getPath() + " does not match CollectionPath root " + this.collectionPath.getRoot());
        this.summary = PathSummaryRecord.of(result.getSummary());
        this.status = ScanStatus.COMPLETED;
        this.stopped = LocalDateTime.now();
        if ((result.getResults() != null) && result.getResults().size() > this.results.size()) {
            this.results.clear();
            this.results.addAll(result.getResults().stream().map(r -> FileScanRecord.of(this, r)).toList());
        }
    }
    public boolean isCompleted() {
        return this.status == ScanStatus.COMPLETED;
    }
    public boolean addResult(final FileScanRecord result) {
        if (result == null) throw new NullPointerException("FileScanRecord result is null");
        return this.results.add(result);
    }
    public boolean addResults(final java.util.Collection<FileScanRecord> results) {
        if (results == null) throw new NullPointerException("FileScanRecord results are null");
        return this.results.addAll(results);
    }
    public boolean isDamaged() {
        return this.results.stream().anyMatch(r -> r.getStatus() == FileScanStatus.DAMAGED);
    }
    public List<FileScanRecord> getDamagedResults() {
        return this.results.stream().filter(r -> r.getStatus() == FileScanStatus.DAMAGED).toList();
    }
    public int getDamagedCount() {
        return this.results.stream().filter(r -> r.getStatus() == FileScanStatus.DAMAGED).toList().size();
    }
    public boolean isDenied() {
        return this.results.stream().anyMatch(r -> r.getStatus() == FileScanStatus.DENIED);
    }
    public List<FileScanRecord> getDeniedResults() {
        return this.results.stream().filter(r -> r.getStatus() == FileScanStatus.DENIED).toList();
    }
    public int getDeniedCount() {
        return this.results.stream().filter(r -> r.getStatus() == FileScanStatus.DENIED).toList().size();
    }
}