package org.openpreservation.fixity.core.paths;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.openpreservation.fixity.core.digests.Hasher;

/**
 * Interface for scanning paths.
 */
@NullMarked
public interface PathScanner {
    /**
     * 
     * @param path
     * @return
     * @throws FileNotFoundException 
     * @throws AccessDeniedException 
     */
    public Set<@NonNull Path> listDirectories(final Path path, final boolean recursive) throws FileNotFoundException, AccessDeniedException;
    /**
     * 
     * @param path
     * @return
     * @throws FileNotFoundException 
     * @throws AccessDeniedException 
     */
    public Set<@NonNull Path> listFiles(final Path path, final boolean recursive) throws FileNotFoundException, AccessDeniedException;
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

        @SuppressWarnings("null")
        protected PathScanResult.Builder builder;
        @SuppressWarnings("null")
        protected Path root;

        @Override
        public Set<@NonNull Path> listDirectories(final Path path, final boolean recursive) throws FileNotFoundException, AccessDeniedException {
            return listPaths(path, recursive, true);
        }

        @Override
        public Set<@NonNull Path> listFiles(final Path path, final boolean recursive) throws FileNotFoundException, AccessDeniedException {
            return listPaths(path, recursive, false);
        }

        @Override
        public PathSummary summarise(final Path path, final boolean recursive) throws FileNotFoundException {
            return PathSummary.of(path, recursive);
        }

        @Override
        public PathScanResult scan(final Path path, final Hasher hasher, final boolean recursive) throws FileNotFoundException {
            if (!Files.exists(path)) throw new FileNotFoundException("Path does not exist: " + path.toString());
            if (!Files.isDirectory(path)) throw new IllegalArgumentException("Path is not a directory: " + path.toString());
            if (!Files.isReadable(path)) return PathScanResult.builder(path,
                                                                       PathSummary.of(path, 0L, 0L, 1L, 0L),
                                                                       Folder.unreadable(null, path))
                                                                    .build();
            try {
                this.setupScan(path);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to setup scan", e);
            }
            this.root = Folder.absolutePath(path);
            @NonNull Folder rootFolder = Folder.readable(null, path);
            this.builder = PathScanResult.builder(path, summarise(path, recursive), rootFolder);
            scanPath(rootFolder, hasher, recursive);
            return this.builder.build();
        }

        abstract protected void setupScan(final Path path) throws Exception;
        abstract protected void processResults(final Set<@NonNull FileScanResult> results) throws Exception;

        private void scanPath(final Folder toScan, final Hasher hasher, final boolean recursive) throws FileNotFoundException {
            try {
                this.scanFiles(toScan, hasher);
                this.processResults(toScan.getFileScanResults());
            } catch (AccessDeniedException e ) {
                builder.addFolder(Folder.unreadable(toScan, toScan.getRelativePath()));
                return;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to process results", e);
            }
            builder.addFolder(toScan);
            if (!recursive) return;
            if (toScan.isReadable()) {
                try {
                    for (final Path dirPath : listDirectories(Folder.resolve(root, toScan.getRelativePath()), false)) {
                        if (!Files.isReadable(dirPath)) {
                            builder.addFolder(Folder.unreadable(toScan, Folder.resolve(toScan.getRelativePath(), Folder.getFileName(dirPath))));
                            continue;
                        }
                        scanPath(Folder.readable(toScan, dirPath), hasher, true);
                    }
                } catch (AccessDeniedException e) {
                    // This really shouldn't happen since we check readability in scanFiles, but if it does,
                    // we just skip the subdirectory and continue with the next one.;
                }
            }
        }

        private void scanFiles(final Folder toScan, final Hasher hasher) throws FileNotFoundException, AccessDeniedException {
            for (final Path filePath : listFiles(Folder.resolve(root, toScan.getRelativePath()), false)) {
                final FileScanResult fileScanResult = FileScanResult.FileScanResultImpl.of(filePath, hasher);
                if (fileScanResult != null) {
                    toScan.addFileScanResult(fileScanResult);
                    this.builder.addBytesScanned(fileScanResult.getLength());
                }
                this.builder.addFilesScanned(1L);
            }
        }

        @SuppressWarnings("null")
        static Set<@NonNull Path> listPaths(final Path path, final boolean recursive, final boolean directories) throws FileNotFoundException, AccessDeniedException {
            if (!Files.exists(path)) throw new FileNotFoundException("Path does not exist: " + path.toString());
            if (!Files.isDirectory(path)) throw new IllegalArgumentException("Path is not a directory: " + path.toString());
            if (!Files.isReadable(path)) throw new AccessDeniedException("Path is not readable: " + path.toString());
            final Set<@NonNull Path> results = new HashSet<>();
            if (!directories) {
                final File[] files = path.toFile().listFiles(new FilesOnlyFilter());
                if (files != null) {
                    for (final File file : files) {
                        results.add(file.toPath());
                    }
                }
            }
            final File[] dirs = path.toFile().listFiles(new DirsOnlyFilter());
            if (dirs == null) return results;
            for (final File file : dirs) {
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
            @SuppressWarnings("null")
            @Override
            public boolean accept(@Nullable File pathname) {
                return pathname.isFile();
            }
        }

        static final class DirsOnlyFilter implements FileFilter {
            @SuppressWarnings("null")
            @Override
            public boolean accept(@Nullable File pathname) {
                return pathname.isDirectory();
            }
        }
    }

    public class PathScannerImpl extends AbstractPathScanner {
        private PathScannerImpl() {
            super();
        }

        @Override
        protected void processResults(final Set<@NonNull FileScanResult> results) {
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
