package io.swagger.server.api.util;

import io.swagger.server.api.MainApiHeader;
import io.vertx.core.MultiMap;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ResourceResponse<T> {
    MultiMap headers;
    T response;

    public ResourceResponse() {
        super();
        this.headers = MultiMap.caseInsensitiveMultiMap();
    }

    public ResourceResponse<T> addHeader(MainApiHeader header) {
        if (header.getValue() != null)
            this.headers.add(header.getName(), header.getValue());
        else
            this.headers.add(header.getName(), header.getValues());
        return this;
    }

    public ResourceResponse<T> addHeader(String name, String value) {
        this.headers.add(name, value);
        return this;
    }

    public ResourceResponse<T> addHeaders(String name, Iterable<String> values) {
        this.headers.add(name, values);
        return this;
    }

    public ResourceResponse<T> setResponse(T response) {
        this.response = response;
        return this;
    }

    public MultiMap getHeaders() {
        return headers;
    }

    public T getResponse() {
        return response;
    }

    public String toJson() {
        if(response == null || response instanceof Void) return null;
        if(response instanceof JsonObject) return ((JsonObject) response).encodePrettily();
        if(response instanceof JsonArray) return ((JsonArray) response).encodePrettily();
        return Json.encodePrettily(response);
    }
}