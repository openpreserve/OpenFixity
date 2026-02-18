package org.openpreservation.fixity.core.paths;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Objects;

public interface PathSummary {
    /**
     * Get the root path for this summary.
     * @return Path The root path.
     */
    public Path getPath();
    /**
     * Get the total number of bytes for all files below this path.
     * @return long The total number of bytes.
     */
    public long getTotalBytes();
    /**
     * Get the total number of files below this path.
     * @return long The total number of files.
     */
    public long getTotalFiles();
    /**
     * Get the total number of directories below this path that are unreadable due to file system permissions.
     * @return long The total number of unreadable directories.
     */
    public long getTotalUnreadableDirectories();
    /**
     * Get the total number of files below this path that are unreadable due to file system permissions.
     * @return long The total number of unreadable files.
     */
    public long getTotalUnreadableFiles();

    static final class PathSummaryImpl implements PathSummary {

        private final Path path;
        private final long totalFiles;
        private final long totalBytes;
        private final long totalUnreadableDirectories;
        private final long totalUnreadableFiles;

        private PathSummaryImpl() {
            this.path = null;
            this.totalFiles = 0L;
            this.totalBytes = 0L;
            this.totalUnreadableDirectories = 0L;
            this.totalUnreadableFiles = 0L;
        }

        private PathSummaryImpl(final Path path, final long totalFiles, final long totalBytes, final long totalUnreadableDirectories, final long totalUnreadableFiles) {
            this.path = path;
            this.totalFiles = totalFiles;
            this.totalBytes = totalBytes;
            this.totalUnreadableDirectories = totalUnreadableDirectories;
            this.totalUnreadableFiles = totalUnreadableFiles;
        }

        @Override
        public final Path getPath() {
            return this.path;
        }

        @Override
        public final long getTotalFiles() {
            return this.totalFiles;
        }

        @Override
        public final long getTotalBytes() {
            return this.totalBytes;
        }

        @Override
        public final long getTotalUnreadableDirectories() {
            return this.totalUnreadableDirectories;
        }

        @Override
        public final long getTotalUnreadableFiles() {
            return this.totalUnreadableFiles;
        }

        @Override
        public final int hashCode() {
            return Objects.hash(path, totalFiles, totalBytes, totalUnreadableDirectories, totalUnreadableFiles);
        }

        @Override
        public final boolean equals(final Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof PathSummaryImpl))
                return false;
            final PathSummaryImpl other = (PathSummaryImpl) obj;
            return Objects.equals(path, other.path) && totalFiles == other.totalFiles && totalBytes == other.totalBytes
                    && totalUnreadableDirectories == other.totalUnreadableDirectories && totalUnreadableFiles == other.totalUnreadableFiles;
        }

        @Override
        public final String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("PathSummaryImpl [");
            if (path != null)
                builder.append("path=").append(path).append(", ");
            builder.append("totalFiles=").append(totalFiles).append(", totalBytes=").append(totalBytes).append(", totalUnreadableDirectories=").append(totalUnreadableDirectories).append(", totalUnreadableFiles=").append(totalUnreadableFiles).append("]");
            return builder.toString();
        }

        static final PathSummary of(final Path path, final boolean recursive) throws FileNotFoundException {
            if (path == null) throw new NullPointerException("Path cannot be null");
            if (!path.toFile().exists()) throw new FileNotFoundException("Path does not exist: " + path.toString());
            if (!path.toFile().isDirectory()) return new PathSummaryImpl(path, 1L, path.toFile().length(), 0L, path.toFile().canRead() ? 0L : 1L);
            final long[] counts = countFiles(path.toFile(), recursive);
            return new PathSummaryImpl(path, counts[0], counts[1], counts[2], counts[3]);
        }

        private static final long[] countFiles(final File directory, final boolean recursive) {
            long totalBytes = 0L;
            long totalFiles = 0L;
            long totalUnreadableFiles = 0L;
            long totalUnreadableDirectories = 0L;
            File[] files = directory.listFiles();
            if (files == null) {
                return new long[] { 0L, 0L, 1L, 0L };
            }
            for (final File file : files) {
                if (file.isFile()) {
                    if (!file.canRead()) {
                        totalUnreadableFiles++;
                    }
                    totalFiles++;
                    totalBytes += file.length();
                } else if (file.isDirectory()) {
                    if (!file.canRead()) {
                        totalUnreadableDirectories++;
                    } else if (recursive) {
                        final long[] subCounts = countFiles(file, true);
                        totalFiles += subCounts[0];
                        totalBytes += subCounts[1];
                        totalUnreadableDirectories += subCounts[2];
                        totalUnreadableFiles += subCounts[3];
                    }
                }
            }
            return new long[] { totalFiles, totalBytes, totalUnreadableDirectories, totalUnreadableFiles };
        }
    }
    static final PathSummary EMPTY = new PathSummaryImpl();

    public static PathSummary of(final Path path, final boolean recursive) throws FileNotFoundException {
        return PathSummaryImpl.of(path, recursive);
    }

    public static PathSummary of(final Path path, final long totalFiles, final long totalBytes, final long totalUnreadableDirectories, final long totalUnreadableFiles) {
        return new PathSummaryImpl(path, totalFiles, totalBytes, totalUnreadableDirectories, totalUnreadableFiles);
    }
}
