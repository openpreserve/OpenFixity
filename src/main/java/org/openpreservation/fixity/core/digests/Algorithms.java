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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Set;
import java.util.TreeSet;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

import com.fasterxml.jackson.annotation.JsonValue;

@NullMarked
public enum Algorithms {
    /**
     * Algorithms listed in order of preference for use in fixity checks, with the most preferred listed first.
     */
    SHA_1("SHA-1", "da39a3ee5e6b4b0d3255bfef95601890afd80709"),
    SHA_256("SHA-256", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),
    SHA_512("SHA-512", "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e"),
    SHA_224("SHA-224", "d14a028c2a3a2bc9476102bb288234c415a2b01f828ea62ac5b3e42f"),
    SHA_384("SHA-384", "38b060a751ac96384cd9327eb1b1e36a21fdb71114be07434c0cc7bf63f6e1da274edebfe76f65fbd51ad2f14898b95b"),
    SHA_512_224("SHA-512/224", "6ed0dd02806fa89e25de060c19d3ac86cabb87d6a0ddd05c333b84f4"),
    SHA_512_256("SHA-512/256", "c672b8d1ef56ed28ab87c3622c5114069bdd3ad7b8f9737498d0c01ecef0967a"),
    MD2("MD2", "8350e5a3e24c153df2275c9f80692773"),
    MD5("MD5", "d41d8cd98f00b204e9800998ecf8427e"),
    SHA3_224("SHA3-224", "6b4e03423667dbb73b6e15454f0eb1abd4597f9a1b078e3f5b5a6bc7"),
    SHA3_256("SHA3-256", "a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a"),
    SHA3_384("SHA3-384", "0c63a75b845e4f7d01107d852e4c2485c51a50aaaa94fc61995e71bbee983a2ac3713831264adb47fb6bd1e058d5f004"),
    SHA3_512("SHA3-512", "a69f73cca23a9ac5c8b567dc185a756e97c982164fe25859e0d1dcc1475c80a615b2123af1f5f94c11e3e9402c3ac558f500199d95b6d3e301758586281dcd26");
    @SuppressWarnings("null")
    private static final String SERVICE_TYPE = MessageDigest.class.getSimpleName();;
    /**
     * A sorted set of all available message digest algorithms.
     * These can be used to identify algorithms supported by the digestmer implementations.
     */
    @SuppressWarnings("null")
    public static final EnumSet<@NonNull Algorithms> SUPPORTED = EnumSet.allOf(Algorithms.class);
    public static final EnumSet<@NonNull Algorithms> AVAILABLE = getAvailable();
    public static final Algorithms DEFAULT = getDefault();
    public static Algorithms fromString(final String name) throws NoSuchAlgorithmException {
        for (final Algorithms alg : SUPPORTED) {
            if (alg.name.equalsIgnoreCase(name)) {
                return alg;
            }
        }
        throw new NoSuchAlgorithmException("Unsupported algorithm: " + name); }    

    /**
     * Return a sorted set of all available message digest algorithms.
     * Used to populate the SUPPORTED_ALGORITHMS constant at initialization.
     *
     * @return Set<String> A set of available message digest algorithm names, sorted alphabetically.
     */
    @SuppressWarnings("null")
    private static final EnumSet<@NonNull Algorithms> getAvailable() {
        final Set<Algorithms> algorithms = new TreeSet<>();
        for (final Provider provider : Security.getProviders()) {
            for (final Provider.Service service : provider.getServices()) {
                if (SERVICE_TYPE.equalsIgnoreCase(service.getType())) {
                    try {
                        final String algName = service.getAlgorithm();
                        if (algName != null) algorithms.add(Algorithms.fromString(algName));
                    } catch (NoSuchAlgorithmException e) {
                        // We can ignore this exception, it simply means that a new JVM algorithm
                        // has been added that we haven't added to our enum yet.
                        // We will catch this and add it to the enum in a future release.
                    }
                }
            }
        }
        if (algorithms.isEmpty()) {
            throw new IllegalStateException("No message digest algorithms available in this Java Runtime Environment.");
        }
        return (algorithms != null) ? EnumSet.copyOf(algorithms) : EnumSet.noneOf(Algorithms.class);
    }
    private static final Algorithms getDefault() {
        for (final Algorithms alg : Algorithms.values()) { if (AVAILABLE.contains(alg)) { return alg; } }
        throw new IllegalStateException("No supported message digest algorithms available in this Java Runtime Environment.");
    }
    
    private final String name;
    private final byte[] nullBytes;

    private Algorithms(final String name, final String nullHex) {
        this.name = name;
        this.nullBytes = parseHex(nullHex);
    }

    static final byte[] parseHex(final String hexString) {
        final byte[] bytes = HexFormat.of().parseHex(hexString);
        return (bytes != null) ? bytes : new byte[0];
    }

    static final String formatHex(final byte[] bytes) {
        final String hexString = HexFormat.of().formatHex(bytes);
        return (hexString != null) ? hexString : "";
    }

    @JsonValue
    public String getName() { return this.name; }
    public String getNullHex() { return formatHex(nullBytes); }
    // Enum constants are JVM-wide singletons, so handing out the live array would let any
    // caller permanently corrupt this algorithm's reference digest. Copy on the way out.
    public byte[] getNullBytes() { return this.nullBytes.clone(); }
    public int getLength() { return this.nullBytes.length; }
    @SuppressWarnings("null")
    public MessageDigest getMessageDigest() throws NoSuchAlgorithmException { return MessageDigest.getInstance(this.name); }
}
