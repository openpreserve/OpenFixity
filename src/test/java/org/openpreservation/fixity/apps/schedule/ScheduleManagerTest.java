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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpreservation.fixity.apps.schedule.JobDetails.SimpleJobDetails;
import org.openpreservation.fixity.apps.schedule.test.TestJob;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

public class ScheduleManagerTest {
    private static final AtomicInteger jobExecutionCount = new AtomicInteger(0);

    @BeforeAll
    public static void setUpScheduler() throws SchedulerException {
        new StdSchedulerFactory().getScheduler();
    }

    @AfterAll
    public static void tearDownScheduler() throws SchedulerException {
        ScheduleManager.shutdown();
    }

    @BeforeEach
    public void ensureSchedulerRunning() throws SchedulerException {
        if (!ScheduleManager.isRunning()) {
            ScheduleManager.resume();
        }
    }

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
}
