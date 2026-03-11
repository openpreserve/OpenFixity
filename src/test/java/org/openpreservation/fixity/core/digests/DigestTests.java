package org.openpreservation.fixity.core.digests;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({ DigestResultTest.class, DigesterTest.class })
public class DigestTests {
	// Nothing to do
}
