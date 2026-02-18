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
