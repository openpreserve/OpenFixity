package org.openpreservation.fixity.apps.server;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import io.dropwizard.lifecycle.Managed;

public class QuartzManager implements Managed {
    private final Scheduler scheduler;

    public QuartzManager() {
        try {
            scheduler = new StdSchedulerFactory().getScheduler();
        } catch (final SchedulerException e) {
            throw new IllegalStateException("Failed to initialize scheduler", e);
        }
    }

    @Override
    public void start() throws Exception {
        if (!scheduler.isStarted()) {
            scheduler.start();
        }
    }

    @Override
    public void stop() throws Exception {
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

}
