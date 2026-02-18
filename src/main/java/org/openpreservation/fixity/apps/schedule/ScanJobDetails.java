package org.openpreservation.fixity.apps.schedule;

public class ScanJobDetails extends JobDetails.SimpleJobDetails<ScanJob> {
    public final String toScan;
    public final String algorithm;
    public final Class<? extends ScanJob> jobClass = ScanJob.class;

    private ScanJobDetails(final String name, final String group, final String cronExpression, final String toScan, final String algorithm) {
        super(ScanJob.class, name, group, cronExpression);
        this.toScan = toScan;
        this.algorithm = algorithm;
    }

    public static ScanJobDetails of(final String name, final String group, final String cronExpression, final String toScan, final String algorithm) {
        return new ScanJobDetails(name, group, cronExpression, toScan, algorithm);
    }
}
