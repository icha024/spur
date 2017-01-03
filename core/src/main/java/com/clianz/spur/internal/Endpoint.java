package com.clianz.spur.internal;

import java.util.Objects;
import java.util.function.BiConsumer;

import com.clianz.spur.Req;
import com.clianz.spur.Res;

import io.undertow.util.HttpString;

public class Endpoint<T> {

    private String path;
    private HttpString method;
    private BiConsumer<Req, Res> reqResBiConsumer;
    private Class<T> bodyClassType;

    public Endpoint(HttpString method, String path, BiConsumer<Req, Res> reqResBiConsumer, Class<T> bodyClassType) {
        this.method = method;
        this.path = path;
        this.reqResBiConsumer = reqResBiConsumer;
        this.bodyClassType = bodyClassType;
    }

    public String getPath() {
        return path;
    }

    public HttpString getMethod() {
        return method;
    }

    public BiConsumer<Req, Res> getReqResBiConsumer() {
        return reqResBiConsumer;
    }

    public Class<T> getBodyClassType() {
        return bodyClassType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Endpoint endpoint = (Endpoint) o;
        return Objects.equals(path, endpoint.path) && method == endpoint.method;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, method);
    }
}
