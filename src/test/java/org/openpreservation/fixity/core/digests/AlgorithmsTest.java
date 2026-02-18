package org.openpreservation.fixity.core.digests;

import java.security.NoSuchAlgorithmException;

import org.junit.Test;

public class AlgorithmsTest {
    @Test
    public void testFromString() throws NoSuchAlgorithmException {
        Algorithms alg = Algorithms.fromString("sha-1");
        assert(alg == Algorithms.SHA_1);
    }
}
