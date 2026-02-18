package org.openpreservation.fixity.apps.schedule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.openpreservation.fixity.apps.schedule.JobDetails.SimpleJobDetails;
import org.openpreservation.fixity.apps.schedule.test.TestJob;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;

public class ScheduleManagerTest {
    private static final AtomicInteger jobExecutionCount = new AtomicInteger(0);

    public static int incrementCounter() {
        return jobExecutionCount.incrementAndGet();
    }

    @Test
    public void testPause() throws SchedulerException {
        ScheduleManager.start();
        assertTrue(ScheduleManager.isRunning());
        ScheduleManager.pause();
        assertFalse(ScheduleManager.isRunning());
    }

    @Test
    public void testResume() throws SchedulerException {
        ScheduleManager.start();
        assertTrue(ScheduleManager.isRunning());
        ScheduleManager.pause();
        assertFalse(ScheduleManager.isRunning());
        ScheduleManager.resume();
        assertTrue(ScheduleManager.isRunning());
    }

    @Test
    public void testScheduleJob() throws SchedulerException, InterruptedException {
        ScheduleManager.start();
        jobExecutionCount.set(0);
        JobDetails.SimpleJobDetails<@NonNull TestJob> jobDetails = SimpleJobDetails.of(TestJob.class, "TestJob", "TestGroup", "0/1 * * * * ?");
        JobDetail jobDetail = ScheduleManager.scheduleJob(jobDetails);
        TimeUnit.SECONDS.sleep(3); // Wait for a few seconds to allow the job to execute
        assertTrue(jobExecutionCount.get() > 2);
        System.out.println("Job executed " + jobExecutionCount.get() + " times.");
        assertTrue(ScheduleManager.getScheduledJobKeys().stream().map(JobKey::getName).collect(Collectors.toList()).contains("TestJob"));
        ScheduleManager.deleteJob(jobDetail.getKey());
    }

   // @Test
    // public void testScheduledScan() throws SchedulerException, InterruptedException {
    //     ScheduleManager.start();
    //     ScanJobDetails scanJobDetails = ScanJobDetails.of("TestScan", "ScanGroup", "0/1 * * * * ?", "test/path", "SHA-256");
    //     final JobDetail jobDetail = ScheduleManager.scheduleScan(scanJobDetails);
    //     TimeUnit.SECONDS.sleep(1); // Wait for a few seconds to allow the job to execute
    //     System.out.println("Job executed " + jobExecutionCount.get() + " times.");
    //     assertTrue(ScheduleManager.getScheduledJobKeys().stream().map(JobKey::getName).collect(Collectors.toList()).contains("TestScan"));
    //     ScheduleManager.deleteJob(jobDetail.getKey());
    // }
}
