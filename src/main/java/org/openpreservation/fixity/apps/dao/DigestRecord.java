package org.openpreservation.fixity.apps.dao;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.openpreservation.fixity.core.digests.Algorithms;
import org.openpreservation.fixity.core.digests.DigestResult;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity()
@Table()
public class DigestRecord implements DigestResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false)
    private final Algorithms algorithm;
    @Column(nullable = false)
    private final String digest;
    @Column(nullable = false)
    private final long messageLength;
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private final Set<FileScanRecord> fileScans;

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
    public Algorithms getAlgorithm() {
        return this.algorithm;
    }

    @Override
    public String toHexString() {
        return this.digest;
    }

    public String getShortenedDigest() {
        return this.algorithm + ": " + (this.digest.length() > 8 ? this.digest.substring(0, 8) + "..." : this.digest);
    }

    @Override
    public byte[] getDigestBytes() {
        return this.digest.getBytes(StandardCharsets.UTF_8);
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
        if (result == null) throw new NullPointerException("DigestResult is null");
        return new DigestRecord(result.getAlgorithm(), result.toHexString(), result.getMessageLength());
    }

    static final DigestRecord of(final Algorithms algorithm, final String digest, final long messageLength) {
        return new DigestRecord(algorithm, digest, messageLength);
    }
}
