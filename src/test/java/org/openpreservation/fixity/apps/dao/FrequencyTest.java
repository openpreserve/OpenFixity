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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.quartz.CronExpression;

public class FrequencyTest {

    @Test
    public void testHourlyCron() {
        // Every hour at 30 minutes past: seconds=0 minutes=30 hours=* dom=* month=* dow=?
        assertEquals("0 30 * * * ?", Frequency.HOURLY.toCron(30, 0, 1));
    }

    @Test
    public void testDailyCron() {
        // Every day at 02:15
        assertEquals("0 15 2 * * ?", Frequency.DAILY.toCron(15, 2, 1));
    }

    @Test
    public void testWeeklyCron() {
        // Every week on Monday (Quartz dow 2) at 09:00
        assertEquals("0 0 9 ? * 2", Frequency.WEEKLY.toCron(0, 9, 2));
    }

    @Test
    public void testGeneratedCronsAreValidQuartzExpressions() throws Exception {
        // The real proof: Quartz itself must accept every expression we generate.
        CronExpression.validateExpression(Frequency.HOURLY.toCron(5, 0, 1));
        CronExpression.validateExpression(Frequency.DAILY.toCron(45, 23, 1));
        CronExpression.validateExpression(Frequency.WEEKLY.toCron(0, 0, 7));
    }

    @Test
    public void testInvalidMinuteRejected() {
        assertThrows(IllegalArgumentException.class, () -> Frequency.DAILY.toCron(60, 2, 1));
    }

    @Test
    public void testInvalidHourRejected() {
        assertThrows(IllegalArgumentException.class, () -> Frequency.DAILY.toCron(0, 24, 1));
    }

    @Test
    public void testInvalidDayOfWeekRejected() {
        assertThrows(IllegalArgumentException.class, () -> Frequency.WEEKLY.toCron(0, 0, 8));
    }
}
