package org.openpreservation.fixity.apps.server.resources.api;

import java.util.Set;

import org.openpreservation.fixity.core.digests.Algorithms;

import jakarta.ws.rs.GET;

@jakarta.ws.rs.Path("/api/digests")
public class DigestsResource {
    public DigestsResource() {
        super();
    }

    @GET
    @jakarta.ws.rs.Path("/algorithms/")
    public Set<Algorithms> getAvailableAlgorithms() {
        return Algorithms.AVAILABLE;
    }

    @GET
    @jakarta.ws.rs.Path("/algorithms/default/")
    public Algorithms getDefaultAlgorithm() {
        return Algorithms.DEFAULT;
    }
}
