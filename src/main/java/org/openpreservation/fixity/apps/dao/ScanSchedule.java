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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.openpreservation.fixity.core.digests.Algorithms;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * A recurring scan: which registered path to scan, how often, and with which digest algorithm.
 * Persisted so schedules survive a restart; on startup they are re-registered with Quartz.
 *
 * <p>Users set a friendly preset ({@link Frequency} plus a time of day); the Quartz cron
 * expression is derived from that. A raw cron override is available for power users.
 */
@Entity
@Table
@NamedQuery(name = "ScanSchedule.findAll", query = "SELECT s FROM ScanSchedule s")
@NamedQuery(name = "ScanSchedule.findEnabled", query = "SELECT s FROM ScanSchedule s WHERE s.enabled = true")
@NamedQuery(name = "ScanSchedule.findByCollectionPath",
        query = "SELECT s FROM ScanSchedule s WHERE s.collectionPath = :collectionPath")
@NullMarked
public class ScanSchedule implements Serializable {
    private static final long serialVersionUID = 6820174639284710265L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    // The path this schedule scans. Not serialized whole (it would drag the scan graph and could
    // cycle); pathId, pathName and pathRoot are exposed below for the frontend instead.
    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_path_id", nullable = false)
    private CollectionPath collectionPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Frequency frequency;

    // Explicit column names: "hour" and "minute" are reserved words in H2 (datetime keywords),
    // so the unquoted defaults would fail CREATE TABLE.
    @Column(name = "minute_of_hour", nullable = false)
    private int minute;

    @Column(name = "hour_of_day", nullable = false)
    private int hour;

    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Algorithms algorithm;

    // A raw Quartz cron, set only when a power user bypasses the preset. When present it wins.
    @Column(nullable = true)
    private @Nullable String cronOverride;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private LocalDateTime created;

    @SuppressWarnings("null")
    ScanSchedule() {
        // For JPA.
    }

    private ScanSchedule(final CollectionPath collectionPath, final Frequency frequency, final int minute,
                         final int hour, final int dayOfWeek, final Algorithms algorithm,
                         final @Nullable String cronOverride, final boolean enabled, final LocalDateTime created) {
        this.collectionPath = collectionPath;
        this.frequency = frequency;
        this.minute = minute;
        this.hour = hour;
        this.dayOfWeek = dayOfWeek;
        this.algorithm = algorithm;
        this.cronOverride = cronOverride;
        this.enabled = enabled;
        this.created = created;
    }

    /** A preset schedule; the cron is derived from the frequency and time. */
    public static ScanSchedule of(final CollectionPath collectionPath, final Frequency frequency,
                                  final int minute, final int hour, final int dayOfWeek,
                                  final Algorithms algorithm) {
        // Validate the preset by building the cron now, so an invalid time is rejected on
        // creation rather than at scheduling time.
        frequency.toCron(minute, hour, dayOfWeek);
        return new ScanSchedule(collectionPath, frequency, minute, hour, dayOfWeek, algorithm, null, true,
                LocalDateTime.now());
    }

    /** A schedule driven by a raw Quartz cron expression (power users). */
    public static ScanSchedule ofCron(final CollectionPath collectionPath, final String cron,
                                      final Algorithms algorithm) {
        return new ScanSchedule(collectionPath, Frequency.DAILY, 0, 0, 1, algorithm, cron, true,
                LocalDateTime.now());
    }

    /** The Quartz cron expression this schedule runs on: the override if set, else the preset. */
    @JsonProperty("cron")
    public String toCron() {
        return cronOverride != null ? cronOverride : frequency.toCron(minute, hour, dayOfWeek);
    }

    @Nullable
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @JsonProperty("pathId")
    public @Nullable Long getPathId() {
        return collectionPath.getId();
    }

    @JsonProperty("pathName")
    public String getPathName() {
        return collectionPath.getName();
    }

    @JsonProperty("pathRoot")
    public String getPathRoot() {
        return collectionPath.getFullPath();
    }

    @JsonIgnore
    public CollectionPath getCollectionPath() {
        return collectionPath;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public int getMinute() {
        return minute;
    }

    public int getHour() {
        return hour;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public Algorithms getAlgorithm() {
        return algorithm;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    /** A stable Quartz job name for this schedule, distinct from one-off scan jobs. */
    @JsonIgnore
    public @NonNull String getJobName() {
        return "schedule-" + (id != null ? id : "new");
    }

    private static final String[] DAY_NAMES =
            { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };

    /** A human sentence for this schedule, used by the server-rendered views. */
    @JsonProperty("description")
    public String getDescription() {
        if (cronOverride != null) {
            return "Custom (" + cronOverride + ")";
        }
        final String time = String.format("%02d:%02d", hour, minute);
        switch (frequency) {
            case HOURLY:
                return String.format("Every hour at %02d minutes past", minute);
            case DAILY:
                return "Every day at " + time;
            case WEEKLY:
                final String day = (dayOfWeek >= 1 && dayOfWeek <= 7) ? DAY_NAMES[dayOfWeek - 1] : "week";
                return "Every " + day + " at " + time;
            default:
                return toCron();
        }
    }
}
