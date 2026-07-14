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

import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.hibernate.SessionFactory;
import org.jspecify.annotations.NonNull;
import org.openpreservation.fixity.apps.dao.Collection;
import org.openpreservation.fixity.apps.dao.CollectionPath;
import org.openpreservation.fixity.apps.dao.DataFactory;
import org.openpreservation.fixity.apps.dao.DigestRecord;
import org.openpreservation.fixity.apps.dao.FileScanRecord;
import org.openpreservation.fixity.apps.dao.FolderScanRecord;
import org.openpreservation.fixity.apps.dao.PathRegistration;
import org.openpreservation.fixity.apps.dao.PathScan;
import org.openpreservation.fixity.apps.dao.PathSummaryRecord;
import org.openpreservation.fixity.apps.schedule.JobDetailSerializer;
import org.openpreservation.fixity.apps.server.config.OpenFixityConfiguration;
import org.openpreservation.fixity.apps.server.exceptions.OpenFixityExceptionMapper;
import org.openpreservation.fixity.apps.server.exceptions.RFC7807Details;
import org.openpreservation.fixity.apps.server.exceptions.WebApplicationExceptionMapper;
import org.openpreservation.fixity.apps.server.resources.api.SchedulerResource;
import org.openpreservation.fixity.apps.server.resources.views.IndexResource;
import org.openpreservation.fixity.apps.server.resources.views.JobsResource;
import org.openpreservation.fixity.apps.server.resources.views.PathScansResource;
import org.openpreservation.fixity.apps.server.views.ErrorView;
import org.openpreservation.fixity.apps.server.views.FixityAppView;
import org.quartz.JobDetail;

import com.fasterxml.jackson.databind.module.SimpleModule;

import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.jersey.errors.ErrorEntityWriter;
import io.dropwizard.views.common.ViewBundle;
import jakarta.ws.rs.core.MediaType;

/**
 * @author <a href="mailto:carl@openpreservation.org">Carl Wilson</a>
 *         <a href="https://github.com/carlwilson">carlwilson AT github</a>
 *
 * @version 0.1
 *
 *          Created 2 Aug 2018:16:20:27
 */

public class OpenFixityServer extends Application<OpenFixityConfiguration> {
    private static final String NAME = "open-fixity-rest"; //$NON-NLS-1$
    private static OpenFixityConfiguration configuration;
    // Published so Quartz jobs, which run outside Jersey's injection context, can reach the
    // Hibernate bundle via getHibernate(). Nothing outside this class reads them directly.
    private static DataFactory dataFactory;
    private static OpenFixityServer application;
    private final HibernateBundle<OpenFixityConfiguration> hibernate =
        new HibernateBundle<OpenFixityConfiguration>(Collection.class,
                                                     CollectionPath.class,
                                                     DigestRecord.class,
                                                     FileScanRecord.class,
                                                     FolderScanRecord.class,
                                                     PathRegistration.class,
                                                     PathScan.class,
                                                     PathSummaryRecord.class)
        {
            @Override
            public DataSourceFactory getDataSourceFactory(OpenFixityConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        };
    /**
     * Main method for Jetty server application. Simply calls the run method with
     * command line args.
     *
     * @param args command line arguments as string array.
     * @throws Exception passes any exception thrown by run
     */
    public static void main(String[] args) throws Exception {
        new OpenFixityServer().run(args);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void initialize(Bootstrap<OpenFixityConfiguration> bootstrap) {
        bootstrap.addBundle(new MultiPartBundle());
        // Dropwizard assets bundle to map static resources
        bootstrap.addBundle(new AssetsBundle("/org/openpreservation/fixity/apps/server/assets/css", "/css", null, "css")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        bootstrap.addBundle(new AssetsBundle("/org/openpreservation/fixity/apps/server/assets/js", "/js", null, "js")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        bootstrap.addBundle(new AssetsBundle("/org/openpreservation/fixity/apps/server/assets/img", "/img", null, "img")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        bootstrap.addBundle(hibernate);
        bootstrap.addBundle(new ViewBundle<>());
    }

    @Override
    public void run(OpenFixityConfiguration configuration, Environment environment) {
        // Add health checks
        // Set up servlets session handler
        application = this;
        environment.servlets().setSessionHandler(new SessionHandler());
        setConfiguration(configuration);
        registerResources(environment);
        environment.getObjectMapper().registerModule(new SimpleModule() {
            {
                addSerializer(JobDetail.class, new JobDetailSerializer());
            }
        });
    }

    @SuppressWarnings("null")
    public void registerResources(Environment environment) {
        dataFactory = new DataFactory(hibernate.getSessionFactory());
        // Setup exception mapping to integrate ESafe exceptions with appropriate HTTP
        // codes
        environment.jersey().register(new ErrorEntityWriter<RFC7807Details, @NonNull FixityAppView>(MediaType.TEXT_HTML_TYPE, FixityAppView.class) {
            @Override
            protected @NonNull FixityAppView getRepresentation(RFC7807Details message) {
                return new ErrorView(message);
            }
        });
        environment.lifecycle().manage(new QuartzManager());
        environment.jersey().register(new OpenFixityExceptionMapper());
        environment.jersey().register(new WebApplicationExceptionMapper());
        environment.jersey().register(new IndexResource());
        // Serves the React single page app from the jar at /app. The Mustache views remain
        // the default UI at /; the two run side by side while the React app is brought up.
        environment.jersey().register(new org.openpreservation.fixity.apps.server.resources.views.ReactAppResource());
        environment.jersey().register(new org.openpreservation.fixity.apps.server.resources.api.CollectionsResource(dataFactory));
        environment.jersey().register(new org.openpreservation.fixity.apps.server.resources.api.PathsResource(dataFactory));
        environment.jersey().register(org.openpreservation.fixity.apps.server.resources.api.DigestsResource.class);
        environment.jersey().register( org.openpreservation.fixity.apps.server.resources.api.FolderInfoResource.class);
        environment.jersey().register(new org.openpreservation.fixity.apps.server.resources.views.CollectionsResource(dataFactory));
        environment.jersey().register(org.openpreservation.fixity.apps.server.resources.views.FoldersResource.class);
        environment.jersey().register(new org.openpreservation.fixity.apps.server.resources.views.PathsResource(dataFactory));
        environment.jersey().register(new PathScansResource(dataFactory));
        environment.jersey().register(new JobsResource());
        environment.jersey().register(new SchedulerResource());
        environment.jersey().register(new org.openpreservation.fixity.apps.server.resources.api.ScanResultsResource(dataFactory));
    }

    public static final OpenFixityConfiguration getConfiguration() {
        return configuration;
    }

    public static final synchronized @NonNull HibernateBundle<OpenFixityConfiguration> getHibernate() {
        if (application.hibernate == null) {
            throw new IllegalStateException("Hibernate has not been initialized yet");
        }
        return application.hibernate;
    }

    public static final synchronized @NonNull SessionFactory getSessionFactory() {
        SessionFactory sessionFactory = application.hibernate.getSessionFactory();
        if (sessionFactory == null) {
            throw new IllegalStateException("SessionFactory has not been initialized yet");
        }
        return sessionFactory;
    }
    
    private static final synchronized OpenFixityConfiguration setConfiguration(final OpenFixityConfiguration config) {
        configuration = config;
        return configuration;
    }
}
