package dev.franjaresc.wolfandfox.route.domain;

public record DriverCode(String value) {

    public DriverCode {
        BusinessCode.requireValid(value, "Driver code");
    }
}
