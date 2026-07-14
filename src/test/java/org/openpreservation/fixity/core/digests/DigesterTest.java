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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @BeforeEach
    public void setUp() throws IOException {
        emptyFile = Utils.createTempFileWithText("empty", "");
        testFile = Utils.createTempFileWithText("test", testText);
    }

    @AfterEach
    public void tearDown() throws IOException {
        Files.deleteIfExists(emptyFile);
        Files.deleteIfExists(testFile);
    }

    @SuppressWarnings("null")
    @Test
    public void testSupportedAlgorithms() throws IOException, NoSuchAlgorithmException {
        Algorithms.AVAILABLE.forEach(alg -> {
            try {
                final @NonNull Set<@NonNull DigestResult> results = Hasher.instance(Collections.singleton(alg)).hash("".getBytes(StandardCharsets.UTF_8));
                assertFalse(results.isEmpty());
                assertEquals(DigestResult.NULL_DIGESTS.get(alg), results.iterator().next());
            } catch (NoSuchAlgorithmException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testDefault() throws NoSuchAlgorithmException, IOException {
        DigestResult test = DigestResult.DEFAULT_NULL_DIGEST;
        assertSame(DigestResult.DEFAULT_NULL_DIGEST, test);
    }

    @Test
    public void testEmptyFile() throws NoSuchAlgorithmException, IOException {
        try (final FileInputStream fis = new FileInputStream(emptyFile.toFile())) {
            @SuppressWarnings("null")
            final @NonNull Set<@NonNull DigestResult> results = Hasher.instance(Collections.singleton(Algorithms.DEFAULT)).hash(fis);
            assertFalse(results.isEmpty());
            assertEquals(DigestResult.DEFAULT_NULL_DIGEST, results.iterator().next());
            assertNotSame(DigestResult.DEFAULT_NULL_DIGEST, results.iterator());
        }
    }

    @Test
    public void testTextFile() throws NoSuchAlgorithmException, IOException {
        @SuppressWarnings("null")
        final @NonNull Set<@NonNull DigestResult> expected = Hasher.instance(Collections.singleton(Algorithms.DEFAULT))
                .hash(testText.getBytes(StandardCharsets.UTF_8));
        try (final FileInputStream fis = new FileInputStream(testFile.toFile())) {
            @SuppressWarnings("null")
            final @NonNull Set<@NonNull DigestResult> results = Hasher.instance(Collections.singleton(Algorithms.DEFAULT)).hash(fis);
            assertEquals(expected, results);
            assertNotSame(expected, results);
        }
    }
}
