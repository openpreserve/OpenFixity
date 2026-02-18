package org.openpreservation.fixity.apps.dao;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.openpreservation.fixity.core.paths.PathSummary;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;

@Entity()
@NamedQuery(name = "CollectionPath.getByRoot",query = "SELECT cp FROM CollectionPath cp WHERE cp.root = :root")
@NamedQuery(name = "CollectionPath.findAll",query = "SELECT cp FROM CollectionPath cp")
public final class CollectionPath implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 65535)
    private final String root;
    private final LocalDate added = LocalDate.now();
    @JsonManagedReference
    @OneToMany(mappedBy = "collectionPath", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<@NonNull PathRegistration> pathRegistrations;
    @JsonManagedReference
    @OneToMany(mappedBy = "collectionPath", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<@NonNull PathScan> pathScans;

    private CollectionPath() {
        this(null); // For JPA
    }

    private CollectionPath(final Path root) {
        this(root, new HashSet<>());
    }

    private CollectionPath(final Path root, final Set<@NonNull PathRegistration> pathRegistrations) {
        this(root, pathRegistrations, new HashSet<>());
    }

    private CollectionPath(final Path root, final Set<@NonNull PathRegistration> pathRegistrations, final Set<@NonNull PathScan> pathScans) {
        super();
        this.root = (root == null) ? null : root.toAbsolutePath().normalize().toString();
        this.pathRegistrations = new HashSet<>(pathRegistrations);
        this.pathScans = new HashSet<>(pathScans);
    }

    public CollectionPath register(PathRegistration pathRegistration) {
        if (pathRegistration == null) throw new NullPointerException();
        this.pathRegistrations.add(pathRegistration);
        return this;
    }

    public PathScan createPathScan(final PathSummary pathSummary) {
        final @NonNull PathScan scan = PathScan.of(this, PathSummaryRecord.of(pathSummary));
        this.pathScans.add(scan);
        return scan;
    }

    public Long getId() {
        return this.id;
    }

    public Path getRoot() {
        return Path.of(root);
    }

    public LocalDate getAdded() {
        return this.added;
    }

    public String getName() {
        return Path.of(root).getFileName().toString();
    }

    public String getFullPath() {
        return root;
    }

    public String getJobId() {
        return "path." + this.getName() +"." + this.getId();
    }

    public Set<@NonNull PathRegistration> getPathRegistrations() {
        return Collections.unmodifiableSet(pathRegistrations);
    }

    public Set<@NonNull PathRegistration> getRegisteredPaths() {
        return pathRegistrations.stream().filter(PathRegistration::isRegistered).collect(Collectors.toSet());
    }

    public Set<@NonNull PathRegistration> getDeRegisteredPaths() {
        return pathRegistrations.stream().filter(PathRegistration::isDeRegistered).collect(Collectors.toSet());
    }

    public Set<@NonNull PathScan> getPathScans() {
        return new HashSet<>(pathScans);
    }

    public Optional<@NonNull PathScan> getLatestScan() {
        return this.pathScans.stream().max((s1, s2) -> s1.getStarted().compareTo(s2.getStarted()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(root, added);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof CollectionPath))
            return false;
        CollectionPath other = (CollectionPath) obj;
        return Objects.equals(root, other.root) && Objects.equals(added, other.added);
    }

    public static CollectionPath of(final Path root) {
        if (root == null) throw new NullPointerException();
        if (!Files.isDirectory(root) || !Files.isReadable(root)) throw new IllegalArgumentException(String.format("Root '%s' is not a readable directory", root));
        return new CollectionPath(root);
    }
}
