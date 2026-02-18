package org.openpreservation.fixity.core.paths;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public interface PathScanResult {
    public Path getPath();
    public long getFilesScanned();
    public long getBytesScanned();
    public PathSummary getSummary();
    public Set<? extends FileScanResult> getResults();

    static final class PathScanResultImpl implements PathScanResult {
        private final Path path;
        private final long filesScanned;
        private final long bytesScanned;
        private final PathSummary summary;
        private final Set<FileScanResult> results;

        private PathScanResultImpl(final Path path, final long filesScanned, final long bytesScanned, final PathSummary summary, final Set<FileScanResult> results) {
            this.path = path;
            this.filesScanned = filesScanned;
            this.bytesScanned = bytesScanned;
            this.summary = summary;
            this.results = Collections.unmodifiableSet(results);
        }

        @Override
        public Path getPath() {
            return path;
        }

        @Override
        public long getFilesScanned() {
            return filesScanned;
        }

        @Override
        public long getBytesScanned() {
            return bytesScanned;
        }

        public PathSummary getSummary() {
            return summary;
        }

        @Override
        public Set<FileScanResult> getResults() {
            return this.results;
        }

        
        @Override
        public int hashCode() {
            return Objects.hash(path, filesScanned, bytesScanned, summary, results);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof PathScanResultImpl))
                return false;
            PathScanResultImpl other = (PathScanResultImpl) obj;
            return Objects.equals(path, other.path) && filesScanned == other.filesScanned
                    && bytesScanned == other.bytesScanned && Objects.equals(summary, other.summary)
                    && Objects.equals(results, other.results);
        }
    }

    public static PathScanResultImpl of(final Path path, final long filesScanned,
                                        final long bytesScanned, final PathSummary summary,
                                        final Set<FileScanResult> results) {
        return new PathScanResultImpl(path, filesScanned, bytesScanned, summary, results);
    }
}
