package org.openpreservation.fixity.apps.dao;

import org.openpreservation.fixity.core.digests.Algorithms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
@Entity()
public class DigestAlgorithm {
    public static DigestAlgorithm of(final Algorithms algorithm) {
        if (algorithm == null) throw new NullPointerException("Algorithm cannot be null.");
        return new DigestAlgorithm(algorithm);
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    final Algorithms algorithm;
    private DigestAlgorithm(final Algorithms algorithm) { this.algorithm = algorithm; }
    public Algorithms getAlgorithm() { return algorithm; }
}
