package org.openpreservation.fixity.apps.dao;

public enum PathScanStatus {
    DAMAGED("<i class=\"bi bi-file-earmark-x\"></i>"),
    DENIED("<i class=\"bi bi-file-earmark-lock2\"></i>"),
    IGNORED("<i class=\"bi bi-file\"></i>"),
    NOTFOUND("<i class=\"bi bi-file-earmark-minus\"></i>"),
    ADDED("<i class=\"bi bi-file-plus\"></i>"),
    CHANGED("<i class=\"bi bi-file-earmark-diff\"></i>"),
    VERIFIED("<i class=\"bi bi-file-check\"></i>");

    private final String symbol;

    private PathScanStatus(final String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return this.symbol;
    }
}
