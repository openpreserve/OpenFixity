package org.openpreservation.fixity.core.digests;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface Hasher {
    /**
     * Calculate the digest of the provided InputStream message
     *
     * @param message an InputStream containing the message to digest.
     * @return DigestResult The result of the digest calculation.
     * @throws java.io.IOException if an I/O error occurs during reading of the message.
     */
    public Set<@NonNull DigestResult> hash(final InputStream message) throws IOException;
    /**
     * Calculate the digest of the provided byte[] message using the specified algorithm.
     *
     * @param message a byte[] containing the message to digest.
     * @return DigestResult The result of the digest calculation.
     */
    public Set<@NonNull DigestResult> hash(final byte[] message) throws IOException;

    final class HasherImpl implements Hasher {
        // Buffer size for reading streams
        private static final int BUFFER_SIZE = (8 * 1024);
        private final Set<@NonNull MessageDigest> messageDigest = new HashSet<>();
        private HasherImpl(final Set<@NonNull Algorithms> algorithms) throws NoSuchAlgorithmException {
            super();
            if (algorithms.isEmpty()) {
                throw new IllegalArgumentException("At least one algorithm must be provided");
            }
            for (final Algorithms alg : algorithms) {
                MessageDigest md = MessageDigest.getInstance(alg.getName());
                if (md == null) {
                    throw new NoSuchAlgorithmException("Algorithm not found: " + alg.getName());
                }
                this.messageDigest.add(md);
            }
        }

        @Override
        public Set<@NonNull DigestResult> hash(final InputStream message) throws IOException {
            final DigestInputStream combinedStreams = this.combineStreams(message);
            final byte[] buff = new byte[BUFFER_SIZE];
            long totalBytes = 0L;
            long bytesRead = 0L;
            final Set<@NonNull DigestResult> results = new HashSet<>();
            // Wrap them all in a buffered stream for efficiency
            try (final BufferedInputStream bis = new BufferedInputStream(combinedStreams)) {
                // Read the entire stream while calculating the length
                while ((bytesRead = bis.read(buff, 0, BUFFER_SIZE)) > -1) {
                    totalBytes += bytesRead;
                }
            }
            for (final MessageDigest md : this.messageDigest) {
                try {
                    if (md != null) {
                        final String algName = md.getAlgorithm();
                        final byte[] digestBytes = md.digest();
                        if (algName != null && digestBytes != null) {
                            results.add(DigestResult.of(Algorithms.fromString(algName), digestBytes, totalBytes));
                        }
                    }
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException("Unexpected error calculating digest result: " + e.getMessage(), e);
                }
            }
            return results;
        }

        private DigestInputStream combineStreams(final InputStream message) {
            DigestInputStream combinedStream = null;
            for (final MessageDigest md : this.messageDigest) {
                md.reset();
                combinedStream = (combinedStream == null) ? new DigestInputStream(message, md) : new DigestInputStream(combinedStream, md);
            }
            if (combinedStream == null) {
                throw new IllegalStateException("Failed to combine streams: no MessageDigests available");
            }
            return combinedStream;
        }

        @Override
        public Set<@NonNull DigestResult> hash(final byte[] message) throws IOException {
            // Implementation goes here
            return this.hash(new ByteArrayInputStream(message));
        }
    }

    
    public static Hasher instance(final Set<@NonNull Algorithms> algorithms) throws NoSuchAlgorithmException {
        return new HasherImpl(algorithms);
    }
}
