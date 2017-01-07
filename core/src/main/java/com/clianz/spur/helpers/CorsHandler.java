package com.clianz.spur.helpers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

public class CorsHandler implements HttpHandler {

    private static final HttpString ACCESS_CONTROL_ALLOW_ORIGIN = new HttpString("Access-Control-Allow-Origin");
    private HttpHandler next;

    public CorsHandler(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders()
                .put(ACCESS_CONTROL_ALLOW_ORIGIN, getRequestHeader(exchange, Headers.ORIGIN));
        this.next.handleRequest(exchange);
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
}
