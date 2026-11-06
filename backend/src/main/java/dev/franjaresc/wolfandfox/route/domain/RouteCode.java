package dev.franjaresc.wolfandfox.route.domain;

public record RouteCode(String value) {

    public RouteCode {
        BusinessCode.requireValid(value, "Route code");
    }
}
