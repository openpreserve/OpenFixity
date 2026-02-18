package org.openpreservation.fixity.core.paths;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.openpreservation.fixity.core.digests.Hasher;

/**
 * Interface for scanning paths.
 */
public interface PathScanner {
    /**
     * 
     * @param path
     * @return
     * @throws FileNotFoundException 
     */
    public Set<Path> listDirectories(final Path path, final boolean recursive) throws FileNotFoundException;
    /**
     * 
     * @param path
     * @return
     * @throws FileNotFoundException 
     */
    public Set<Path> listFiles(final Path path, final boolean recursive) throws FileNotFoundException;
    /**
     * Create a PathSummary for the given path.
     * @param path the path to summarize
     * @return the path summary
     * @throws FileNotFoundException 
     */
    public PathSummary summarise(final Path path, final boolean recursive) throws FileNotFoundException;
    /**
     * Create a ScanResult for the given path.
     * @param path the path to scan
     * @return the scan result
     * @throws FileNotFoundException 
     */
    public PathScanResult scan(final Path path, final Hasher hasher, final boolean recursive) throws FileNotFoundException;

    public abstract class AbstractPathScanner implements PathScanner {

        protected PathSummary toProcess = PathSummary.EMPTY;
        protected long bytesScanned = 0L;
        protected long filesScanned = 0L;
        protected final Set<FileScanResult> results = new HashSet<>();

        @Override
        public Set<Path> listDirectories(final Path path, final boolean recursive) throws FileNotFoundException {
            return listPaths(path, recursive, true);
        }

        @Override
        public Set<Path> listFiles(final Path path, final boolean recursive) throws FileNotFoundException {
            return listPaths(path, recursive, false);
        }

        @Override
        public PathSummary summarise(final Path path, final boolean recursive) throws FileNotFoundException {
            return PathSummary.of(path, recursive);
        }

        @Override
        public PathScanResult scan(final Path path, final Hasher hasher, final boolean recursive) throws FileNotFoundException {
            if ((path == null) || (hasher == null)) throw new NullPointerException("Path and Hasher cannot be null");
            if (!Files.exists(path)) throw new FileNotFoundException("Path does not exist: " + path.toString());
            if (!Files.isDirectory(path)) throw new IllegalArgumentException("Path is not a directory: " + path.toString());
            try {
                this.setupScan(path);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to setup scan", e);
            }
            this.toProcess = summarise(path, recursive);
            this.bytesScanned = 0L;
            this.filesScanned = 0L;
            scanPath(path, hasher, recursive);
            return PathScanResult.of(path, this.filesScanned, this.bytesScanned, this.toProcess, results);
        }

        abstract protected void setupScan(final Path path) throws Exception;
        abstract protected void processResults(final Set<FileScanResult> results) throws Exception;

        private void scanPath(final Path path, final Hasher hasher, final boolean recursive) throws FileNotFoundException {
            if (recursive) {
                for (final Path dirPath : listDirectories(path, false)) {
                    scanPath(dirPath, hasher, true);
                }
            }
            Set<FileScanResult> batchResults = new HashSet<>();
            for (final Path filePath : listFiles(path, false)) {
                final FileScanResult fileScanResult = FileScanResult.FileScanResultImpl.of(filePath, hasher);
                batchResults.add(fileScanResult);
                this.filesScanned++;
                this.bytesScanned += fileScanResult.getLength();
            }
            this.results.addAll(batchResults);
            try {
                this.processResults(batchResults);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to process results", e);
            }
        }

        static private Set<Path> listPaths(final Path path, final boolean recursive, final boolean directories) throws FileNotFoundException {
            if(path == null) throw new NullPointerException("Path cannot be null");
            if (!Files.exists(path)) throw new FileNotFoundException("Path does not exist: " + path.toString());
            if (!Files.isDirectory(path)) throw new IllegalArgumentException("Path is not a directory: " + path.toString());
            if (!Files.isReadable(path)) return new HashSet<>();
            final Set<Path> results = new HashSet<>();
            if (!directories) {
                for (final File file : path.toFile().listFiles(new FilesOnlyFilter())) {
                    results.add(file.toPath());
                }
            }
            for (final File file : path.toFile().listFiles(new DirsOnlyFilter())) {
                if (directories) {
                    results.add(file.toPath());
                }
                if (recursive && file.canRead()) {
                    results.addAll(listPaths(file.toPath(), true, directories));
                }
            }
            return results;
        }

        static final class FilesOnlyFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        }

        static final class DirsOnlyFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        }
    }

    public class PathScannerImpl extends AbstractPathScanner {
        private PathScannerImpl() {
            super();
        }

        @Override
        protected void processResults(final Set<FileScanResult> results) {
            // Do nothing in this simple implementation.
        }

        @Override
        protected void setupScan(Path path) {
            // Do nothing in this simple implementation.
        }
    }

    public static PathScanner instance() {
        return new PathScannerImpl();
    }
}
