/**
 * 
 */
package org.openpreservation.fixity.core.digests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * @author <a href="mailto:carl@openplanetsfoundation.org">Carl Wilson</a>
 *         <a href="https://github.com/carlwilson">carlwilson AT github</a>
 * @version 0.1
 * 
 *          Created 20 Jul 2012:03:29:11
 */

public class DigestResultTest {
    /**
     * Test method for
     * {@link org.openpreservation.fixity.core.digests.DigestResultImpl#DEFAULT}.
     * @throws IOException 
     * @throws NoSuchAlgorithmException 
     */
    @Test
    public void testDefaultResult() throws NoSuchAlgorithmException, IOException {
        // Get the null streamId
        final DigestResult defaultResult = DigestResult.NULL_DIGESTS.get(Algorithms.DEFAULT);
        // I static so should be same object the static instance
        assertSame(DigestResult.DEFAULT_NULL_DIGEST, defaultResult);
    }

    /**
     * Test method for
     * {@link org.openpreservation.fixity.core.digests.DigestResultImpl#DEFAULT}.
     * @throws IOException 
     * @throws NoSuchAlgorithmException 
     */
    @Test
    public void testEmptyResult() throws NoSuchAlgorithmException, IOException {
        assertEquals(Hasher.instance(Algorithms.DEFAULT).hash(new byte[0]),
                     Hasher.instance(Algorithms.DEFAULT).hash("".getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Test method for
     * {@link org.openpreservation.fixity.core.digests.DigestResultImpl#DEFAULT}.
     * @throws IOException 
     * @throws NoSuchAlgorithmException 
     */
    @Test
    public void testEmptyFileResult() throws NoSuchAlgorithmException, IOException {
        for (Algorithms algorithm : Algorithms.AVAILABLE) {
            assertEquals(Hasher.instance(algorithm).hash(new byte[0]),
                         Hasher.instance(algorithm).hash(ClassLoader.getSystemResourceAsStream("org/openpreservation/fixity/digests/empty")));
        }
    }

    /**
     * Test method for
     * {@link org.openpreservation.fixity.core.digests.DigestResultImpl#of(long, String, String)}.
     * @throws IOException 
     * @throws NoSuchAlgorithmException 
     */
    @Test(expected = IllegalArgumentException.class)
    public void testOfNullAlgorithm() throws NoSuchAlgorithmException, IOException {
        // Try creating a byte stream with a length less than zero, use the valid empty
        // hash values
        DigestResult.of(null, Hasher.instance(Algorithms.SHA_1).hash("".getBytes(StandardCharsets.UTF_8)).getDigestBytes(), 0L);
    }

    /**
     * Test method for
     * {@link org.openpreservation.fixity.core.digests.DigestResultImpl#of(long, String, String)}.
     * @throws IOException 
     * @throws NoSuchAlgorithmException 
     */
    @Test(expected = NoSuchAlgorithmException.class)
    public void testOfBadAlgorithm() throws NoSuchAlgorithmException, IOException {
        // Try creating a byte stream with a length less than zero, use the valid empty
        // hash values
        Algorithms.fromString("BAD-ALGORITHM");
    }

    /**
     * Test method for
     * {@link org.openpreservation.fixity.core.digests.DigestResultImpl#of(long, String, String)}.
     * @throws IOException 
     * @throws NoSuchAlgorithmException 
     */
    @Test(expected = IllegalArgumentException.class)
    public void testOfNullDigest() throws NoSuchAlgorithmException, IOException {
        // Try creating a byte stream with an empty sha value
        DigestResult.of(Algorithms.DEFAULT, null, 0L);
    }

    /**
     * Test method for
     * {@link org.opf_labs.spruce.bytestream.ByteStreams#idFromValues(long, String, String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testOfShortDigest() {
        final DigestResult nullDigest = DigestResult.NULL_DIGESTS.get(Algorithms.DEFAULT);

        // Try creating a byte stream with an empty md5 value
        DigestResult.of(Algorithms.DEFAULT, Arrays.copyOf(nullDigest.getDigestBytes(), (int) nullDigest.getDigestLength() - 1), 0L);
    }

    /**
     * Test method for
     * {@link org.opf_labs.spruce.bytestream.ByteStreams#idFromValues(long, String, String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testOfLongDigest() {
        final DigestResult nullDigest = DigestResult.NULL_DIGESTS.get(Algorithms.DEFAULT);

        // Try creating a byte stream with an empty md5 value
        DigestResult.of(Algorithms.DEFAULT, Arrays.copyOf(nullDigest.getDigestBytes(), (int) nullDigest.getDigestLength() + 1), 0L);
    }

    /**
     * Test method for
     * {@link org.opf_labs.spruce.bytestream.ByteStreams#idFromValues(long, String, String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testOfShortMessage() {
        final DigestResult nullDigest = DigestResult.NULL_DIGESTS.get(Algorithms.DEFAULT);

        // Try creating a byte stream with an empty md5 value
        DigestResult.of(Algorithms.DEFAULT, nullDigest.getDigestBytes(), -1L);
    }

    /**
     * Test the hash and equals contract for the class using EqualsVerifier
     */
    @Test
    public void testEqualsContract() {
        EqualsVerifier.forClass(DigestResult.DigestResultImpl.class).verify();
    }
}
