package com.clianz.spur.helpers;

import java.util.Base64;
import java.util.logging.Logger;

import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;

public class BasicAuthHandler implements HttpHandler {
    private static final Logger LOGGER = Logger.getLogger(BasicAuthHandler.class.getName());
    private HttpHandler authChecker;

    public BasicAuthHandler(HttpHandler next, String expectedUsername, String expectedPassword) {
        this.authChecker = Handlers.predicate(exchange -> {
            String auth = getRequestHeader(exchange, new HttpString("Authorization"));
            if (auth == null) {
                return false;
            } else {
                if (auth.equals("Basic " + Base64.getEncoder()
                        .encodeToString((expectedUsername + ":" + expectedPassword).getBytes()))) {
                    return true;
                }
            }
            return false;
        }, next, exchange -> {
            exchange.getResponseHeaders()
                    .put(new HttpString("WWW-Authenticate"), "Basic realm=\"realm\"");
            exchange.setStatusCode(StatusCodes.UNAUTHORIZED)
                    .endExchange();
        });
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        authChecker.handleRequest(httpServerExchange);
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
