package io.repox.api3d.app.utils;

public enum MDCVariables {
    PATH("uri"),
    USER("user");

    private final String value;

    MDCVariables(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
