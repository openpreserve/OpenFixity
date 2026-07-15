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
package org.openpreservation.fixity;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Assumptions;

public class Utils {

    /**
     * Skip (not fail) a test whose setup needs a genuinely unreadable file or directory when the
     * OS will not create that condition. {@code File.setReadable(false)} is a no-op on Windows,
     * so a path meant to be denied stays readable there and any assertion about denial is invalid.
     *
     * <p>This does not mask a product bug: the scanner still reports DENIED for files it genuinely
     * cannot read (e.g. an NTFS ACL that denies the current user), which these tests cannot
     * synthesise on Windows. The affected tests run on Linux and macOS.
     */
    public static void assumeReadDenialHonoured(final Path path) {
        Assumptions.assumeFalse(Files.isReadable(path),
                "Skipped: this OS does not honour read-denial via setReadable (e.g. Windows), "
                        + "so the unreadable-path precondition cannot be created here: " + path);
    }
    
    public static final boolean deleteDirectory(final File directory) {
        if (directory == null || !directory.exists()) {
            return true;
        }
        boolean success = true;
        for (final File file : directory.listFiles()) {
            file.setReadable(true);
            if (file.isFile()) {
                success &= file.delete();
            } else if (file.isDirectory()) {
                success &= deleteDirectory(file);
                success &= file.delete();
            }
        }
        return success && directory.delete();
    }
   public boolean isVsCodeTestDebug() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("-Xrunjdwp");
    }

    @NonNull
    public static Path createTempTestPath(final String prefix) throws IOException {
        Path testPath = Files.createTempDirectory(prefix);
        Path file = Files.createFile(testPath.resolve("file1.txt"));
        Files.write(file, "file1.txt".getBytes(StandardCharsets.UTF_8));
        file = Files.createFile(testPath.resolve("file2.txt"));
        Files.write(file, "file2.txt".getBytes(StandardCharsets.UTF_8));
        Files.createDirectory(testPath.resolve("dir1"));
        Path file3 = Files.createFile(testPath.resolve("dir1/file3.txt"));
        Files.write(file3, "file3.txt".getBytes(StandardCharsets.UTF_8));
        file3.toFile().setReadable(false);
        Path dir2 = Files.createDirectory(testPath.resolve("dir2"));
        file = Files.createFile(testPath.resolve("dir2/file4.txt"));
        Files.write(file, "file4.txt".getBytes(StandardCharsets.UTF_8));
        dir2.toFile().setReadable(false);
        // The scan counts and statuses every consumer of this fixture asserts depend on file3
        // and dir2 being unreadable. Where the OS will not honour that (Windows), skip rather
        // than report misleading failures.
        assumeReadDenialHonoured(file3);
        assumeReadDenialHonoured(dir2);
        return testPath;
    }

    @NonNull
    public static Path createTempFileWithText(final String prefix, final String content) throws IOException {
        Path tempFile = Files.createTempFile(prefix, ".txt");
        Files.write(tempFile, content.getBytes(StandardCharsets.UTF_8));
        if (tempFile == null || !tempFile.toFile().exists()) {
            throw new IOException("Failed to create temporary file: " + tempFile);
        }
        return tempFile;
    }

    public static void addNestedDir(final Path testPath) throws IOException {
        Path file = Files.createDirectory(testPath.resolve("dir1/dir3"));
        file = Files.createFile(testPath.resolve("dir1/dir3/file4.txt"));
        Files.write(file, "file4.txt".getBytes(StandardCharsets.UTF_8));
    }

    public static void makeAllReadable(final Path testPath) throws IOException {
        testPath.toFile().setReadable(true);
        for (final File file : testPath.toFile().listFiles()) {
            file.setReadable(true);
            if (file.isDirectory()) {
                makeAllReadable(file.toPath());
            }
        }
    }
}
