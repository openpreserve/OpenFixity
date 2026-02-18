package org.openpreservation.fixity.apps.server.views;

import java.util.List;

import org.quartz.JobKey;

public class JobsView extends FixityAppView {
    private final List<JobKey> jobKeys;
    public JobsView(final List<JobKey> jobKeys) {
        super("jobs.mustache", "jobs");
        this.jobKeys = jobKeys;
    }

    public List<JobKey> getJobKeys() {
        return jobKeys;
    }
}
