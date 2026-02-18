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
