/**
 *
 */
package org.openpreservation.fixity.core.digests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * @author <a href="mailto:carl@openplanetsfoundation.org">Carl Wilson</a>
 *         <a href="https://github.com/carlwilson">carlwilson AT github</a>
 * @version 0.1
 *
 *          Created 20 Jul 2012:03:29:11
 */

public class DigestResultTest {
    @Test
    public void testDefaultResult() throws NoSuchAlgorithmException, IOException {
        @SuppressWarnings("null")
        final DigestResult defaultResult = DigestResult.NULL_DIGESTS.get(Algorithms.DEFAULT);
        assertSame(DigestResult.DEFAULT_NULL_DIGEST, defaultResult);
    }

    @SuppressWarnings("null")
    @Test
    public void testEmptyResult() throws NoSuchAlgorithmException, IOException {
        assertEquals(Hasher.instance(Collections.singleton(Algorithms.DEFAULT)).hash(new byte[0]),
                     Hasher.instance(Collections.singleton(Algorithms.DEFAULT)).hash("".getBytes(StandardCharsets.UTF_8)));
    }

    @SuppressWarnings("null")
    @Test
    public void testEmptyFileResult() throws NoSuchAlgorithmException, IOException {
        for (Algorithms algorithm : Algorithms.AVAILABLE) {
            assertEquals(Hasher.instance(Collections.singleton(algorithm)).hash(new byte[0]),
                         Hasher.instance(Collections.singleton(algorithm)).hash(ClassLoader.getSystemResourceAsStream("org/openpreservation/fixity/digests/empty")));
        }
    }

    @SuppressWarnings("null")
    @Test
    public void testOfNullAlgorithm() throws NoSuchAlgorithmException, IOException {
        assertThrows(IllegalArgumentException.class, () ->
            DigestResult.of(null,
                            Hasher.instance(Collections.singleton(Algorithms.SHA_1)).hash("".getBytes(StandardCharsets.UTF_8)).iterator().next().getDigestBytes(),
                            0L));
    }

    @Test
    public void testOfBadAlgorithm() {
        assertThrows(NoSuchAlgorithmException.class, () ->
            Algorithms.fromString("BAD-ALGORITHM"));
    }

    @SuppressWarnings("null")
    @Test
    public void testOfNullDigest() {
        assertThrows(NullPointerException.class, () ->
            DigestResult.of(Algorithms.DEFAULT, null, 0L));
    }

    @SuppressWarnings("null")
    @Test
    public void testOfShortDigest() {
        final DigestResult nullDigest = DigestResult.NULL_DIGESTS.get(Algorithms.DEFAULT);
        assertThrows(IllegalArgumentException.class, () ->
            DigestResult.of(Algorithms.DEFAULT, Arrays.copyOf(nullDigest.getDigestBytes(), (int) nullDigest.getDigestLength() - 1), 0L));
    }

    @SuppressWarnings("null")
    @Test
    public void testOfLongDigest() {
        final DigestResult nullDigest = DigestResult.NULL_DIGESTS.get(Algorithms.DEFAULT);
        assertThrows(IllegalArgumentException.class, () ->
            DigestResult.of(Algorithms.DEFAULT, Arrays.copyOf(nullDigest.getDigestBytes(), (int) nullDigest.getDigestLength() + 1), 0L));
    }

    @Test
    public void testOfShortMessage() {
        @SuppressWarnings("null")
        final DigestResult nullDigest = DigestResult.NULL_DIGESTS.get(Algorithms.DEFAULT);
        assertThrows(IllegalArgumentException.class, () ->
            DigestResult.of(Algorithms.DEFAULT, nullDigest.getDigestBytes(), -1L));
    }

    @Test
    public void testEqualsContract() {
        EqualsVerifier.forClass(DigestResult.DigestResultImpl.class).verify();
    }
}
