package com.clianz.spur;

import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import com.google.gson.Gson;

import org.xnio.channels.StreamSourceChannel;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;

public class Req {

    private static Validator validator;

    static {
        Configuration<?> config = Validation.byDefaultProvider()
                .configure();
        ValidatorFactory factory = config.buildValidatorFactory();
        validator = factory.getValidator();
        factory.close();
    }

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

    public <T> void bodyAsValidatedObject(Consumer<T> jsonObjectConsumer, Class<T> t) {
        httpServerExchange.getRequestReceiver()
                .receiveFullString((exchange, str) -> {
                    T parsedType = gson.fromJson(str, t);
                    Set<ConstraintViolation<T>> constraintViolations = validator.validate(parsedType);
                    if (constraintViolations.isEmpty()) {
                        jsonObjectConsumer.accept(parsedType);
                    } else {
                        exchange.setStatusCode(400);
                        exchange.getResponseSender()
                                .send(gson.toJson(new InvalidValues(constraintViolations.stream()
                                        .map(violation -> violation.getPropertyPath()
                                                .toString())
                                        .collect(Collectors.toList()))));
                        exchange.endExchange();
                    }
                }, StandardCharsets.UTF_8);
    }

    public long bodyLength() {
        return httpServerExchange.getRequestContentLength();
    }

    private class InvalidValues {
        private List<String> invalidValues;

        private InvalidValues(List<String> invalidValues) {
            this.invalidValues = invalidValues;
        }

        public List<String> getInvalidValues() {
            return invalidValues;
        }
    }

}
