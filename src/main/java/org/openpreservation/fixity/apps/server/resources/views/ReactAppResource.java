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
package org.openpreservation.fixity.apps.server.resources.views;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Serves the React single page application, which Vite builds into the jar under
 * {@link #RESOURCE_BASE}. A request for a file (anything with an extension) returns that
 * asset; anything else returns index.html so the client-side router can handle the route.
 */
@Path("/app")
public class ReactAppResource {

    private static final String RESOURCE_BASE = "/org/openpreservation/fixity/apps/server/webapp";

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response serveRoot() {
        return Response.ok(readResource("index.html"), MediaType.TEXT_HTML).build();
    }

    @GET
    @Path("/{path:.*}")
    public Response serveReactApp(@PathParam("path") final String path) {
        // Anything with a file extension is an asset request; everything else is a client-side
        // route, which must return index.html so the React router can resolve it.
        if (path.matches(".*\\.[a-zA-Z0-9]+$")) {
            return Response.ok(readResource(path), getContentType(path)).build();
        }
        return Response.ok(readResource("index.html"), MediaType.TEXT_HTML).build();
    }

    /**
     * Read a bundled asset into memory.
     *
     * The bytes are read eagerly and the stream closed here, rather than handing the open
     * stream to JAX-RS, so the descriptor cannot leak on an error path. The assets are a
     * built SPA, so they are small and this costs nothing.
     */
    private byte[] readResource(final String relativePath) {
        // The path segment comes straight off the URL, and @Path("/{path:.*}") matches "..".
        // Without this, a crafted request could walk out of the webapp directory and read any
        // other resource on the classpath, which includes configuration and class files.
        if (relativePath.contains("..")) {
            throw new NotFoundException("Not found: " + relativePath);
        }
        try (InputStream resource = getClass().getResourceAsStream(RESOURCE_BASE + "/" + relativePath)) {
            if (resource == null) {
                throw new NotFoundException("Not found: " + relativePath);
            }
            return resource.readAllBytes();
        } catch (final IOException e) {
            throw new UncheckedIOException("Could not read bundled asset: " + relativePath, e);
        }
    }

    private String getContentType(String path) {
        String lowercase = path.toLowerCase();
        if (lowercase.endsWith(".js")) return "application/javascript";
        if (lowercase.endsWith(".css")) return "text/css";
        if (lowercase.endsWith(".svg")) return "image/svg+xml";
        if (lowercase.endsWith(".png")) return "image/png";
        if (lowercase.endsWith(".jpg") || lowercase.endsWith(".jpeg")) return "image/jpeg";
        if (lowercase.endsWith(".ico")) return "image/x-icon";
        if (lowercase.endsWith(".woff")) return "font/woff";
        if (lowercase.endsWith(".woff2")) return "font/woff2";
        if (lowercase.endsWith(".ttf")) return "font/ttf";
        if (lowercase.endsWith(".json")) return "application/json";
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
