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
package org.openpreservation.fixity.apps.dao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

/**
 * Guards the persisted representation of every enum on every entity.
 *
 * JPA maps an enum as ORDINAL unless told otherwise, storing its position rather than its
 * name. Reordering or inserting a constant then silently changes the meaning of every row
 * already written.
 *
 * This is not hypothetical here. The Algorithms enum was reordered during development:
 * SHA_1 moved from ordinal 2 to ordinal 0, and SHA_256 from 4 to 1. Any database written
 * before that reorder and read after it would have had every digest re-attributed to a
 * different algorithm, and DigestRecord.algorithm was stored as an ordinal. A SHA-1 hash
 * would have been read back as SHA-512. checkDigests() matches on algorithm, so the fixity
 * comparison would either find no match and report UNVERIFIED, quietly abandoning the check,
 * or match the wrong algorithm and report CHANGED on a file that never changed.
 *
 * For a tool whose entire purpose is detecting silent change, that is the one failure mode
 * we cannot ship. Rather than name the columns individually, which is how the DigestRecord
 * one was missed in the first place, this reflects over the entities and checks all of them.
 */
@SuppressWarnings("null")
public class EnumMappingTest {

    /** Every entity registered with the session factory. */
    private static final Class<?>[] ENTITIES = {
            Collection.class,
            CollectionPath.class,
            DigestAlgorithm.class,
            DigestRecord.class,
            FileScanRecord.class,
            FolderScanRecord.class,
            PathRegistration.class,
            PathScan.class,
            PathSummaryRecord.class,
    };

    @BeforeEach
    public void setUp() {
        TestSessionFactory.beginTransaction();
    }

    @AfterEach
    public void tearDown() {
        TestSessionFactory.rollback();
    }

    private static String tableName(final Class<?> entity) {
        Table table = entity.getAnnotation(Table.class);
        String name = (table != null && !table.name().isEmpty()) ? table.name() : entity.getSimpleName();
        return name.toUpperCase(Locale.ROOT);
    }

    private static String columnName(final Field field) {
        Column column = field.getAnnotation(Column.class);
        String name = (column != null && !column.name().isEmpty()) ? column.name() : field.getName();
        return name.toUpperCase(Locale.ROOT);
    }

    /** The declared column type, or null when the entity is not mapped to a table. */
    private static String columnType(final String table, final String column) {
        List<String> types = TestSessionFactory.currentSession()
                .createNativeQuery("SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE UPPER(TABLE_NAME) = :table AND UPPER(COLUMN_NAME) = :column", String.class)
                .setParameter("table", table)
                .setParameter("column", column)
                .getResultList();
        return types.isEmpty() ? null : types.get(0).toUpperCase(Locale.ROOT);
    }

    @Test
    public void testNoEnumIsPersistedAsAnOrdinal() {
        List<String> offenders = new ArrayList<>();
        int checked = 0;

        for (final Class<?> entity : ENTITIES) {
            for (final Field field : entity.getDeclaredFields()) {
                if (!field.getType().isEnum()) {
                    continue;
                }
                final String table = tableName(entity);
                final String column = columnName(field);
                final String type = columnType(table, column);
                if (type == null) {
                    // Not mapped to a table in this schema; nothing to assert.
                    continue;
                }
                checked++;
                // A name-mapped enum lands on H2's native ENUM type, or a text type elsewhere.
                // An ordinal mapping lands on an integer type, which is the regression to catch.
                if (!type.contains("CHAR") && !type.equals("ENUM")) {
                    offenders.add(entity.getSimpleName() + "." + field.getName()
                            + " -> " + table + "." + column + " is " + type);
                }
            }
        }

        assertTrue(checked > 0, "reflected over no enum columns at all, so this test proves nothing");
        assertTrue(offenders.isEmpty(),
                "These enums are stored by ordinal, so reordering the enum would silently rewrite "
                        + "the meaning of every existing row. Add @Enumerated(EnumType.STRING):\n  "
                        + String.join("\n  ", offenders));
    }

    @Test
    public void testTheDigestAlgorithmColumnSpecifically() {
        // Called out on its own because this is the one that bites hardest: the algorithm
        // decides which previous digest a scan compares against.
        String type = columnType("DIGESTRECORD", "ALGORITHM");
        assertNotNull(type, "DIGESTRECORD.ALGORITHM not found");
        assertFalse(type.contains("INT"), "DIGESTRECORD.ALGORITHM is " + type
                + ". Digests would be re-attributed to the wrong algorithm if Algorithms is reordered.");
    }
}
