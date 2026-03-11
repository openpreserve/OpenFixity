package org.openpreservation.fixity;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.NonNull;

public class Utils {
    
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
        file = Files.createFile(testPath.resolve("dir1/file3.txt"));
        Files.write(file, "file3.txt".getBytes(StandardCharsets.UTF_8));
        file.toFile().setReadable(false);
        Path dir2 = Files.createDirectory(testPath.resolve("dir2"));
        file = Files.createFile(testPath.resolve("dir2/file4.txt"));
        Files.write(file, "file4.txt".getBytes(StandardCharsets.UTF_8));
        dir2.toFile().setReadable(false);
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
