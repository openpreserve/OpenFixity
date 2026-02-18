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
            scheduler = new StdSchedulerFactory().getScheduler();
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

        scheduler.scheduleJob(jobDetail, parseTrigger(scanJobDetails));
        scheduler.start();
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
        scheduler.start();
        return jobDetail;
    }

    public static void deleteJob(final JobKey jobKey) throws SchedulerException {
        scheduler.deleteJob(jobKey);
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
