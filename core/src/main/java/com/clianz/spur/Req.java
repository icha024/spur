package com.clianz.spur;

import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.Map;
import java.util.function.Consumer;

import com.google.gson.Gson;

import org.xnio.channels.StreamSourceChannel;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;

public class Req {

    private static Gson gson = new Gson();
    private HttpServerExchange httpServerExchange;

    public Req(HttpServerExchange httpServerExchange) {
        this.httpServerExchange = httpServerExchange;
    }

    public HttpServerExchange getRawHttpServerExchange() {
        return httpServerExchange;
    }

    public Map<String, Deque<String>> params() {
        return httpServerExchange.getQueryParameters();
    }

    public HeaderMap headers() {
        return httpServerExchange.getRequestHeaders();
    }

    public StreamSourceChannel bodyAsStream() {
        return httpServerExchange.getRequestChannel();
    }

    public void bodyAsBytes(Consumer<byte[]> byteConsumer) {
        httpServerExchange.getRequestReceiver()
                .receiveFullBytes((exchange, bytes) -> byteConsumer.accept(bytes));
    }

    public void body(Consumer<String> stringConsumer) {
        httpServerExchange.getRequestReceiver()
                .receiveFullString((httpServerExchange1, s) -> stringConsumer.accept(s), StandardCharsets.UTF_8);
    }

    public <T> void body(Consumer<T> stringConsumer, T t) {
        httpServerExchange.getRequestReceiver()
                .receiveFullString((httpServerExchange1, str) -> {
                    T parsedType = (T) gson.fromJson(str, t.getClass());
                    stringConsumer.accept(parsedType);
                }, StandardCharsets.UTF_8);
    }

    public long bodyLength() {
        return httpServerExchange.getRequestContentLength();
    }

}
