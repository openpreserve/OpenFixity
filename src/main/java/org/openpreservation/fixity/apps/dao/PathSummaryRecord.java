package org.openpreservation.fixity.apps.dao;

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

import org.openpreservation.fixity.core.paths.PathSummary;

@Entity(name = "PathSummaryRecord")
@Table(name = "PathSummaryRecord")
public class PathSummaryRecord implements PathSummary, Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private final long totalFiles;
    @Column(nullable = false)
    private final long totalBytes;
    @Column(nullable = false)
    private final long unreadableDirectories;
    @Column(nullable = false)
    private final long totalUnreadableFiles;
    @OneToOne(mappedBy = "summary")
    private PathScan pathScan;

    private PathSummaryRecord() {
        this(0L, 0L, 0L, 0L);
    }

    private PathSummaryRecord(final long totalFiles, final long totalBytes, final long unreadableDirectories, final long totalUnreadableFiles) {
        this.totalFiles = totalFiles;
        this.totalBytes = totalBytes;
        this.unreadableDirectories = unreadableDirectories;
        this.totalUnreadableFiles = totalUnreadableFiles;
    }

    static final PathSummaryRecord of(final long totalFiles, final long totalBytes, final long unreadableDirectories, final long totalUnreadableFiles) {
        return new PathSummaryRecord(totalFiles, totalBytes, unreadableDirectories, totalUnreadableFiles);
    }

    static final PathSummaryRecord empty() {
        return new PathSummaryRecord();
    }

    @Override
    public Path getPath() {
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
        if (summary == null) throw new NullPointerException("PathSummary is null");
        return new PathSummaryRecord(summary.getTotalFiles(), summary.getTotalBytes(), summary.getTotalUnreadableDirectories(), summary.getTotalUnreadableFiles());
    }
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
