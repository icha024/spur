package com.clianz.spur;

import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;
import org.jboss.logging.Logger;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.StatusCodes;

public class Req<T> {

    private static final Logger LOGGER = Logger.getLogger(Req.class.getName());
    private static final ThreadLocal<ObjectMapper> LOCAL_MAPPER = new ThreadLocal<>();
    private static Validator validator;

    static {
        Configuration<?> config = Validation.byDefaultProvider()
                .configure();
        ValidatorFactory factory = config.buildValidatorFactory();
        validator = factory.getValidator();
        factory.close();
    }

    private HttpServerExchange httpServerExchange;
    private T body;

    private Class<T> bodyClassType;

    protected Req(HttpServerExchange httpServerExchange, Class<T> bodyClassType) {
        this.httpServerExchange = httpServerExchange;
        this.bodyClassType = bodyClassType;
    }

    public HttpServerExchange rawHttpServerExchange() {
        return httpServerExchange;
    }

    public Map<String, Deque<String>> params() {
        return httpServerExchange.getQueryParameters();
    }

    public Optional<String> param(String paramName) {
        Deque<String> deque = httpServerExchange.getQueryParameters()
                .get(paramName);
        if (deque == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(deque.getFirst());
    }

    public HeaderMap headers() {
        return httpServerExchange.getRequestHeaders();
    }

    public Optional<String> header(String headerName) {
        return Optional.ofNullable(httpServerExchange.getRequestHeaders()
                .getFirst(headerName));
    }

    public String method() {
        return httpServerExchange.getRequestMethod()
                .toString();
    }

    public T body() {
        return this.body;
    }

    protected void parseBody(PostParseConsumer objectConsumer) {
        if (bodyClassType == null || bodyClassType.equals(Void.class)) {
            objectConsumer.postParse(httpServerExchange, null);
        } else {
            httpServerExchange.getRequestReceiver()
                    .receiveFullString((exchange, str) -> convertBodyStringToObj(objectConsumer, exchange, str), StandardCharsets.UTF_8);
        }
    }

    private void convertBodyStringToObj(PostParseConsumer objectConsumer, HttpServerExchange exchange, String str) {
        if (bodyClassType.equals(String.class)) {
            this.body = (T) str;
            objectConsumer.postParse(exchange, str);
            return;
        }

        ObjectMapper objectMapper = LOCAL_MAPPER.get();
        if (objectMapper == null) {
            objectMapper = JsonFactory.createUseJSONDates();
            LOCAL_MAPPER.set(objectMapper);
        }

        T parsedType;
        try {
            parsedType = objectMapper.readValue(str, bodyClassType);
        } catch (Exception e) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.endExchange();
            return;
        }

        if (parsedType == null) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.endExchange();
            return;
        }

        Set<ConstraintViolation<T>> constraintViolations = validator.validate(parsedType);
        if (constraintViolations.isEmpty()) {
            this.body = parsedType;
            objectConsumer.postParse(exchange, parsedType);
        } else {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender()
                    .send(objectMapper.toJson(new InvalidValues(constraintViolations.stream()
                            .map(violation -> violation.getPropertyPath()
                                    .toString())
                            .collect(Collectors.toList()))));
            exchange.endExchange();
        }
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

    @FunctionalInterface
    public interface PostParseConsumer<T> {
        void postParse(HttpServerExchange exchange, T body);
    }

}
