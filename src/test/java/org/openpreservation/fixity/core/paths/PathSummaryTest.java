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
package org.openpreservation.fixity.core.paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpreservation.fixity.Utils;

import nl.jqno.equalsverifier.EqualsVerifier;

public class PathSummaryTest {
    Path testPath;

    @BeforeEach
    public void setUp() throws IOException, NoSuchAlgorithmException {
        testPath = Utils.createTempTestPath("fixity-pathsummary-tests");
    }

    @AfterEach
    public void tearDown() throws IOException {
        Utils.deleteDirectory(testPath);
    }

    @Test
    public void testEquals() {
        EqualsVerifier.forClass(PathSummary.PathSummaryImpl.class).verify();
    }

    @SuppressWarnings("null")
    @Test
    public void testOfLongPath() throws IOException {
        PathSummary summary = PathSummary.of(this.testPath, true);
        assertEquals(PathSummary.of(this.testPath, 3, 27, 1, 1), summary);
    }
}
