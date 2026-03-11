package org.openpreservation.fixity.apps.dao;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({ MockScannerTest.class, CollectionTest.class, PathRegistrationTest.class, CollectionPathTest.class, PathScanTest.class })
public class ModelTests {

}
