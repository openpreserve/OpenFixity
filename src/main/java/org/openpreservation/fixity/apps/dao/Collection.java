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
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
@NullMarked
public final class Collection implements Serializable {
    private static final long serialVersionUID = 7834951203674829153L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;
    @Column(nullable = false, unique = true)
    private final String name;
    @Column(nullable = false)
    private final LocalDateTime created;
    @JsonManagedReference
    @OneToMany(mappedBy = "collection", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Column(nullable = false)
    private Set<@NonNull PathRegistration> pathRegistrations;

    private Collection() {
        this(""); // For JPA
    }

    @SuppressWarnings("null")
    private Collection(final String name) {
        this(name, LocalDateTime.now(), new HashSet<>());
    }

    private Collection(final String name, final LocalDateTime created, final Set<@NonNull PathRegistration> pathRegistrations) {
        this.name = name;
        this.created = created;
        this.pathRegistrations = new HashSet<>(pathRegistrations);
    }

    @Nullable
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

    @SuppressWarnings("null")
    public Set<@NonNull PathRegistration> getPathRegistrations() {
        return Collections.unmodifiableSet(this.pathRegistrations);
    }

    @SuppressWarnings("null")
    public Set<@NonNull PathRegistration> getRegisteredPaths() {
        return Collections.unmodifiableSet(this.pathRegistrations.stream().filter(PathRegistration::isRegistered).collect(Collectors.toSet()));
    }

    @SuppressWarnings("null")
    public Set<@NonNull PathRegistration> getDeRegisteredPaths() {
        return Collections.unmodifiableSet(this.pathRegistrations.stream().filter(PathRegistration::isDeRegistered).collect(Collectors.toSet()));
    }

    public Collection register(final PathRegistration pathRegistration) {
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
    public boolean equals(@Nullable final Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Collection))
            return false;
        final Collection other = (Collection) obj;
        return Objects.equals(name, other.name) && Objects.equals(created, other.created);
    }
    
    static final Collection of(final String name) {
        if (name.isBlank()) throw new IllegalArgumentException("Name cannot be blank");
        return new Collection(name);
    }
}
