/*
 * OpenFixity is an application for monitoring and reporting on the fixity of files.
 * Copyright (C) 2026 Open Preservation Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openpreservation.fixity.apps.server.resources.api;

import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.openpreservation.fixity.core.digests.Algorithms;

import jakarta.ws.rs.GET;

@jakarta.ws.rs.Path("/api/digests")
@NullMarked
public class DigestsResource {
    public DigestsResource() {
        super();
    }

    @GET
    @jakarta.ws.rs.Path("/algorithms/")
    public Set<@NonNull Algorithms> getAvailableAlgorithms() {
        return Algorithms.AVAILABLE;
    }

    @GET
    @jakarta.ws.rs.Path("/algorithms/default/")
    public Algorithms getDefaultAlgorithm() {
        return Algorithms.DEFAULT;
    }
}
