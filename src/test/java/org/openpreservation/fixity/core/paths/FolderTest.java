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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpreservation.fixity.Utils;
import org.openpreservation.fixity.core.digests.Algorithms;
import org.openpreservation.fixity.core.digests.Hasher;

import nl.jqno.equalsverifier.EqualsVerifier;

@SuppressWarnings("null")
public class FolderTest {
    Path testDir;
    Path file;

    @BeforeEach
    public void setUp() throws IOException {
        testDir = Files.createTempDirectory("folder-test");
        file = testDir.resolve("file.txt");
        Files.writeString(file, "content");
    }

    @AfterEach
    public void tearDown() throws IOException {
        Utils.deleteDirectory(testDir);
    }

    @Test
    public void testEquals() {
        EqualsVerifier.forClass(Folder.class).verify();
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testEqualsNonFolder() {
        assertFalse(Folder.readable(null, testDir).equals("not a folder"));
    }

    @Test
    public void testEqualsFolder() {
        assertTrue(Folder.readable(null, testDir).equals(Folder.readable(null, testDir)));
    }

    // --- Factory methods ---

    @Test
    public void testReadableRootIsReadableAndIsRoot() {
        Folder folder = Folder.readable(null, testDir);
        assertTrue(folder.isReadable());
        assertTrue(folder.isRoot());
        assertNull(folder.getParent());
    }

    @Test
    public void testReadableWithParentIsNotRoot() {
        Folder parent = Folder.readable(null, testDir);
        Folder child = Folder.readable(parent, testDir.resolve("sub"));
        assertFalse(child.isRoot());
        assertEquals(parent, child.getParent());
    }

    @Test
    public void testUnreadableIsNotReadable() {
        Folder folder = Folder.unreadable(null, testDir);
        assertFalse(folder.isReadable());
        assertTrue(folder.isRoot());
        assertTrue(folder.getFileScanResults().isEmpty());
    }

    @Test
    public void testOfWithResultsIsReadableAndContainsResults() throws IOException, NoSuchAlgorithmException {
        FileScanResult result = FileScanResult.of(file, Hasher.instance(EnumSet.of(Algorithms.SHA_256)));
        Folder folder = Folder.of(null, testDir, Set.of(result));
        assertTrue(folder.isReadable());
        assertEquals(1, folder.getFileScanResults().size());
        assertTrue(folder.getFileScanResults().contains(result));
    }

    // --- addFileScanResult ---

    @Test
    public void testAddFileScanResultAddsToSet() throws IOException, NoSuchAlgorithmException {
        Folder folder = Folder.readable(null, testDir);
        FileScanResult result = FileScanResult.of(file, Hasher.instance(EnumSet.of(Algorithms.SHA_256)));
        folder.addFileScanResult(result);
        assertEquals(1, folder.getFileScanResults().size());
    }

    @Test
    public void testAddFileScanResultRejectsFileNotInFolder() throws IOException, NoSuchAlgorithmException {
        Path subDir = Files.createDirectory(testDir.resolve("sub"));
        Path subFile = subDir.resolve("other.txt");
        Files.writeString(subFile, "other");
        Folder folder = Folder.readable(null, testDir);
        FileScanResult wrongResult = FileScanResult.of(subFile, Hasher.instance(EnumSet.of(Algorithms.SHA_256)));
        assertThrows(IllegalArgumentException.class, () -> folder.addFileScanResult(wrongResult));
    }

    // --- equals / hashCode ---

    @Test
    public void testEqualFoldersAreEqual() {
        Folder a = Folder.readable(null, testDir);
        Folder b = Folder.readable(null, testDir);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testFoldersWithDifferentPathsAreNotEqual() {
        Folder a = Folder.readable(null, testDir);
        Folder b = Folder.readable(null, testDir.resolve("other"));
        assertFalse(a.equals(b));
    }

    // --- Static utility methods ---

    @Test
    public void testResolve() {
        Path root = Path.of("/tmp");
        Path relative = Path.of("sub/file.txt");
        assertEquals(Path.of("/tmp/sub/file.txt"), Folder.resolve(root, relative));
    }

    @Test
    public void testRelativise() {
        String root = testDir.toString();
        assertEquals("file.txt", Folder.relativise(root, file));
    }

    @Test
    public void testAbsolutePathString() {
        Path relative = Path.of("a/../b");
        String result = Folder.absolutePathString(relative);
        assertNotNull(result);
        assertFalse(result.contains(".."));
    }

    @Test
    public void testGetFileName() {
        assertEquals(Path.of("file.txt"), Folder.getFileName(file));
    }

    @Test
    public void testAbsolutePath() {
        Path relative = Path.of("a/../b");
        Path result = Folder.absolutePath(relative);
        assertFalse(result.toString().contains(".."));
        assertTrue(result.isAbsolute());
    }
}
