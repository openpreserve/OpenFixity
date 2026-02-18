package org.openpreservation.fixity.core.paths;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openpreservation.fixity.Utils;

import nl.jqno.equalsverifier.EqualsVerifier;

public class PathSummaryTest {
    Path testPath;

    @Before
    public void setUp() throws IOException, NoSuchAlgorithmException {
        testPath = Utils.createTempTestPath("fixity-pathsummary-tests");
    }

    @After
    public void tearDown() throws IOException {
        Utils.deleteDirectory(testPath.toFile());
    }

    @Test
    public void testEquals() {
        EqualsVerifier.forClass(PathSummary.PathSummaryImpl.class).verify();
    }

    @Test
    public void testOfLongPath() throws IOException {
        PathSummary summary = PathSummary.of(this.testPath, true);
        assertEquals(PathSummary.of(this.testPath, 3, 27, 1, 1), summary);
    }
}
