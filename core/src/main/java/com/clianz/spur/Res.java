package com.clianz.spur;

import static com.clianz.spur.helpers.HttpMethods.HEAD;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;
import org.jboss.logging.Logger;
import org.xnio.channels.StreamSinkChannel;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

public class Res {

    private static final Logger LOGGER = Logger.getLogger(Res.class.getName());
    private static final String JSON_CONTENT_TYPE = "application/json";
    private ObjectMapper jsonMapper = JsonFactory.createUseJSONDates();
    private HttpServerExchange httpServerExchange;

    protected Res(HttpServerExchange httpServerExchange) {
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
        if (httpServerExchange.getRequestMethod()
                .equals(HEAD)) {
            httpServerExchange.endExchange();
            return;
        }
        httpServerExchange.getResponseSender()
                .send(body, StandardCharsets.UTF_8);
        httpServerExchange.endExchange();
    }

    public void send() {
        httpServerExchange.endExchange();
    }

    public void send(ByteBuffer byteBuffer) {
        if (httpServerExchange.getRequestMethod()
                .equals(HEAD)) {
            httpServerExchange.endExchange();
            return;
        }
        httpServerExchange.getResponseSender()
                .send(byteBuffer);
        httpServerExchange.endExchange();
    }

    public void send(Object obj) {
        if (httpServerExchange.getRequestMethod()
                .equals(HEAD)) {
            httpServerExchange.endExchange();
            return;
        }
        httpServerExchange.getResponseHeaders()
                .put(Headers.CONTENT_TYPE, JSON_CONTENT_TYPE);

        String jsonStr = jsonMapper.toJson(obj);
        ByteBuffer byteBuffer = ByteBuffer.wrap(jsonStr.getBytes(StandardCharsets.UTF_8));
        StreamSinkChannel responseChannel = httpServerExchange.getResponseChannel();
        int written;
        try {
            do {
                written = responseChannel.write(byteBuffer);
            } while (byteBuffer.hasRemaining() && written > 0);
        } catch (IOException e) {
            LOGGER.error("Can not write response: " + e.getMessage(), e);
        }
    }
}
