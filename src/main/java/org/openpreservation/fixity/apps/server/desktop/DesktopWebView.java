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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

/**
 * The native application window — a JavaFX {@link WebView} that renders the local
 * {@code /app} UI. JavaFX bundles its own web engine, so this needs no OS webview runtime
 * (WebView2 / WKWebView / WebKitGTK) installed on the user's machine.
 *
 * <p>Launched via the "launcher trick": {@link #show(String)} is called from
 * {@link OpenFixityDesktop} (which does <em>not</em> extend {@link Application}), so JavaFX
 * starts cleanly from the classpath without the "JavaFX runtime components are missing"
 * error. {@link #show(String)} blocks until the window is closed.
 */
public class DesktopWebView extends Application {

    // PNG, not SVG: JavaFX's Image cannot decode SVG, so an SVG icon silently fails to load and
    // the window falls back to the toolkit's default (a generic gear). Several sizes are offered
    // so the window manager can pick the sharpest for the title bar, taskbar and Alt-Tab.
    private static final String ICON_BASE = "/org/openpreservation/fixity/apps/server/desktop/icons/icon-";
    private static final int[] ICON_SIZES = { 16, 32, 128, 256, 512 };

    /**
     * Show the window and block until it is closed. Throws (e.g. {@link NoClassDefFoundError})
     * if JavaFX is not on the classpath, which the caller treats as "fall back to a browser".
     */
    public static void show(final String url) {
        OpenFixityDesktop.appUrl = url;
        Application.launch(DesktopWebView.class);
    }

    @Override
    public void start(final Stage stage) {
        final WebView webView = new WebView();
        webView.getEngine().load(OpenFixityDesktop.appUrl);

        // Cap the initial (restore) size to the screen's work area. A fixed size larger than the
        // display — common on smaller or scaled screens — centres so that the title bar sits above
        // the top of the screen, leaving users unable to grab, move or resize the window.
        final Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        final double width = Math.min(1280, screen.getWidth());
        final double height = Math.min(860, screen.getHeight());

        final Scene scene = new Scene(webView, width, height);
        stage.setTitle("OpenFixity");
        stage.setScene(scene);
        stage.setMinWidth(Math.min(800, screen.getWidth()));
        stage.setMinHeight(Math.min(600, screen.getHeight()));
        loadIcon(stage);
        stage.centerOnScreen();
        // Open maximised so the title bar is always on-screen and reachable. The capped size above
        // is what the window restores to when un-maximised. Not full screen, which hides the bar.
        stage.setMaximized(true);
        stage.show();
    }

    @Override
    public void stop() {
        // Window closed: stop JavaFX and exit so Dropwizard's shutdown hook stops the server.
        Platform.exit();
        System.exit(0);
    }

    private void loadIcon(final Stage stage) {
        for (final int size : ICON_SIZES) {
            try (InputStream icon = DesktopWebView.class.getResourceAsStream(ICON_BASE + size + ".png")) {
                if (icon != null) {
                    stage.getIcons().add(new Image(icon));
                }
            } catch (final IOException | RuntimeException ignored) {
                // an icon is optional; skip a size that cannot be read rather than failing to open
            }
        }
    }
}
