/**
 * 
 */
package org.openpreservation.fixity.apps.server.views;


import org.openpreservation.fixity.apps.server.exceptions.RFC7807Details;

/**
 * @author cfw
 *
 */
public class ErrorView extends FixityAppView {
    private final RFC7807Details errorDetails;

    public ErrorView(final RFC7807Details message) {
        super("error.mustache", "none");
        this.errorDetails = message;
    }

    public RFC7807Details getErrorDetails() {
        return errorDetails;
    }
}
