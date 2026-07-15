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

package org.openpreservation.fixity.apps.server.desktop;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.openpreservation.fixity.apps.server.OpenFixityServer;

/**
 * Desktop entry point for OpenFixity.
 *
 * <p>Unlike {@link OpenFixityServer} (which expects {@code server <config.yml>} on the
 * command line), this launcher takes <strong>no arguments</strong>: it self-configures,
 * picks free loopback ports, starts the Dropwizard server in the background, waits until it
 * is ready, and then shows the {@code /app} UI in a native window (falling back to the
 * system browser if no window engine is available). Closing the window stops the server.
 *
 * <p>This is the main class used by the packaged desktop installers (jpackage). The plain
 * fat JAR still launches the server via {@code OpenFixityServer} for headless/server use.
 */
public final class OpenFixityDesktop {

    private static final String CONFIG_TEMPLATE =
            "/org/openpreservation/fixity/apps/server/desktop-server.yml.template";
    private static final int READY_TIMEOUT_SECONDS = 90;

    /** The local URL of the running UI; read by the window class. */
    // Package-private: only DesktopWebView, in this package, reads it. Volatile because the
    // launcher thread writes it and the JavaFX thread reads it.
    static volatile String appUrl;

    private OpenFixityDesktop() {
    }

    public static void main(final String[] args) throws Exception {
        final int appPort = freePort();
        final int adminPort = freePort();
        appUrl = "http://127.0.0.1:" + appPort + "/app/";

        final Path dataDir = resolveDataDir();
        Files.createDirectories(dataDir.resolve("logs"));
        final Path config = writeConfig(appPort, adminPort, dataDir);

        startServer(config);

        if (!waitForReady(appUrl, READY_TIMEOUT_SECONDS)) {
            System.err.println("OpenFixity server did not become ready within "
                    + READY_TIMEOUT_SECONDS + "s; opening the UI anyway.");
        }

        // Prefer a native window; fall back to the system browser if no engine is available.
        try {
            DesktopWebView.show(appUrl);
        } catch (final Throwable windowFailure) {
            System.err.println("Native window unavailable (" + windowFailure
                    + "); opening the system browser instead.");
            openBrowser(appUrl);
            // Jetty's non-daemon threads keep the JVM (and the server) alive.
        }
    }

    /** Start the Dropwizard server on a background thread (run() blocks on join()). */
    private static void startServer(final Path config) {
        final Thread server = new Thread(() -> {
            try {
                new OpenFixityServer().run("server", config.toString());
            } catch (final Exception e) {
                System.err.println("OpenFixity server failed to start: " + e);
                e.printStackTrace();
                System.exit(2);
            }
        }, "openfixity-server");
        server.setDaemon(false);
        server.start();
    }

    /**
     * The per-user data directory (H2 database, logs, generated config). Defaults to
     * {@code ~/.openfixity} but can be relocated via the {@code OPENFIXITY_HOME} environment
     * variable or {@code -Dopenfixity.home=...} (useful for testing or portable installs).
     */
    private static Path resolveDataDir() {
        final String override = firstNonBlank(
                System.getProperty("openfixity.home"), System.getenv("OPENFIXITY_HOME"));
        if (override != null) {
            return Paths.get(override);
        }
        return Paths.get(System.getProperty("user.home"), ".openfixity");
    }

    private static String firstNonBlank(final String... values) {
        for (final String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    /** Reserve an OS-assigned free port (closed immediately so the server can bind it). */
    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private static Path writeConfig(final int appPort, final int adminPort, final Path dataDir)
            throws IOException {
        final String template;
        try (InputStream in = OpenFixityDesktop.class.getResourceAsStream(CONFIG_TEMPLATE)) {
            if (in == null) {
                throw new IOException("Missing desktop config template: " + CONFIG_TEMPLATE);
            }
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        final String logFile = dataDir.resolve("logs").resolve("open-fixity.log")
                .toString().replace('\\', '/');
        // Absolute H2 path under the data dir. AUTO_SERVER=TRUE lets a second launch (or a
        // separately-running server) share the database instead of failing on a file lock.
        final String dbPath = dataDir.resolve("open-fixity").toString().replace('\\', '/');
        final String dbUrl = "jdbc:h2:" + dbPath + ";AUTO_SERVER=TRUE";
        final String yaml = template
                .replace("__APP_PORT__", Integer.toString(appPort))
                .replace("__ADMIN_PORT__", Integer.toString(adminPort))
                .replace("__DB_URL__", dbUrl)
                .replace("__LOG_FILE__", logFile);
        final Path config = dataDir.resolve("desktop-server.yml");
        Files.writeString(config, yaml, StandardCharsets.UTF_8);
        return config;
    }

    /** Poll the UI URL until the server responds (any HTTP status) or the timeout elapses. */
    private static boolean waitForReady(final String url, final int seconds) {
        final long deadline = System.nanoTime() + seconds * 1_000_000_000L;
        while (System.nanoTime() < deadline) {
            try {
                final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);
                conn.setRequestMethod("GET");
                final int code = conn.getResponseCode();
                conn.disconnect();
                if (code >= 200 && code < 500) {
                    return true;
                }
            } catch (final IOException notUpYet) {
                // server not accepting connections yet
            }
            try {
                Thread.sleep(300);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private static void openBrowser(final String url) {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (final Exception ignored) {
            // fall through to printing the URL
        }
        System.out.println("OpenFixity is running. Open your browser at: " + url);
    }
}
