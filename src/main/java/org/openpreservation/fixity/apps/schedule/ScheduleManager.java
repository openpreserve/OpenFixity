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

import java.util.ArrayList;
import java.util.List;

import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

public class ScheduleManager {
    private static Scheduler scheduler = null;

    static {
        try {
            scheduler = new StdSchedulerFactory()
                    .getAllSchedulers()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> 
                            new IllegalStateException("Failed to initialize scheduler: No schedulers found"));
        } catch (final SchedulerException e) {
            throw new IllegalStateException("Failed to initialize scheduler", e);
        }
    }

    public static void start() throws SchedulerException {
        if (!scheduler.isStarted()) {
            scheduler.start();
        }
    }
    public static boolean isRunning() throws SchedulerException {
        return scheduler.isStarted() && !scheduler.isInStandbyMode();
    }
    public static boolean isPaused() throws SchedulerException {
        return scheduler.isInStandbyMode();
    }
    
    public static void shutdown() throws SchedulerException {
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
    public static void pause() throws SchedulerException {
        if (!scheduler.isInStandbyMode()) {
            scheduler.standby();
        }
    }
    public static void resume() throws SchedulerException {
        if (scheduler.isInStandbyMode()) {
            scheduler.start();
        }
    }
    public static JobDetail scheduleScan(final ScanJobDetails scanJobDetails) throws SchedulerException {
        // Create job detail
        JobDetail jobDetail = JobBuilder.newJob(scanJobDetails.jobClass)
                .withIdentity(scanJobDetails.getName(), scanJobDetails.getGroup())
                .usingJobData("toScan", scanJobDetails.toScan)
                .usingJobData("algorithm", scanJobDetails.algorithm)
                .build();

        // Idempotent scheduling: drop any job already under this identity first, so a repeat
        // scan (or a job left behind by a run that was killed before its trigger fired) does
        // not fail with ObjectAlreadyExistsException. Recurring registration relies on this too.
        scheduler.deleteJob(jobDetail.getKey());
        scheduler.scheduleJob(jobDetail, parseTrigger(scanJobDetails));
        return jobDetail;
    }

    private static Trigger parseTrigger(final ScanJobDetails scanJobDetails) {
        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger()
                .withIdentity(scanJobDetails.getName() + "Trigger", scanJobDetails.getGroup());
        if ((scanJobDetails.getCronExpression() != null) && !scanJobDetails.getCronExpression().isBlank()) {
            return triggerBuilder.withSchedule(validateCronExpression(scanJobDetails.getCronExpression()))
                                 .build();
        }
        return triggerBuilder.startNow()
                             .build();
    }

    public static JobDetail scheduleJob(final JobDetails<? extends Job> jobDetails) throws SchedulerException {
        // Create job detail
        JobDetail jobDetail = JobBuilder.newJob(jobDetails.getJobClass())
                .withIdentity(jobDetails.getName(), jobDetails.getGroup())
                .build();

        // Create trigger
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobDetails.getName() + "Trigger", jobDetails.getGroup())
                .withSchedule(validateCronExpression(jobDetails.getCronExpression()))
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
        return jobDetail;
    }

    public static void deleteJob(final JobKey jobKey) throws SchedulerException {
        scheduler.deleteJob(jobKey);
    }

    /** Quartz group for persisted recurring schedules, kept separate from one-off scan jobs. */
    public static final String SCHEDULE_GROUP = "scheduled";

    /**
     * Register a persisted recurring scan with Quartz. Replaces any existing job of the same
     * name first, so re-registering an updated schedule does not fail with "job already exists".
     *
     * @param jobName   a stable name unique to the schedule (see ScanSchedule.getJobName)
     * @param toScan    the absolute path to scan
     * @param algorithm the digest algorithm name (e.g. "SHA-256")
     * @param cron      the Quartz cron expression
     */
    public static JobDetail scheduleRecurringScan(final String jobName, final String toScan,
            final String algorithm, final String cron) throws SchedulerException {
        unscheduleQuietly(jobName);
        return scheduleScan(ScanJobDetails.of(jobName, SCHEDULE_GROUP, cron, toScan, algorithm));
    }

    /** Remove a recurring schedule's job if present; never throws, for use on delete and re-register. */
    public static void unscheduleQuietly(final String jobName) {
        try {
            scheduler.deleteJob(JobKey.jobKey(jobName, SCHEDULE_GROUP));
        } catch (final SchedulerException ignored) {
            // best-effort: nothing to remove, or the scheduler is unavailable
        }
    }

    public static List<JobKey> getScheduledJobKeys() throws SchedulerException {
        List<JobKey> retVal = new ArrayList<>();
        for (String groupName : scheduler.getJobGroupNames()) {
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                retVal.add(jobKey);
            }
        }
        return retVal;
    }

    public static List<JobDetail> getScheduledJobDetails() throws SchedulerException {
        List<JobDetail> retVal = new ArrayList<>();
        for (String groupName : scheduler.getJobGroupNames()) {
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                if (jobDetail != null) {
                    retVal.add(jobDetail);
                }
            }
        }
        return retVal;
    }

    public static CronScheduleBuilder validateCronExpression(final String cronExpression) {
        try {
            return CronScheduleBuilder.cronSchedule(cronExpression);
        } catch (final RuntimeException e) {
            throw new IllegalArgumentException("Invalid cron expression: " + cronExpression, e);
        }
    }
}
