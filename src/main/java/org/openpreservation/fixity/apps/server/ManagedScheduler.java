package org.openpreservation.fixity.apps.server;

import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Environment;

public class ManagedScheduler extends Application<Configuration> {
    @Override
    public void run(final Configuration configuration, final Environment environment) throws Exception {
        environment.lifecycle().manage(new QuartzManager());
    }
}
