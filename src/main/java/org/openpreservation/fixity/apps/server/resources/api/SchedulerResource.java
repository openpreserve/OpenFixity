package org.openpreservation.fixity.apps.server.resources.api;

import org.openpreservation.fixity.apps.schedule.ScheduleManager;
import org.quartz.SchedulerException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;

@jakarta.ws.rs.Path("/api")
public class SchedulerResource {

    @GET
    @jakarta.ws.rs.Path("/scheduler/isRunning")
    public boolean isSchedulerRunning() {
        try {
            return ScheduleManager.isRunning();
        } catch (SchedulerException e) {
            throw new InternalServerErrorException("Failed to check if scheduler is running: " + e.getMessage(), e);
        }
    }

    @GET
    @jakarta.ws.rs.Path("/scheduler/isPaused")
    public boolean isSchedulerPaused() {
        try {
            return ScheduleManager.isPaused();
        } catch (SchedulerException e) {
            throw new InternalServerErrorException("Failed to check if scheduler is paused: " + e.getMessage(), e);
        }
    }

    @POST
    @jakarta.ws.rs.Path("/scheduler/resume")
    public boolean resumeScheduler() {
        try {
            if (!ScheduleManager.isRunning()) {
                ScheduleManager.resume();
            }
            return false;
        } catch (SchedulerException e) {
            throw new InternalServerErrorException("Failed to start scheduler: " + e.getMessage(), e);
        }
    }

    @POST
    @jakarta.ws.rs.Path("/scheduler/pause")
    public boolean pauseScheduler() {
        try {
            if (ScheduleManager.isRunning()) {
                ScheduleManager.pause();
            }
            return true;
        } catch (SchedulerException e) {
            throw new InternalServerErrorException("Failed to pause scheduler: " + e.getMessage(), e);
        }
    }
}
