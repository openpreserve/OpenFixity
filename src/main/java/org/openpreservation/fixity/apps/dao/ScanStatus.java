package org.openpreservation.fixity.apps.dao;

public enum ScanStatus {
    INITIALISED("Initialised"),
    STARTED("Started"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled"),
    INTERRUPTED("Interrupted");

    public final String label;

    private ScanStatus(final String label) {
        this.label = label;
    }
}
