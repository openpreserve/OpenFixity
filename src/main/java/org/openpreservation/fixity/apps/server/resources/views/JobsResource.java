package org.openpreservation.fixity.apps.server.resources.views;

import org.openpreservation.fixity.apps.schedule.ScheduleManager;
import org.openpreservation.fixity.apps.server.views.JobsView;
import org.quartz.SchedulerException;

import io.dropwizard.hibernate.UnitOfWork;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Path;

@Path("/jobs")
public class JobsResource {
    public JobsResource() {
        super();
    }

    @UnitOfWork
    @GET
    public JobsView getSchedules() {
        try {
            return new JobsView(ScheduleManager.getScheduledJobKeys());
        } catch (SchedulerException e) {
            throw new InternalServerErrorException("Failed to schedule job: " + e.getMessage(), e);
        }
    }

}
