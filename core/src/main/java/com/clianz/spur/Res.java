package com.clianz.spur;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.gson.Gson;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

public class Res {

    private static final String JSON_CONTENT_TYPE = "application/json";
    private static Gson gson = new Gson();
    private HttpServerExchange httpServerExchange;

    public Res(HttpServerExchange httpServerExchange) {
        this.httpServerExchange = httpServerExchange;
    }

    public HttpServerExchange getRawHttpServerExchange() {
        return httpServerExchange;
    }

    public Res header(String header, String value) {
        httpServerExchange.getResponseHeaders()
                .put(new HttpString(header), value);
        return this;
    }

    public Res headers(Map<String, String> headers) {
        headers.forEach((header, value) -> httpServerExchange.getResponseHeaders()
                .put(new HttpString(header), value));
        return this;
    }

    public Res status(int statusCode) {
        httpServerExchange.setStatusCode(statusCode);
        return this;
    }

    public void send(String body) {
        httpServerExchange.getResponseSender()
                .send(body, StandardCharsets.UTF_8);
    }

    public void send(Object obj) {
        httpServerExchange.getResponseHeaders()
                .put(Headers.CONTENT_TYPE, JSON_CONTENT_TYPE);
        httpServerExchange.getResponseSender()
                .send(gson.toJson(obj));
    }

    public void send(ByteBuffer byteBuffer) {
        httpServerExchange.getResponseSender()
                .send(byteBuffer);
    }
}
