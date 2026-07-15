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

import org.openpreservation.fixity.apps.dao.ScanSchedule;
import org.openpreservation.fixity.apps.dao.ScanScheduleDAO;
import org.openpreservation.fixity.apps.schedule.ScheduleManager;

import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import io.dropwizard.lifecycle.Managed;

/**
 * On startup, re-registers persisted recurring schedules with Quartz. Quartz uses an in-memory
 * job store, so without this every schedule would be lost on restart. Registered as a managed
 * object after {@link QuartzManager}, so the scheduler is already running when this runs.
 */
public class ScheduleRegistrar implements Managed {

    @Override
    public void start() {
        // Quartz jobs and startup tasks run outside Jersey's per-request session, so bridge to a
        // Hibernate session with the UnitOfWorkAwareProxyFactory, the pattern used by ScanJob.
        final ScanScheduleDAO dao = new ScanScheduleDAO(OpenFixityServer.getSessionFactory());
        final Loader loader = new UnitOfWorkAwareProxyFactory(OpenFixityServer.getHibernate())
                .create(Loader.class, ScanScheduleDAO.class, dao);
        loader.registerEnabled();
    }

    @Override
    public void stop() {
        // Quartz is shut down by QuartzManager; nothing to do here.
    }

    /** The @UnitOfWork target: its public method runs inside a Hibernate session via the proxy. */
    public static class Loader {
        private final ScanScheduleDAO dao;

        public Loader(final ScanScheduleDAO dao) {
            this.dao = dao;
        }

        @UnitOfWork
        public void registerEnabled() {
            int registered = 0;
            for (final ScanSchedule schedule : dao.findEnabled()) {
                try {
                    ScheduleManager.scheduleRecurringScan(
                            schedule.getJobName(),
                            schedule.getCollectionPath().getFullPath(),
                            schedule.getAlgorithm().getName(),
                            schedule.toCron());
                    registered++;
                } catch (final Exception e) {
                    // One bad schedule should not stop the rest from loading.
                    System.err.println("Could not re-register schedule " + schedule.getId() + ": " + e);
                }
            }
            System.out.println("Re-registered " + registered + " scan schedule(s) with the scheduler.");
        }
    }
}
