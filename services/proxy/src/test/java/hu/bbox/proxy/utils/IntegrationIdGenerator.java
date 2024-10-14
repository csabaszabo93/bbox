package hu.bbox.proxy.utils;

import java.util.function.Supplier;

public class IntegrationIdGenerator implements Supplier<String> {
    private String id = "default";

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String get() {
        return id;
    }
}
