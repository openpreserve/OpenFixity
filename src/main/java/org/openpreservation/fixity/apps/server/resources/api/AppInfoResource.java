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

import java.lang.management.ManagementFactory;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Reports application and runtime information for the frontend's About view.
 */
@Path("/api/info")
@Produces(MediaType.APPLICATION_JSON)
public class AppInfoResource {

    /** Serialized directly to JSON; field names are the API contract the frontend reads. */
    public static final class AppInfo {
        public final String appName = "OpenFixity";
        // Read from the jar manifest (Implementation-Version), populated from the POM version by
        // the jar plugin, so it cannot drift from the build. Falls back when run from classes.
        public final String version = versionOrDefault();
        public final String javaVersion = System.getProperty("java.version");
        public final String javaVendor = System.getProperty("java.vendor");
        public final String osName = System.getProperty("os.name");
        public final String osArch = System.getProperty("os.arch");
        public final String osVersion = System.getProperty("os.version");
        // Part of the frontend's AppInfo contract. There is no runtime API for it, so it tracks
        // the ${dropwizard.version} property in the POM and must be updated alongside it.
        public final String dropwizardVersion = "5.0.1";
        public final long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();

        private static String versionOrDefault() {
            final String v = AppInfoResource.class.getPackage().getImplementationVersion();
            return (v != null) ? v : "dev";
        }
    }

    @GET
    public AppInfo getAppInfo() {
        return new AppInfo();
    }
}
