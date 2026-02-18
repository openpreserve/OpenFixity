package org.openpreservation.fixity.core.paths;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openpreservation.fixity.Utils;

import nl.jqno.equalsverifier.EqualsVerifier;

public class FileScanResultTest {
    Path testPath;

    @Before
    public void setUp() throws IOException, NoSuchAlgorithmException {
        testPath = Utils.createTempTestPath("fixity-fileresult-tests");
    }

    @After
    public void tearDown() throws IOException {
        Utils.deleteDirectory(testPath.toFile());
    }

    @Test
    public void testEquals() {
        EqualsVerifier.forClass(FileScanResult.FileScanResultImpl.class).verify();
    }
}
