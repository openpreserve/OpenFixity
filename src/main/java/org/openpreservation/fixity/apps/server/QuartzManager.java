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
