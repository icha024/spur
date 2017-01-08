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
    private final String expectedCredential;

    public BasicAuthHandler(HttpHandler next, String expectedUsername, String expectedPassword) {
        this.expectedCredential = "Basic " + Base64.getEncoder()
                .encodeToString((expectedUsername + ":" + expectedPassword).getBytes());
        this.authChecker = Handlers.predicate(exchange -> {
            String userCredential = getRequestHeader(exchange, new HttpString("Authorization"));
            if (userCredential == null) {
                return false;
            } else if (expectedCredential.equals(userCredential)) {
                return true;
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
