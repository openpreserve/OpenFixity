package org.openpreservation.fixity.apps.dao;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity(name = "PathRegistration")
@Table(name = "PathRegistration")
@NamedQuery(name = "PathRegistration.getByPath", query = "SELECT pr FROM PathRegistration pr WHERE pr.collectionPath.id = :pathId AND pr.collection.id = :collectionId AND pr.deregisteredAt IS NULL")
public final class PathRegistration implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private final Collection collection;
    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_path_id")
    private final CollectionPath collectionPath;
    @Column(nullable = false)
    private final LocalDateTime registeredAt;
    private LocalDateTime deregisteredAt;

    private PathRegistration() {
        this(null, null);
    }

    private PathRegistration(final Collection collection, final CollectionPath collectionPath) {
        super();
        this.collection = collection;
        this.collectionPath = collectionPath;
        this.registeredAt = LocalDateTime.now();
        this.deregisteredAt = null;
    }

    public static final PathRegistration of(final Collection collection, final CollectionPath collectionPath) {
        if ((collection == null) || (collectionPath == null)) throw new NullPointerException();
        return new PathRegistration(collection, collectionPath);
    }

    public Long id() {
        return this.id;
    }

    public Collection getCollection() {
        return this.collection;
    }

    public CollectionPath getCollectionPath() {
        return this.collectionPath;
    }

    public LocalDateTime getRegisteredAt() {
        return this.registeredAt;
    }

    public LocalDateTime getDeregisteredAt() {
        return this.deregisteredAt;
    }

    public LocalDateTime deregister() {
        return this.deregisteredAt = LocalDateTime.now();
    }

    public boolean isDeRegistered() {
        return this.deregisteredAt != null;
    }

    public boolean isRegistered() {
        return this.deregisteredAt == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(collection, collectionPath, registeredAt, deregisteredAt);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof PathRegistration))
            return false;
        PathRegistration other = (PathRegistration) obj;
        return Objects.equals(collection, other.collection) && Objects.equals(collectionPath, other.collectionPath)
                && Objects.equals(registeredAt, other.registeredAt)
                && Objects.equals(deregisteredAt, other.deregisteredAt);
    }

    
}
