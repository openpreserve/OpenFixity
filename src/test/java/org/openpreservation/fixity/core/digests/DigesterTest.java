/**
 * 
 */
package org.openpreservation.fixity.core.digests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openpreservation.fixity.Utils;

/**
 * @author <a href="mailto:carl@openplanetsfoundation.org">Carl Wilson</a>
 *         <a href="https://github.com/carlwilson">carlwilson AT github</a>
 * @version 0.1
 * 
 *          Created 20 Jul 2012:03:29:11
 */

public class DigesterTest {
    Path emptyFile;
    static final String testText = "The quick brown fox jumps over the lazy dog";
    Path testFile;

    @Before
    public void setUp() throws IOException {
        emptyFile = Utils.createTempFileWithText("empty", "");
        testFile = Utils.createTempFileWithText("test", testText);
    }

    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists(emptyFile);
        Files.deleteIfExists(testFile);
    }

    @Test
    public void testSupportedAlgorithms() throws IOException, NoSuchAlgorithmException {
        Algorithms.AVAILABLE.forEach(alg -> {
            try {
                DigestResult result = Hasher.instance(alg).hash("".getBytes(StandardCharsets.UTF_8));
                assertEquals(DigestResult.NULL_DIGESTS.get(alg), result);
            } catch (NoSuchAlgorithmException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testDefault() throws NoSuchAlgorithmException, IOException {
        // Get the null streamId
        DigestResult test = DigestResult.DEFAULT_NULL_DIGEST;
        assertSame(DigestResult.DEFAULT_NULL_DIGEST, test);
    }

    @Test
    public void testEmptyFile() throws NoSuchAlgorithmException, IOException {
        // Get the null streamId
        try (final FileInputStream fis = new FileInputStream(emptyFile.toFile())) {
            final DigestResult result = Hasher.instance(Algorithms.DEFAULT).hash(fis);
            assertEquals(DigestResult.DEFAULT_NULL_DIGEST, result);
            assertNotSame(DigestResult.DEFAULT_NULL_DIGEST, result);
        }
    }

    @Test
    public void testTextFile() throws NoSuchAlgorithmException, IOException {
        // Get the null streamId
        final DigestResult expected = Hasher.instance(Algorithms.DEFAULT)
                .hash(testText.getBytes(StandardCharsets.UTF_8));
        try (final FileInputStream fis = new FileInputStream(testFile.toFile())) {
            final DigestResult result = Hasher.instance(Algorithms.DEFAULT).hash(fis);
            assertEquals(expected, result);
            assertNotSame(expected, result);
        }
    }
}
