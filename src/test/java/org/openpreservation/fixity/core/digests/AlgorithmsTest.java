package org.openpreservation.fixity.core.digests;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.Test;

public class AlgorithmsTest {
    @Test
    public void testFromString() throws NoSuchAlgorithmException {
        Algorithms alg = Algorithms.fromString("sha-1");
        assertSame(Algorithms.SHA_1, alg);
    }
}
