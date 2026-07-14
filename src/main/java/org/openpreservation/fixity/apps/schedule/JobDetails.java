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

import org.quartz.Job;

public interface JobDetails<J extends Job> {
    String getName();
    String getGroup();
    String getCronExpression();
    Class<? extends Job> getJobClass();

    static class SimpleJobDetails<J extends Job> implements JobDetails<J> {
        private final String name;
        private final String group;
        private final String cronExpression;
        private final Class<? extends Job> jobClass;

        protected SimpleJobDetails(final Class<? extends Job> jobClass, final String name, final String group, final String cronExpression) {
            super();
            this.jobClass = jobClass;
            this.name = name;
            this.group = group;
            this.cronExpression = cronExpression;
        }

        public static <J extends Job> SimpleJobDetails<J> of(final Class<J> jobClass, final String name, final String group, final String cronExpression) {
            return new SimpleJobDetails<>(jobClass, name, group, cronExpression);
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getGroup() {
            return this.group;
        }

        @Override
        public String getCronExpression() {
            return this.cronExpression;
        }

        @Override
        public Class<? extends Job> getJobClass() {
            return (Class<? extends Job>) this.jobClass;
        }
    }
}
