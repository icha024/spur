package com.clianz.spur;

import java.util.Objects;
import java.util.function.BiConsumer;

import io.undertow.util.HttpString;

public class Endpoint implements Comparable<Endpoint> {

    private String path;
    private HttpString method;
    private BiConsumer<Req, Res> reqResBiConsumer;

    public Endpoint(HttpString method, String path, BiConsumer<Req, Res> reqResBiConsumer) {
        this.method = method;
        this.path = path;
        this.reqResBiConsumer = reqResBiConsumer;
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

    @Override
    public int compareTo(Endpoint o) {
        int comparePath = this.path.compareToIgnoreCase(o.path);
        if (comparePath == 0) {
            return this.method.compareTo(o.method);
        }
        return comparePath;
    }
}
