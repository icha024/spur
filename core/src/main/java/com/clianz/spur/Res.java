package com.clianz.spur;

import static com.clianz.spur.internal.HttpMethods.HEAD;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.gson.Gson;

import org.xnio.channels.StreamSinkChannel;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

public class Res {

    private static final String JSON_CONTENT_TYPE = "application/json";
    private static Gson gson = new Gson();
    private HttpServerExchange httpServerExchange;
    //    StreamSinkChannel resChannel;

    protected Res(HttpServerExchange httpServerExchange) {
        this.httpServerExchange = httpServerExchange;
        //        this.resChannel = httpServerExchange.getResponseChannel();
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
            //            httpServerExchange.endExchange();
            return;
        }
        httpServerExchange.getResponseSender()
                .send(body, StandardCharsets.UTF_8);
        //        httpServerExchange.endExchange();
    }

    public void send() {
        //        httpServerExchange.endExchange();
    }

    public void send(ByteBuffer byteBuffer) {
        if (httpServerExchange.getRequestMethod()
                .equals(HEAD)) {
            //            httpServerExchange.endExchange();
            return;
        }
        httpServerExchange.getResponseSender()
                .send(byteBuffer);
        //        httpServerExchange.endExchange();
    }

    public void send(Object obj) {
        if (httpServerExchange.getRequestMethod()
                .equals(HEAD)) {
            //            httpServerExchange.endExchange();
            return;
        }
        httpServerExchange.getResponseHeaders()
                .put(Headers.CONTENT_TYPE, JSON_CONTENT_TYPE);

        //        httpServerExchange.getResponseSender()
        //                .send(gson.toJson(obj));
        //        httpServerExchange.endExchange();

        try {
            String jsonStr = gson.toJson(obj);
            ByteBuffer byteBuffer = ByteBuffer.wrap(jsonStr.getBytes(StandardCharsets.UTF_8));
            StreamSinkChannel responseChannel = httpServerExchange.getResponseChannel();
            //                    .write(byteBuffer);

            int written;
            do {
                written = responseChannel.write(byteBuffer);
            } while (byteBuffer.hasRemaining() && written > 0);
//            httpServerExchange.getResponseSender().send(jsonStr);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
