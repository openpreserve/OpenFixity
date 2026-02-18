package org.openpreservation.fixity.apps.server.resources.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

import org.openpreservation.fixity.apps.server.resources.OS;
import org.openpreservation.fixity.core.paths.PathScanner;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PathParam;

@jakarta.ws.rs.Path("/api/folders")
public class FoldersResource {
    public static final class FolderInfo {
        private static boolean isHidden(final Path path) {
            try {
                return Files.isHidden(path);
            } catch (final IOException e) {
                return false;
            }
        }
        public final int id;
        public final boolean hasParent;
        public final int parentId;
        public final String name;
        public final boolean isReadable;
        public final boolean isHidden;
        

        FolderInfo(final Path path) {
            this.id = hashPath(path);
            this.hasParent = path.getParent() != null;
            this.parentId = (this.hasParent) ? hashPath(path.getParent()) : 0;
            this.name = (path.getFileName() != null) ? path.getFileName().toString() : path.toString();
            this.isReadable = Files.isReadable(path);
            this.isHidden = isHidden(path);
        }


        @Override
        public int hashCode() {
            return Objects.hash(id, hasParent, parentId, name, isReadable, isHidden);
        }


        @Override
        public boolean equals(final Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof FolderInfo))
                return false;
            final FolderInfo other = (FolderInfo) obj;
            return id == other.id && hasParent == other.hasParent && parentId == other.parentId
                    && Objects.equals(name, other.name) && isReadable == other.isReadable && isHidden == other.isHidden;
        }
    }
    private static final int MAX_ENTRIES = 1000; // Limit the number of entries returned to prevent overwhelming the client

    private static final Map<Integer, Path> cachedFolders = new LinkedHashMap<>(MAX_ENTRIES + 100, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<Integer, Path> eldest) {
            return size() > MAX_ENTRIES;
        }
    };
    private static final Map<Integer, Path> roots = Collections.unmodifiableMap(roots());
    static final Integer defaultRoot = deriveRoot();
    static final Path defaultHome = deriveHome();


    static Map<Integer, Path> roots() {
        if (!(roots == null) && (!roots.isEmpty())) return Collections.unmodifiableMap(roots);
        final Map<Integer, Path> rootsMap = new HashMap<>();
        for (final File root : File.listRoots()) {
            final Path rootPath = root.toPath().toAbsolutePath();
            rootsMap.put(hashPath(rootPath), rootPath);
        }
        return Collections.unmodifiableMap(rootsMap);
    }

    static Integer deriveRoot() {
        if (roots.isEmpty()) {
            throw new IllegalStateException("No filesystem roots found.");
        } else if (roots.size() == 1) {
            return roots.keySet().iterator().next();
        }
        if (OS.isWindows()) {
            return getWinSystemDrive().id;
        }
        return roots.keySet().iterator().next();
    }

    private static final Path deriveHome() {
        String homeEnv = System.getProperty("user.home");
        if (homeEnv != null) {
            Path homePath = Paths.get(homeEnv);
            if (Files.exists(homePath) && Files.isDirectory(homePath)) {
                return homePath.toAbsolutePath();
            }
        }
        return roots.values().iterator().next();
    }

    private static final FolderInfo getWinSystemDrive() {
        final String systemDriveEnv = System.getenv("SystemDrive");
        if (systemDriveEnv != null && !systemDriveEnv.isEmpty()) {
            final Path systemDrive = Paths.get(System.getenv("SystemDrive") + "\\").toAbsolutePath();
            if (roots.containsKey(hashPath(systemDrive))) {
                return new FolderInfo(systemDrive);
            }
        }
        return new FolderInfo(roots.values().iterator().next());
    }
    private static final int hashPath(final Path path) {
        return path.toAbsolutePath().hashCode();
    }

    @GET
    @jakarta.ws.rs.Path("/roots/")
    public Set<FolderInfo> getRoots() {
        return roots.values().stream().map(FolderInfo::new).collect(java.util.stream.Collectors.toSet());
    }

    @GET
    public Set<FolderInfo> getFolderRoots() {
        return getRoots();
    }

    @GET
    @jakarta.ws.rs.Path("/home/")
    public FolderInfo getHomeFolder() {
        FolderInfo homeInfo = new FolderInfo(defaultHome);
        cachedFolders.put(homeInfo.id, defaultHome);
        return homeInfo;
    }

    @GET
    @jakarta.ws.rs.Path("/{folderId}/")
    public FolderInfo getFolder(@PathParam("folderId") final int folderId) {
        Path path = cachedFolders.get(folderId);
        path = (path == null) ? roots.get(folderId) : path;
        if (path == null) {
            throw new NotFoundException("Path with ID " + folderId + " not found.");
        }
        return new FolderInfo(path);
    }

    @GET
    @jakarta.ws.rs.Path("/{folderId}/children")
    public Set<FolderInfo> getChildFolders(@PathParam("folderId") final int folderId) {
        final Path path = getPathById(folderId);
        final Set<FolderInfo> children = new HashSet<>();
        try {
            for (final Path child : PathScanner.instance().listDirectories(path, false)) {
                final int childId = hashPath(child);
                cachedFolders.put(childId, child.toAbsolutePath());
                children.add(new FolderInfo(child));

            }
        } catch (final FileNotFoundException e) {
            throw new NotFoundException("Path with ID " + folderId + " not found on filesystem.");
        }
        return children;
    }

    @GET
    @jakarta.ws.rs.Path("/{folderId}/parents")
    public Stack<FolderInfo> getParentFolders(@PathParam("folderId") final int folderId) {
        final Stack<FolderInfo> parents = new Stack<>();
        Path child = getPathById(folderId);
        while (child != null) {
            child = child.getParent();
            if (child != null) {
                cachedFolders.putIfAbsent(child.hashCode(), child);
                parents.add(new FolderInfo(child));
            }
        }
        return parents;
    }

    @GET
    @jakarta.ws.rs.Path("/roots/default/")
    public FolderInfo getDefaultRoot() {
        return new FolderInfo(getPathById(defaultRoot));
    }

    static Path getPathById(final int folderId) {
        Path path = cachedFolders.get(folderId);
        if (path == null) {
            path = roots.get(folderId);
        }
        if (path == null) {
            throw new NotFoundException("Path with ID " + folderId + " not found.");
        }
        return path;
    }
}
