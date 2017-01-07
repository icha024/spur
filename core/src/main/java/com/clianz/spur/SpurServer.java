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

import static com.clianz.spur.helpers.HttpMethods.DELETE;
import static com.clianz.spur.helpers.HttpMethods.GET;
import static com.clianz.spur.helpers.HttpMethods.HEAD;
import static com.clianz.spur.helpers.HttpMethods.OPTIONS;
import static com.clianz.spur.helpers.HttpMethods.POST;
import static com.clianz.spur.helpers.HttpMethods.PUT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;

import com.clianz.spur.helpers.Endpoint;
import com.clianz.spur.helpers.WebSocketMessageSender;
import com.clianz.spur.helpers.WebSocketOnConnect;
import com.clianz.spur.helpers.WebSocketOnMessage;

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
import io.undertow.util.StatusCodes;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

public class SpurServer {

    private static final Logger LOGGER = Logger.getLogger(SpurServer.class.getName());
    private static final String SERVER_ALREADY_STARTED = "Server already started.";
    private static final SpurServer server = new SpurServer();
    private static final HttpString ACCESS_CONTROL_REQUEST_METHOD = new HttpString("Access-Control-Request-Method");
    private static final HttpString ACCESS_CONTROL_ALLOW_ORIGIN = new HttpString("Access-Control-Allow-Origin");
    private static final HttpString ACCESS_CONTROL_ALLOW_METHOD = new HttpString("Access-Control-Allow-Methods");

    private static Map<String, Map<HttpString, Endpoint>> endpointsMap = new HashMap<>();
    private static AtomicBoolean serverStarted = new AtomicBoolean(false);
    private static Undertow.Builder builder = Undertow.builder();
    public static SpurOptions spurOptions = new SpurOptions();
    //    private static Set<WebSocketChannel> webSocketChannels = null;
    private static Map<String, Set<WebSocketChannel>> webSocketChannelsMap = new HashMap<>();
    private static Map<String, WebSocketHandler> webSocketHandlerMap = new HashMap<>();

    private SpurServer() {
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
        SpurServer.spurOptions = options;

        LOGGER.info("Listening to " + options.host + ":" + options.port);

        if (options.httpsEnabled) {
            SSLContext sslContext = null;
            try {
                sslContext = options.getSslContext();
            } catch (Exception e) {
                LOGGER.throwing("SpurOptions", "SpurOptions", e);
                throw new RuntimeException("Can not create SSL context from properties: KEYSTORE=" + options.keystorePath + " TRUSTSTORE="
                        + options.truststorePath);
            }
            if (sslContext == null) {
                throw new IllegalArgumentException(("HTTPS/SSL context must be configured when HTTPS is enabled"));
            } else if (options.httpsPort == 0) {
                throw new IllegalArgumentException(("HTTPS port must be configured when HTTPS is enabled"));
            }
            builder = builder.addHttpsListener(options.httpsPort, options.host, sslContext);
            LOGGER.info("HTTPS Enabled");
        }

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
            invokePathTemplateHandler(options, methodEndpointMap, exchange);
        }));

        if (!webSocketHandlerMap.isEmpty()) {
            webSocketHandlerMap.forEach((pathPrefix, webSocketHandler) -> {
                LOGGER.info("Adding WS for path: " + webSocketHandler.getPath());
                addWebSocketHandler(pathTemplateHandler, webSocketHandler);
            });
        }

        EncodingHandler gzipEncodingHandler = new EncodingHandler(
                new ContentEncodingRepository().addEncodingHandler("gzip", new GzipEncodingProvider(), 50,
                        Predicates.maxContentSize(options.gzipMaxSize))).setNext(pathTemplateHandler);

        GracefulShutdownHandler gracefulShutdownHandler;
        if (options.gzipEnabled) {
            gracefulShutdownHandler = Handlers.gracefulShutdown(gzipEncodingHandler);
        } else {
            gracefulShutdownHandler = Handlers.gracefulShutdown(pathTemplateHandler);
        }

        HttpHandler secureRedirectHandler;
        if (options.forceHttps) {
            secureRedirectHandler = Handlers.predicate(Predicates.secure(), gracefulShutdownHandler, new RedirectHttpsHandler());
        } else {
            secureRedirectHandler = gracefulShutdownHandler;
        }

        return secureRedirectHandler;
    }

    private static void addWebSocketHandler(PathTemplateHandler pathTemplateHandler, WebSocketHandler webSocketHandler) {
        // TODO: Add security/auth
        pathTemplateHandler.add(webSocketHandler.getPath(), Handlers.websocket((exchange, channel) -> {
            if (webSocketChannelsMap.get(webSocketHandler.getPath()) == null) {
                webSocketChannelsMap.put(webSocketHandler.getPath(), channel.getPeerConnections());
            }
            //            double randomKey = Math.random();
            //            LOGGER.info("Setting secret: " + randomKey);
            //            channel.setAttribute("myKey", "secret key: " + randomKey);
            //            broadcastToAllWebsockets("A new user connected!");

            WebSocketMessageSender sender = new WebSocketMessageSender(channel);
            webSocketHandler.getWebSocketOnConnect()
                    .onConnect(sender);
            channel.getReceiveSetter()
                    .set(new AbstractReceiveListener() {
                        @Override
                        protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
                            //                            LOGGER.info("Key was: " + channel.getAttribute("myKey"));
                            webSocketHandler.getWebSocketOnMessage()
                                    .onMessage(message.getData(), sender);
                        }
                    });
            channel.resumeReceives();
        }));
    }

    private static void invokePathTemplateHandler(SpurOptions options, Map<HttpString, Endpoint> methodEndpointsMap,
            HttpServerExchange exchange) {

        HttpString requestMethod = exchange.getRequestMethod();
        String requestAccessControlRequestMethod = getRequestHeader(exchange, ACCESS_CONTROL_REQUEST_METHOD);
        String requestOrigin = getRequestHeader(exchange, Headers.ORIGIN);
        if (requestMethod.equals(HEAD)) {
            requestMethod = GET;
        } else if (requestMethod.equals(OPTIONS) && requestAccessControlRequestMethod != null && methodEndpointsMap.containsKey(
                new HttpString(requestAccessControlRequestMethod)) && isValidCorsOrigin(options, requestOrigin)) {
            setCorsOriginHeader(exchange, requestOrigin);
            setCorsMethodHeader(options, methodEndpointsMap, exchange);
            exchange.endExchange();
            return;
        }

        Endpoint endpoint = methodEndpointsMap.get(requestMethod);
        if (endpoint == null) {
            exchange.setStatusCode(StatusCodes.METHOD_NOT_ALLOWED);
            exchange.getResponseHeaders()
                    .put(Headers.ALLOW, getAllowedMethods(methodEndpointsMap, options));
            exchange.endExchange();
            return;
        }

        //        LOGGER.info("Found method: " + endpoint.getMethod());
        if (isValidCorsOrigin(options, requestOrigin)) {
            setCorsOriginHeader(exchange, requestOrigin);
        }
        Req req = new Req(exchange, endpoint.getBodyClassType());
        req.parseBody((newExchange, body) -> endpoint.getReqResBiConsumer()
                .accept(new Req(newExchange, endpoint.getBodyClassType()), new Res(newExchange)));
    }

    private static String getRequestHeader(HttpServerExchange exchange, HttpString headerName) {
        if (exchange.getRequestHeaders()
                .contains(headerName)) {
            return exchange.getRequestHeaders()
                    .get(headerName)
                    .getFirst();
        }
        return null;
    }

    private static boolean isValidCorsOrigin(SpurOptions options, String requestOrigin) {
        return options.corsHeaders.contains("*") || options.corsHeaders.contains(requestOrigin);
    }

    private static void setCorsOriginHeader(HttpServerExchange exchange, String requestOrigin) {
        exchange.getResponseHeaders()
                .put(ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin);
    }

    private static void setCorsMethodHeader(SpurOptions options, Map<HttpString, Endpoint> methodEndpointMap, HttpServerExchange exchange) {
        exchange.getResponseHeaders()
                .put(ACCESS_CONTROL_ALLOW_METHOD, getAllowedMethods(methodEndpointMap, options));
    }

    private static String getAllowedMethods(Map<HttpString, Endpoint> methodEndpointMap, SpurOptions options) {
        StringBuilder methodsAllowed = new StringBuilder();
        Set<HttpString> methodsDefined = new TreeSet<>(methodEndpointMap.keySet());
        if (methodsDefined.contains(GET)) {
            methodsDefined.add(HEAD);
        }
        if (!options.corsHeaders.isEmpty()) {
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

    public static SpurServer websocket(String pathPrefix, WebSocketOnConnect webSocketOnConnect, WebSocketOnMessage webSocketOnMessage) {
        webSocketHandlerMap.put(pathPrefix, new WebSocketHandler(pathPrefix, webSocketOnConnect, webSocketOnMessage));
        return server;
    }

    public static void broadcastWebsockets(String websocketPath, String msg) {
        Set<WebSocketChannel> webSocketChannels = webSocketChannelsMap.get(websocketPath);
        if (webSocketChannels != null) {
            new ArrayList<>(webSocketChannels).forEach(webSocketChannel -> WebSockets.sendText(msg, webSocketChannel, null));
        }
    }

    public static void broadcastWebsockets(String websocketPath, String msg, String channelAttributeKey,
            Predicate<Object> channelAttributeValueTest) {
        Set<WebSocketChannel> webSocketChannels = webSocketChannelsMap.get(websocketPath);
        if (webSocketChannels != null) {
            new ArrayList<>(webSocketChannels).stream()
                    .filter(webSocketChannel -> channelAttributeValueTest.test(webSocketChannel.getAttribute(channelAttributeKey)))
                    .forEach(webSocketChannel -> WebSockets.sendText(msg, webSocketChannel, null));
        }
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
            if (spurOptions.blockableHandlersEnabled && exchange.isInIoThread()) {
                LOGGER.info("Is in IO thread, dispatching for blockableHandlersEnabled...");
                //                exchange.dispatch(ForkJoinPool.commonPool(), this);
                exchange.dispatch(this);
                return;
            }
            // handler code
            // LOGGER.info("STARTING Async");
            asyncBlockingHandler(exchange);
        }

        void asyncBlockingHandler(HttpServerExchange exchange) throws Exception;
    }

    private static class WebSocketHandler {
        String path;
        WebSocketOnConnect webSocketOnConnect;
        WebSocketOnMessage webSocketOnMessage;

        public WebSocketHandler(String path, WebSocketOnConnect webSocketOnConnect, WebSocketOnMessage webSocketOnMessage) {
            this.path = path;
            this.webSocketOnConnect = webSocketOnConnect;
            this.webSocketOnMessage = webSocketOnMessage;
        }

        public String getPath() {
            return path;
        }

        public WebSocketOnConnect getWebSocketOnConnect() {
            return webSocketOnConnect;
        }

        public WebSocketOnMessage getWebSocketOnMessage() {
            return webSocketOnMessage;
        }
    }

    // https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/http2/Http2Server.java
    private static class RedirectHttpsHandler implements HttpHandler {
        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            exchange.getResponseHeaders()
                    .add(Headers.LOCATION,
                            "https://" + exchange.getHostName() + ":" + (exchange.getHostPort() + 363) + exchange.getRelativePath());
            exchange.setStatusCode(StatusCodes.TEMPORARY_REDIRECT);
        }
    }
}
