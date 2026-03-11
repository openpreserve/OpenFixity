package org.openpreservation.fixity.core.paths;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
    PathScanResultTest.class,
    PathScannerTest.class,
    PathSummaryTest.class
})
public class PathsTests {
	// Nothing to do
}
