package com.clianz.spur.helpers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

// https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/http2/Http2Server.java
public class RedirectHttpsHandler implements HttpHandler {
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders()
                .add(Headers.LOCATION,
                        "https://" + exchange.getHostName() + ":" + (exchange.getHostPort() + 363) + exchange.getRelativePath());
        exchange.setStatusCode(StatusCodes.TEMPORARY_REDIRECT);
    }
}
