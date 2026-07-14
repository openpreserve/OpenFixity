/*
 * OpenFixity is an application for monitoring and reporting on the fixity of files.
 * Copyright (C) 2026 Open Preservation Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openpreservation.fixity.apps.dao;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
@NamedQuery(name = "PathRegistration.findAll", query = "SELECT pr FROM PathRegistration pr")
@NullMarked
public final class PathRegistration implements Serializable {
    private static final long serialVersionUID = 921387465012345678L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;
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
    private @Nullable LocalDateTime deregisteredAt;

    @SuppressWarnings("null")
    private PathRegistration() {
        // For JPA - fields populated by reflection
        this(null, null);
    }

    private PathRegistration(final Collection collection, final CollectionPath collectionPath) {
        super();
        this.collection = collection;
        this.collectionPath = collectionPath;
        this.registeredAt = now();
        this.deregisteredAt = null;
    }

    public static final PathRegistration of(final Collection collection, final CollectionPath collectionPath) {
        return new PathRegistration(collection, collectionPath);
    }

    @Nullable
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

    @Nullable
    public LocalDateTime getDeregisteredAt() {
        return this.deregisteredAt;
    }

    public LocalDateTime deregister() {
        return this.deregisteredAt = now();
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
    public boolean equals(@Nullable Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof PathRegistration))
            return false;
        PathRegistration other = (PathRegistration) obj;
        return Objects.equals(collection, other.collection) && Objects.equals(collectionPath, other.collectionPath)
                && Objects.equals(registeredAt, other.registeredAt)
                && Objects.equals(deregisteredAt, other.deregisteredAt);
    }

    @SuppressWarnings("null")
    private static final LocalDateTime now() {
        return LocalDateTime.now();
    }
    
}
