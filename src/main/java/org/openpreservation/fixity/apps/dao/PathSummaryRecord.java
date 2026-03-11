package org.openpreservation.fixity.apps.dao;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.nio.file.Path;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.openpreservation.fixity.core.paths.PathSummary;

@Entity(name = "PathSummaryRecord")
@Table(name = "PathSummaryRecord")
@NullMarked
public class PathSummaryRecord implements PathSummary, Serializable {
    private static final long serialVersionUID = 5483912760456721987L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;
    @Column(nullable = false)
    private final long totalFiles;
    @Column(nullable = false)
    private final long totalBytes;
    @Column(nullable = false)
    private final long unreadableDirectories;
    @Column(nullable = false)
    private final long totalUnreadableFiles;
    @Column(columnDefinition = "BIGINT DEFAULT 0 NOT NULL")
    private long damagedCount;
    @Column(columnDefinition = "BIGINT DEFAULT 0 NOT NULL")
    private long deniedCount;
    @OneToOne(mappedBy = "summary")
    private @Nullable PathScan pathScan;

    private PathSummaryRecord() {
        this(0L, 0L, 0L, 0L, 0L, 0L);
    }

    private PathSummaryRecord(final long totalFiles, final long totalBytes, final long unreadableDirectories, final long totalUnreadableFiles, final long damagedCount, final long deniedCount) {
        this.totalFiles = totalFiles;
        this.totalBytes = totalBytes;
        this.unreadableDirectories = unreadableDirectories;
        this.totalUnreadableFiles = totalUnreadableFiles;
        this.damagedCount = damagedCount;
        this.deniedCount = deniedCount;
    }

    static final PathSummaryRecord of(final long totalFiles, final long totalBytes, final long unreadableDirectories, final long totalUnreadableFiles) {
        return new PathSummaryRecord(totalFiles, totalBytes, unreadableDirectories, totalUnreadableFiles, 0L, 0L);
    }

    static final PathSummaryRecord empty() {
        return new PathSummaryRecord();
    }

    @Override
    @NonNull
    public Path getPath() {
        if (this.pathScan == null) {
            throw new IllegalStateException("PathSummaryRecord must be associated with a PathScan to retrieve path information.");
        }
        return this.pathScan.getCollectionPath().getRoot();
    }

    @Override
    public long getTotalFiles() {
        return this.totalFiles;
    }

    @Override
    public long getTotalBytes() {
        return this.totalBytes;
    }

    public String getFormattedTotalBytes() {
        return humanReadableByteCountBin(this.totalBytes);
    }

    @Override
    public long getTotalUnreadableDirectories() {
        return this.unreadableDirectories;
    }

    @Override
    public long getTotalUnreadableFiles() {
        return this.totalUnreadableFiles;
    }

    public static final PathSummaryRecord of(final PathSummary summary) {
        return new PathSummaryRecord(summary.getTotalFiles(), summary.getTotalBytes(), summary.getTotalUnreadableDirectories(), summary.getTotalUnreadableFiles(), 0L, 0L);
    }

    public static final PathSummaryRecord of(final PathSummary summary, final long damagedCount, final long deniedCount) {
        return new PathSummaryRecord(summary.getTotalFiles(), summary.getTotalBytes(), summary.getTotalUnreadableDirectories(), summary.getTotalUnreadableFiles(), damagedCount, deniedCount);
    }

    public static final PathSummaryRecord of(final Path path) throws FileNotFoundException {
        return PathSummaryRecord.of(PathSummary.of(path, true));
    }

    public long getDamagedCount() {
        return damagedCount;
    }

    public long getDeniedCount() {
        return deniedCount;
    }

    @SuppressWarnings("null")
    public static String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }
}
