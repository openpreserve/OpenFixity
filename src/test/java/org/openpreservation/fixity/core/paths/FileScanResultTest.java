package org.openpreservation.fixity.core.paths;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpreservation.fixity.Utils;

import nl.jqno.equalsverifier.EqualsVerifier;

public class FileScanResultTest {
    Path testPath;

    @BeforeEach
    public void setUp() throws IOException, NoSuchAlgorithmException {
        testPath = Utils.createTempTestPath("fixity-fileresult-tests");
    }

    @AfterEach
    public void tearDown() throws IOException {
        Utils.deleteDirectory(testPath.toFile());
    }

    @Test
    public void testEquals() {
        EqualsVerifier.forClass(FileScanResult.FileScanResultImpl.class).verify();
    }
}
