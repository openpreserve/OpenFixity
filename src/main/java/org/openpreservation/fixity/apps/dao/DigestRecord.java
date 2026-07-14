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

import java.util.HexFormat;
import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.openpreservation.fixity.core.digests.Algorithms;
import org.openpreservation.fixity.core.digests.DigestResult;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity()
@Table()
@NullMarked
public class DigestRecord implements DigestResult {
    private static final long serialVersionUID = 7263849201847563921L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id = -1L;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private final Algorithms algorithm;
    @Column(nullable = false)
    private final String digest;
    @Column(nullable = false)
    private final long messageLength;
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<FileScanRecord> fileScans;

    private DigestRecord() {
        this(Algorithms.DEFAULT, Algorithms.DEFAULT.getNullHex(), 0L);
    }

    private DigestRecord(final Algorithms algorithm, final String digest, final long messageLength) {
        this(algorithm, digest, messageLength, new HashSet<>());
    }

    private DigestRecord(final Algorithms algorithm, final String digest, final long messageLength, final Set<FileScanRecord> fileContentScan) {
        super();
        this.algorithm = algorithm;
        this.digest = digest;
        this.messageLength = messageLength;
        this.fileScans = fileContentScan;
    }

    @Override
    @NonNull
    public Algorithms getAlgorithm() {
        return this.algorithm;
    }

    @Override
    public String toHexString() {
        return this.digest;
    }

    public String getShortenedDigest() {
        return this.algorithm.getName() + ": " + (this.digest.length() > 8 ? this.digest.substring(0, 8) + "..." : this.digest);
    }

    @Override
    public byte @NonNull[] getDigestBytes() {
        final byte[] bytes = HexFormat.of().parseHex(this.digest);
        return (bytes == null) ? new byte[0] : bytes;
    }

    @Override
    public int getDigestLength() {
        return this.getDigestBytes().length;
    }

    @Override
    public long getMessageLength() {
        return this.messageLength;
    }

    static final DigestRecord of(final DigestResult result) {
        return new DigestRecord(result.getAlgorithm(), result.toHexString(), result.getMessageLength());
    }

    static final DigestRecord of(final Algorithms algorithm, final String digest, final long messageLength) {
        return new DigestRecord(algorithm, digest, messageLength);
    }
}
