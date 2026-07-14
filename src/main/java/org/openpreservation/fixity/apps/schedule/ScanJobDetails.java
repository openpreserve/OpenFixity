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
package org.openpreservation.fixity.apps.schedule;

public class ScanJobDetails extends JobDetails.SimpleJobDetails<ScanJob> {
    public final String toScan;
    public final String algorithm;
    public final Class<? extends ScanJob> jobClass = ScanJob.class;

    private ScanJobDetails(final String name, final String group, final String cronExpression, final String toScan, final String algorithm) {
        super(ScanJob.class, name, group, cronExpression);
        this.toScan = toScan;
        this.algorithm = algorithm;
    }

    public static ScanJobDetails of(final String name, final String group, final String cronExpression, final String toScan, final String algorithm) {
        return new ScanJobDetails(name, group, cronExpression, toScan, algorithm);
    }
}
