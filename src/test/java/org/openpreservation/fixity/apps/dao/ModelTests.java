package org.openpreservation.fixity.apps.dao;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ MockScannerTest.class, CollectionTest.class, PathRegistrationTest.class, CollectionPathTest.class, PathScanTest.class })
public class ModelTests {

}
