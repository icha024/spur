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

import java.util.HashMap;
import java.util.Map;
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

    private static Map<String, Map<HttpString, Endpoint>> endpointsMap = new HashMap<>();
    private static AtomicBoolean serverStarted = new AtomicBoolean(false);
    private static Undertow.Builder builder = Undertow.builder();

    public static void main(final String[] args) {
        get("/", (req, res) -> {
            LOGGER.info("Call GET on root");
            // res.send("Hello Again!");
            res.send(new SpurOptions());
        });
        post("/a", (req, res) -> res.send(new SpurOptions()), SpurOptions.class);
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
                .setServerOption(UndertowOptions.REQUEST_PARSE_TIMEOUT, options.requestParseTimeOut)
                .setServerOption(UndertowOptions.ENABLE_HTTP2, options.http2Enabled)
                .setServerOption(UndertowOptions.MAX_ENTITY_SIZE, options.maxEntitySize)
                .setHandler(getHandlers(options))
                .build();
        server.start();
    }

    private static HttpHandler getHandlers(SpurOptions options) {
        PathTemplateHandler pathTemplateHandler = Handlers.pathTemplate();
        endpointsMap.forEach((path, methodEndpointMap) -> pathTemplateHandler.add(path, (AsyncHttpHandler) exchange -> {
            Endpoint endpoint = methodEndpointMap.get(exchange.getRequestMethod());
            LOGGER.info("Found method: " + endpoint.getMethod());
            if (endpoint != null) {
                Req req = new Req(exchange, endpoint.getBodyClassType());
                Res res = new Res(exchange);
                req.parseBody(body -> endpoint.getReqResBiConsumer()
                        .accept(req, res));
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
        return setPathHandler("GET", path, reqRes, null);
    }

    public static <T> SpurServer put(String path, BiConsumer<Req, Res> reqRes, Class<T> bodyClass) {
        return setPathHandler("PUT", path, reqRes, bodyClass);
    }

    public static <T> SpurServer post(String path, BiConsumer<Req, Res> reqRes, Class<T> bodyClass) {
        return setPathHandler("POST", path, reqRes, bodyClass);
    }

    public static SpurServer delete(String path, BiConsumer<Req, Res> reqRes) {
        return setPathHandler("DELETE", path, reqRes, null);
    }

    private static <T> SpurServer setPathHandler(String method, String path, BiConsumer<Req, Res> reqRes, Class<T> classType) {
        if (serverStarted.get()) {
            throw new IllegalStateException(SERVER_ALREADY_STARTED);
        }
        endpointsMap.putIfAbsent(path, new HashMap<>());
        endpointsMap.get(path)
                .put(new HttpString(method), new Endpoint(new HttpString(method), path, reqRes, classType));
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
