package com.clianz.spur.helpers;

import io.undertow.server.HttpHandler;

public class RequestFilter {
    private io.undertow.predicate.Predicate assertionPredicate;
    private HttpHandler failureHandler;

    public RequestFilter(io.undertow.predicate.Predicate assertionPredicate, HttpHandler failureHandler) {
        this.assertionPredicate = assertionPredicate;
        this.failureHandler = failureHandler;
    }

    public io.undertow.predicate.Predicate getAssertionPredicate() {
        return assertionPredicate;
    }

    public HttpHandler getFailureHandler() {
        return failureHandler;
    }
}
