package org.openpreservation.fixity.apps.schedule;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;

public class TestJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException{
        System.out.println("Executing TestJob...");
        Trigger trigger = context.getTrigger();
        System.out.println("Trigger Next Fire time is : " + trigger.getNextFireTime());
    }

}
