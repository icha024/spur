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

import static com.clianz.spur.internal.HttpMethods.DELETE;
import static com.clianz.spur.internal.HttpMethods.GET;
import static com.clianz.spur.internal.HttpMethods.HEAD;
import static com.clianz.spur.internal.HttpMethods.OPTIONS;
import static com.clianz.spur.internal.HttpMethods.POST;
import static com.clianz.spur.internal.HttpMethods.PUT;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import com.clianz.spur.internal.Endpoint;

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
import io.undertow.util.Headers;
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
        post("/a", String.class, (req, res) -> {
            res.send(req.body());
        });
        delete("/a", (req, res) -> res.send("something gone"));
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
            HttpString requestMethod = exchange.getRequestMethod();

            String requestAccessControlRequestMethods = getHeaderVal(exchange, new HttpString("Access-Control-Request-Method"));
            String requestOrigin = getHeaderVal(exchange, Headers.ORIGIN);
            if (requestMethod.equals(HEAD)) {
                requestMethod = GET;
            } else if (requestMethod.equals(OPTIONS) && methodEndpointMap.containsKey(requestAccessControlRequestMethods)
                    && isValidCORSOrigin(options, requestOrigin)) {
                setCorsHeaders(options, methodEndpointMap, exchange, requestOrigin);
                exchange.endExchange();
                return;
            }

            Endpoint endpoint = methodEndpointMap.get(requestMethod);
            if (endpoint == null) {
                exchange.setStatusCode(405);
                exchange.getResponseHeaders()
                        .put(Headers.ALLOW, getAllowedMethods(methodEndpointMap, options));
                exchange.endExchange();
                return;
            }
            LOGGER.info("Found method: " + endpoint.getMethod());
            if (isValidCORSOrigin(options, requestOrigin)) {
                setCorsHeaders(options, methodEndpointMap, exchange, requestOrigin);
            }
            Req req = new Req(exchange, endpoint.getBodyClassType());
            Res res = new Res(exchange);
            req.parseBody(body -> endpoint.getReqResBiConsumer()
                    .accept(req, res));
        }));

        EncodingHandler gzipEncodingHandler = new EncodingHandler(
                new ContentEncodingRepository().addEncodingHandler("gzip", new GzipEncodingProvider(), 50,
                        Predicates.maxContentSize(options.gzipMaxSize))).setNext(pathTemplateHandler);

        GracefulShutdownHandler gracefulShutdownHandler;
        if (options.gzipEnabled)

        {
            gracefulShutdownHandler = Handlers.gracefulShutdown(gzipEncodingHandler);
        } else

        {
            gracefulShutdownHandler = Handlers.gracefulShutdown(pathTemplateHandler);
        }

        return gracefulShutdownHandler;
    }

    private static String getHeaderVal(HttpServerExchange exchange, HttpString headerName) {
        if (exchange.getRequestHeaders()
                .contains(headerName)) {
            return exchange.getRequestHeaders()
                    .get(headerName)
                    .getFirst();
        }
        return null;
    }

    private static boolean isValidCORSOrigin(SpurOptions options, String requestOrigin) {
        //        if (options.corsHeaders.contains("*") || options.corsHeaders.contains(requestOrigin)) {
        //            return true;
        //        }
        //
        //        if (options.corsHeaders.contains(requestOrigin)) {
        //            return true;
        //        }
        //
        //
        //        if (options.corsHeaders.size() == 0) {
        //            return false;
        //        }
        //
        //        if (requestOrigin == null && !options.corsHeaders.contains("*")) {
        //            return false;
        //        }
        return options.corsHeaders.contains("*") || options.corsHeaders.contains(requestOrigin);
    }

    private static void setCorsHeaders(SpurOptions options, Map<HttpString, Endpoint> methodEndpointMap, HttpServerExchange exchange,
            String requestOrigin) {
        exchange.getResponseHeaders()
                .put(new HttpString("Access-Control-Allow-Methods"), getAllowedMethods(methodEndpointMap, options));
        exchange.getResponseHeaders()
                .put(new HttpString("Access-Control-Allow-Origin"), requestOrigin);
    }

    private static String getAllowedMethods(Map<HttpString, Endpoint> methodEndpointMap, SpurOptions options) {
        StringBuilder methodsAllowed = new StringBuilder();
        Set<HttpString> methodsDefined = new TreeSet<>(methodEndpointMap.keySet());
        if (methodsDefined.contains(GET)) {
            methodsDefined.add(HEAD);
        }
        if (options.corsHeaders.size() > 0) {
            methodsDefined.add(OPTIONS);
        }

        methodsDefined.forEach(httpString -> methodsAllowed.append(", " + httpString));
        return methodsAllowed.toString()
                .substring(2);
    }

    public static Undertow.Builder rawUndertowBuilder() {
        if (serverStarted.get()) {
            throw new IllegalStateException(SERVER_ALREADY_STARTED);
        }
        return builder;
    }

    public static <T> SpurServer get(String path, BiConsumer<Req<T>, Res> reqRes) {
        return setPathHandler(GET, path, reqRes, null);
    }

    public static <T> SpurServer put(String path, Class<T> bodyClass, BiConsumer<Req<T>, Res> reqRes) {
        return setPathHandler(PUT, path, reqRes, bodyClass);
    }

    public static <T> SpurServer post(String path, Class<T> bodyClass, BiConsumer<Req<T>, Res> reqRes) {
        return setPathHandler(POST, path, reqRes, bodyClass);
    }

    public static <T> SpurServer delete(String path, BiConsumer<Req<T>, Res> reqRes) {
        return setPathHandler(DELETE, path, reqRes, null);
    }

    private static <T> SpurServer setPathHandler(HttpString method, String path, BiConsumer<Req<T>, Res> reqRes, Class<T> classType) {
        if (serverStarted.get()) {
            throw new IllegalStateException(SERVER_ALREADY_STARTED);
        }
        endpointsMap.putIfAbsent(path, new HashMap<>());
        endpointsMap.get(path)
                .put(method, new Endpoint(method, path, reqRes, classType));
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
