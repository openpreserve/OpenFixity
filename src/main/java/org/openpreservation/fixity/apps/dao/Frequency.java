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

/**
 * How often a scheduled scan repeats. Each value knows how to render itself as a Quartz cron
 * expression, so the rest of the app deals in friendly presets and only Quartz sees cron.
 *
 * <p>Quartz cron fields are: {@code seconds minutes hours day-of-month month day-of-week}.
 * Day-of-week is 1 (Sunday) to 7 (Saturday).
 */
public enum Frequency {
    HOURLY,
    DAILY,
    WEEKLY;

    /**
     * Build the Quartz cron expression for this frequency.
     *
     * @param minute    minute past the hour, 0-59 (all frequencies)
     * @param hour      hour of day, 0-23 (DAILY and WEEKLY; ignored for HOURLY)
     * @param dayOfWeek Quartz day-of-week, 1 (Sunday) to 7 (Saturday) (WEEKLY only)
     */
    public String toCron(final int minute, final int hour, final int dayOfWeek) {
        if (minute < 0 || minute > 59) {
            throw new IllegalArgumentException("minute must be 0-59, was " + minute);
        }
        switch (this) {
            case HOURLY:
                return String.format("0 %d * * * ?", minute);
            case DAILY:
                requireHour(hour);
                return String.format("0 %d %d * * ?", minute, hour);
            case WEEKLY:
                requireHour(hour);
                if (dayOfWeek < 1 || dayOfWeek > 7) {
                    throw new IllegalArgumentException("dayOfWeek must be 1-7 (Sun-Sat), was " + dayOfWeek);
                }
                return String.format("0 %d %d ? * %d", minute, hour, dayOfWeek);
            default:
                throw new IllegalStateException("Unhandled frequency: " + this);
        }
    }

    private static void requireHour(final int hour) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("hour must be 0-23, was " + hour);
        }
    }
}
