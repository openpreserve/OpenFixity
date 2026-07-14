package org.openpreservation.fixity.apps.dao;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Guards the persisted representation of the status enums.
 *
 * JPA maps an enum as ORDINAL unless told otherwise, storing its position rather than its
 * name. Reordering or inserting a constant then silently changes the meaning of every row
 * already written. For a tool whose entire purpose is detecting silent change, that is the
 * one failure mode we cannot ship, so these columns must be stored by name.
 */
@SuppressWarnings("null")
public class EnumMappingTest {

    @BeforeEach
    public void setUp() {
        TestSessionFactory.beginTransaction();
    }

    @AfterEach
    public void tearDown() {
        TestSessionFactory.rollback();
    }

    private static String columnType(final String table, final String column) {
        List<String> types = TestSessionFactory.currentSession()
                .createNativeQuery("SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE UPPER(TABLE_NAME) = :table AND UPPER(COLUMN_NAME) = :column", String.class)
                .setParameter("table", table)
                .setParameter("column", column)
                .getResultList();
        assertNotNull(types);
        assertTrue(types.size() == 1, "expected exactly one " + table + "." + column + ", found " + types);
        return types.get(0).toUpperCase();
    }

    private static void assertStoredByName(final String table, final String column) {
        String type = columnType(table, column);
        // A name-mapped enum lands on H2's native ENUM type, or on a text type elsewhere.
        // An ordinal mapping lands on an integer type, which is the regression to catch.
        assertTrue(type.contains("CHAR") || type.equals("ENUM"),
                table + "." + column + " has column type " + type + ". The enum is being stored by "
                        + "ordinal, so reordering the enum would silently rewrite the meaning of every "
                        + "existing record. Add @Enumerated(EnumType.STRING).");
    }

    @Test
    public void testFileScanRecordAuditStatusStoredByName() {
        assertStoredByName("FILESCANRECORD", "AUDITSTATUS");
    }

    @Test
    public void testFileScanRecordStatusStoredByName() {
        assertStoredByName("FILESCANRECORD", "STATUS");
    }

    @Test
    public void testPathScanStatusStoredByName() {
        assertStoredByName("PATHSCAN", "STATUS");
    }
}
