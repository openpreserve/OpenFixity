package org.openpreservation.fixity.core.digests;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.jspecify.annotations.NonNull;

public interface Hasher {
    /**
     * Calculate the digest of the provided InputStream message
     *
     * @param message an InputStream containing the message to digest.
     * @return DigestResult The result of the digest calculation.
     * @throws java.io.IOException if an I/O error occurs during reading of the message.
     */
    public @NonNull DigestResult hash(final InputStream message) throws IOException;
    /**
     * Calculate the digest of the provided byte[] message using the specified algorithm.
     *
     * @param message a byte[] containing the message to digest.
     * @return DigestResult The result of the digest calculation.
     */
    public DigestResult hash(final byte[] message) throws IOException;

    final class HasherImpl implements Hasher {
        // Buffer size for reading streams
        private static final int BUFFER_SIZE = (8 * 1024);
        private final MessageDigest messageDigest;
        private HasherImpl(final Algorithms algorithm) throws NoSuchAlgorithmException {
            this.messageDigest = MessageDigest.getInstance(algorithm.getName());
        }

        @Override
        public @NonNull DigestResult hash(final InputStream message) throws IOException {
            this.messageDigest.reset();
            final DigestInputStream sha1Stream = new DigestInputStream(message, this.messageDigest);
            // Wrap them all in a buffered stream for efficiency
            final BufferedInputStream bis = new BufferedInputStream(sha1Stream);
            final byte[] buff = new byte[BUFFER_SIZE];
            long totalBytes = 0L;
            long bytesRead = 0L;
            // Read the entire stream while calculating the length
            while ((bytesRead = bis.read(buff, 0, BUFFER_SIZE)) > -1) {
                totalBytes += bytesRead;
            }
            // Return the new instance from the calulated details
            try {
                return DigestResult.of(Algorithms.fromString(this.messageDigest.getAlgorithm()), this.messageDigest.digest(), totalBytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("Unexpected error calculating digest result: " + e.getMessage(), e);
            }
        }

        @Override
        public DigestResult hash(final byte[] message) throws IOException {
            // Implementation goes here
            return this.hash(new ByteArrayInputStream(message));
        }
    }

    public static Hasher instance(final Algorithms algorithm) throws NoSuchAlgorithmException {
        return new HasherImpl(algorithm);
    }
}
