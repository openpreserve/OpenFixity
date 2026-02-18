package org.openpreservation.fixity.apps.dao;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;

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
import jakarta.persistence.Table;

@Entity()
@Table(indexes = {
        @jakarta.persistence.Index(name = "idx_collection_name", columnList = "name")
    })
@NamedQuery(name = "Collection.getByName",query = "SELECT c FROM Collection c WHERE c.name = :name")
@NamedQuery(name = "Collection.findAll",query = "SELECT c FROM Collection c")
public final class Collection implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private final String name;
    @Column(nullable = false)
    private final LocalDateTime created;
    @JsonManagedReference
    @OneToMany(mappedBy = "collection", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<@NonNull PathRegistration> pathRegistrations;

    private Collection() {
        this(null);
    }

    private Collection(final String name) {
        this(name, LocalDateTime.now(), new HashSet<>());
    }

    private Collection(final String name, final LocalDateTime created, final Set<@NonNull PathRegistration> pathRegistrations) {
        this.name = name;
        this.created = created;
        this.pathRegistrations = new HashSet<>(pathRegistrations);
    }

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public LocalDateTime getCreated() {
        return this.created;
    }

    public int getPathRegistrationsSize() {
        return this.pathRegistrations.size();
    }

    public Set<@NonNull PathRegistration> getPathRegistrations() {
        return Collections.unmodifiableSet(this.pathRegistrations);
    }

    public Set<@NonNull PathRegistration> getRegisteredPaths() {
        return Collections.unmodifiableSet(this.pathRegistrations.stream().filter(PathRegistration::isRegistered).collect(Collectors.toSet()));
    }

    public Set<@NonNull PathRegistration> getDeRegisteredPaths() {
        return Collections.unmodifiableSet(this.pathRegistrations.stream().filter(PathRegistration::isDeRegistered).collect(Collectors.toSet()));
    }

    public Collection register(PathRegistration pathRegistration) {
        if (pathRegistration == null) throw new NullPointerException();
        this.pathRegistrations.add(pathRegistration);
        return this;
    }

    public String getJobId() {
        return "collection." + this.name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, created);
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Collection))
            return false;
        Collection other = (Collection) obj;
        return Objects.equals(name, other.name) && Objects.equals(created, other.created);
    }
    
    static final Collection of(final String name) {
        if (name == null) throw new NullPointerException();
        if (name.isBlank()) throw new IllegalArgumentException("Name cannot be blank");
        return new Collection(name);
    }
}
