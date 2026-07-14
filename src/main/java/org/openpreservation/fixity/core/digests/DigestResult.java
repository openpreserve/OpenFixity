/*
 * OpenFixity is an application for monitoring and reporting on the fixity of files.
 * Copyright (C) 2026 Open Preservation Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openpreservation.fixity.core.digests;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * An interface defining the result of a digest calculation.
 */
@NullMarked
public interface DigestResult extends Serializable {
    /**
     * Get the String identifier of the algorithm used to create this digest.
     * @return a unique String identifier of the algorithm used.
     */
    public Algorithms getAlgorithm();
    /**
     * Get the hex encoded digest value.
     * @return a String representation of the hex encoded digest value.
     */
    public String toHexString();
    /**
     * Get the raw byte array of the digest value.
     * @return a byte[] containing the raw digest value.
     */
    public byte[] getDigestBytes();
    /**
     * Get the length of the digest in bytes.
     * @return the length of the digest in bytes.
     */
    public int getDigestLength();
    /**
     * Get the length of the message that was digested in bytes.
     * @return the length of the message that was digested in bytes.
     */
    public long getMessageLength();

    final static class DigestResultImpl implements DigestResult {
        private final Algorithms algorithm;
        private final byte[] digestBytes;
        private final long messageLength;
        static final DigestResultImpl DEFAULT_INSTANCE = new DigestResultImpl();

        private DigestResultImpl() {
            this.algorithm = Algorithms.DEFAULT;
            this.digestBytes = this.algorithm.getNullBytes();
            this.messageLength = "".getBytes(StandardCharsets.UTF_8).length;
        }

        private DigestResultImpl(
                final Algorithms algorithm,
                final byte @NonNull [] digestBytes,
                final long messageLength) {
            this.algorithm = algorithm;
            // Copy on the way in. A digest is a value; if it stayed a shared reference the
            // caller could mutate a recorded checksum after the fact.
            this.digestBytes = digestBytes.clone();
            this.messageLength = messageLength;
        }

        @Override
        public Algorithms getAlgorithm() {
            return this.algorithm;
        }

        @Override
        public String toHexString() {
            return Algorithms.formatHex(this.digestBytes);
        }

        @Override
        public byte @NonNull [] getDigestBytes() {
            // Copy on the way out, for the same reason.
            return this.digestBytes.clone();
        }

        @Override
        public int getDigestLength() {
            return this.digestBytes.length;
        }

        @Override
        public long getMessageLength() {
            return this.messageLength;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(digestBytes);
            result = prime * result + Objects.hash(algorithm, messageLength);
            return result;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof DigestResultImpl))
                return false;
            final DigestResultImpl other = (DigestResultImpl) obj;
            return Objects.equals(algorithm, other.algorithm) && Arrays.equals(digestBytes, other.digestBytes)
                    && messageLength == other.messageLength;
        }

        @Override
        @SuppressWarnings("null")
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("DigestResultImpl [");
            if (algorithm != null)
                builder.append("algorithm=").append(algorithm).append(", ");
            if (digestBytes != null)
                builder.append("digestBytes=").append(Arrays.toString(digestBytes)).append(", ");
            builder.append("messageLength=").append(messageLength).append(", ");
            if (toHexString() != null)
                builder.append("digestHex=").append(toHexString()).append(", ");
            builder.append("digestLength=").append(getDigestLength()).append("]");
            return builder.toString();
        }
    }
    public static final Map<@NonNull Algorithms, @NonNull DigestResult> NULL_DIGESTS = nullDigests();
    @SuppressWarnings("null")
    public static final DigestResult DEFAULT_NULL_DIGEST = NULL_DIGESTS.get(Algorithms.DEFAULT);

    @SuppressWarnings("null")
    private static Map<@NonNull Algorithms, @NonNull DigestResult> nullDigests() {
        final Map<Algorithms, DigestResult> nulls = new HashMap<>();
        for (Algorithms algorithm : Algorithms.AVAILABLE) {
            nulls.put(algorithm, new DigestResultImpl(algorithm, algorithm.getNullBytes(), 0L));
        }
        return Collections.unmodifiableMap(nulls);
    }

    /**
     * Create a DigestResult instance.
     * @param algorithm the String identifier of the algorithm used.
     * @param digestBytes the byte array containing the digest value.
     * @param messageLength the length of the original message.
     * @return a DigestResult instance.
     */
    public static DigestResult of(
            final Algorithms algorithm,
            final byte @NonNull[] digestBytes,
            final long messageLength) {
        if (!Algorithms.AVAILABLE.contains(algorithm)) {
            throw new IllegalArgumentException("algorithm argument is null or not a supported digest algorithm");
        }
        if (digestBytes.length != NULL_DIGESTS.get(algorithm).getDigestLength()) {
            throw new IllegalArgumentException(String.format("digestBytes argument length %d does not match expected length %d for algorithm %s",
                                                             digestBytes.length, NULL_DIGESTS.get(algorithm).getDigestLength(),
                                                             algorithm));
        }
        if (messageLength < 0L) {
            throw new IllegalArgumentException("messageLength argument is less than zero");
        }
        return new DigestResultImpl(algorithm, digestBytes, messageLength);
    }
}