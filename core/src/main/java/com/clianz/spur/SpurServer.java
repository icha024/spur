/*
 * Copyright 2017 Ian Chan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.clianz.spur;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.GracefulShutdownHandler;
import io.undertow.server.handlers.PathTemplateHandler;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.util.HttpString;

public class SpurServer {

    private static final Logger LOGGER = Logger.getLogger(SpurServer.class.getName());
    private static final String SERVER_ALREADY_STARTED = "Server already started.";
    private static final SpurServer server = new SpurServer();

    private static SortedSet<Endpoint> endpoints = new TreeSet<>();
    private static AtomicBoolean serverStarted = new AtomicBoolean(false);
    private static Undertow.Builder builder = Undertow.builder();

    public static void main(final String[] args) {
        get("/", (req, res) -> {
            LOGGER.info("Call GET on root");
            // res.send("Hello Again!");
            res.send(new SpurOptions());
        });
        start(SpurOptions.gzipEnabled(true));
    }

    public static void start() {
        start(new SpurOptions());
    }

    public static void start(SpurOptions options) {
        startServer(options);
    }

    private static void startServer(SpurOptions options) {
        if (serverStarted.getAndSet(true)) {
            throw new IllegalStateException(SERVER_ALREADY_STARTED);
        }

        LOGGER.info("Listening to " + options.host + ":" + options.port);
        Undertow server = builder.addHttpListener(options.port, options.host)
                .setHandler(getHandlers(options))
                .setServerOption(UndertowOptions.REQUEST_PARSE_TIMEOUT, options.requestParseTimeOut)
                .setServerOption(UndertowOptions.ENABLE_HTTP2, options.http2Enabled)
                .setServerOption(UndertowOptions.MAX_ENTITY_SIZE, options.maxEntitySize)
                .build();
        server.start();
    }

    private static HttpHandler getHandlers(SpurOptions options) {
        PathTemplateHandler pathTemplateHandler = Handlers.pathTemplate();
        endpoints.forEach(endpoint -> pathTemplateHandler.add(endpoint.getPath(), (AsyncHttpHandler) exchange -> {
            // FIXME: over-wrote multi method on same path.
            if (endpoint.getMethod()
                    .equals(exchange.getRequestMethod())) {
                endpoint.getReqResBiConsumer()
                        .accept(new Req(exchange), new Res(exchange));
            }
        }));

        EncodingHandler gzipEncodingHandler = new EncodingHandler(
                new ContentEncodingRepository().addEncodingHandler("gzip", new GzipEncodingProvider(), 50,
                        Predicates.maxContentSize(options.gzipMaxSize))).setNext(pathTemplateHandler);

        GracefulShutdownHandler gracefulShutdownHandler;
        if (options.gzipEnabled) {
            gracefulShutdownHandler = Handlers.gracefulShutdown(gzipEncodingHandler);
        } else {
            gracefulShutdownHandler = Handlers.gracefulShutdown(pathTemplateHandler);
        }

        return gracefulShutdownHandler;
    }

    public static Undertow.Builder getRawUndertowBuilder() {
        if (serverStarted.get()) {
            throw new IllegalStateException(SERVER_ALREADY_STARTED);
        }
        return builder;
    }

    public static SpurServer get(String path, BiConsumer<Req, Res> reqRes) {
        return setPathHandler("GET", path, reqRes);
    }

    public static SpurServer put(String path, BiConsumer<Req, Res> reqRes) {
        return setPathHandler("PUT", path, reqRes);
    }

    public static SpurServer post(String path, BiConsumer<Req, Res> reqRes) {
        return setPathHandler("POST", path, reqRes);
    }

    public static SpurServer delete(String path, BiConsumer<Req, Res> reqRes) {
        return setPathHandler("DELETE", path, reqRes);
    }

    private static SpurServer setPathHandler(String method, String path, BiConsumer<Req, Res> reqRes) {
        if (serverStarted.get()) {
            throw new IllegalStateException(SERVER_ALREADY_STARTED);
        }
        endpoints.add(new Endpoint(new HttpString(method), path, reqRes));
        return server;
    }

    @FunctionalInterface
    private interface AsyncHttpHandler extends HttpHandler {
        default void handleRequest(HttpServerExchange exchange) throws Exception {
            // non-blocking
            if (exchange.isInIoThread()) {
                // LOGGER.info("Is in IO thread");
                exchange.dispatch(this);
                return;
            }
            // handler code
            // LOGGER.info("STARTING Async");
            asyncBlockingHandler(exchange);
        }

        void asyncBlockingHandler(HttpServerExchange exchange) throws Exception;
    }
}
