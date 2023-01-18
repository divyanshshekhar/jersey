/*
 * Copyright (c) 2013, 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.jersey.jdkhttp;

import java.net.URI;
import java.security.AccessController;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import jakarta.ws.rs.core.UriBuilder;

import com.sun.net.httpserver.HttpServer;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterEach;

/**
 * Abstract JDK HTTP Server unit tester.
 *
 * @author Marek Potociar
 */
public abstract class AbstractJdkHttpServerTester {

    public static final String CONTEXT = "";
    private final int DEFAULT_PORT = 0;

    private static final Logger LOGGER = Logger.getLogger(AbstractJdkHttpServerTester.class.getName());

    /**
     * Get the port to be used for test application deployments.
     *
     * @return The HTTP port of the URI
     */
    protected final int getPort() {
        if (server != null) {
            return server.getAddress().getPort();
        }

        final String value = AccessController.doPrivileged(
                PropertiesHelper.getSystemProperty("jersey.config.test.container.port"));
        if (value != null) {

            try {
                final int i = Integer.parseInt(value);
                if (i < 0) {
                    throw new NumberFormatException("Value is negative.");
                }
                return i;
            } catch (NumberFormatException e) {
                LOGGER.log(Level.CONFIG,
                        "Value of 'jersey.config.test.container.port'"
                                + " property is not a valid non-negative integer [" + value + "]."
                                + " Reverting to default [" + DEFAULT_PORT + "].",
                        e);
            }
        }
        return DEFAULT_PORT;
    }

    private volatile HttpServer server;

    public UriBuilder getUri() {
        return UriBuilder.fromUri("http://localhost").port(getPort()).path(CONTEXT);
    }

    public void startServer(Class... resources) {
        ResourceConfig config = new ResourceConfig(resources);
        startServer(config);
    }

    public void startServer(ResourceConfig config) {
        final URI baseUri = getBaseUri();
        config.register(LoggingFeature.class);
        server = JdkHttpServerFactory.createHttpServer(baseUri, config);
        LOGGER.log(Level.INFO, "jdk-http server started on base uri: " + getBaseUri());
    }

    public HttpServer startServer(final URI uri, final ResourceConfig config,
            final SSLContext sslContext, final boolean start) {
        config.register(LoggingFeature.class);
        server = JdkHttpServerFactory.createHttpServer(uri, config, sslContext, start);
        LOGGER.log(Level.INFO,
                "jdk-http server started on base uri: " + UriBuilder.fromUri(uri).port(getPort()).build());
        return server;
    }

    public URI getBaseUri() {
        return UriBuilder.fromUri("http://localhost/").port(getPort()).build();
    }

    public void stopServer() {
        try {
            server.stop(3);
            server = null;
            LOGGER.log(Level.INFO, "Simple-http server stopped.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    public void tearDown() {
        if (server != null) {
            stopServer();
        }
    }
}
