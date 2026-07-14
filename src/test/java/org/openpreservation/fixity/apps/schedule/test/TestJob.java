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
package org.openpreservation.fixity.apps.schedule.test;

import org.openpreservation.fixity.apps.schedule.ScheduleManagerTest;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;

public class TestJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException{
        System.out.println("Executing TestJob...");
        Trigger trigger = context.getTrigger();
        ScheduleManagerTest.incrementCounter();
        System.out.println("Trigger Next Fire time is : " + trigger.getNextFireTime());
    }

}
