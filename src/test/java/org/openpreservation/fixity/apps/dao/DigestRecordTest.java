package org.openpreservation.fixity.apps.dao;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HexFormat;

import org.junit.jupiter.api.Test;
import org.openpreservation.fixity.core.digests.Algorithms;
import org.openpreservation.fixity.core.digests.DigestResult;

public class DigestRecordTest {

    // --- of(Algorithms, String, long) ---

    @Test
    public void testGetAlgorithmReturnsConstructedAlgorithm() {
        DigestRecord record = DigestRecord.of(Algorithms.SHA_256, "hex", 0L);
        assertEquals(Algorithms.SHA_256, record.getAlgorithm());
    }

    @Test
    public void testToHexStringReturnsStoredDigest() {
        DigestRecord record = DigestRecord.of(Algorithms.SHA_256, "abcd1234", 0L);
        assertEquals("abcd1234", record.toHexString());
    }

    @Test
    public void testGetMessageLengthReturnsConstructedValue() {
        DigestRecord record = DigestRecord.of(Algorithms.SHA_256, "hex", 512L);
        assertEquals(512L, record.getMessageLength());
    }

    // --- getDigestBytes / getDigestLength ---

    @Test
    public void testGetDigestBytesAreRawBytesOfHexString() {
        // "abcd1234" is 4 raw bytes (8 hex chars), not 8 UTF-8 bytes
        String hex = "abcd1234";
        DigestRecord record = DigestRecord.of(Algorithms.SHA_256, hex, 0L);
        assertArrayEquals(HexFormat.of().parseHex(hex), record.getDigestBytes());
    }

    @Test
    public void testGetDigestLengthMatchesRawByteCount() {
        // 8 hex chars → 4 raw bytes
        DigestRecord record = DigestRecord.of(Algorithms.SHA_256, "abcd1234", 0L);
        assertEquals(4, record.getDigestLength());
    }

    // --- getShortenedDigest ---

    @Test
    public void testGetShortenedDigestTruncatesWhenLongerThanEightChars() {
        // digest is 10 chars — should be cut to 8 + "..."
        DigestRecord record = DigestRecord.of(Algorithms.SHA_256, "abcdefghij", 0L);
        assertEquals("SHA-256: abcdefgh...", record.getShortenedDigest());
    }

    @Test
    public void testGetShortenedDigestDoesNotTruncateEightCharDigest() {
        // exactly 8 chars — boundary: length > 8 is false, so no truncation
        DigestRecord record = DigestRecord.of(Algorithms.SHA_256, "abcd1234", 0L);
        assertEquals("SHA-256: abcd1234", record.getShortenedDigest());
    }

    @Test
    public void testGetShortenedDigestDoesNotTruncateShortDigest() {
        DigestRecord record = DigestRecord.of(Algorithms.SHA_256, "abc", 0L);
        assertEquals("SHA-256: abc", record.getShortenedDigest());
    }

    // --- of(DigestResult) ---

    @Test
    public void testOfDigestResultCopiesAllFields() {
        DigestResult dr = DigestResult.of(Algorithms.SHA_256, Algorithms.SHA_256.getNullBytes(), 42L);
        DigestRecord record = DigestRecord.of(dr);
        assertEquals(Algorithms.SHA_256, record.getAlgorithm());
        assertEquals(Algorithms.SHA_256.getNullHex(), record.toHexString());
        assertEquals(42L, record.getMessageLength());
    }
}
