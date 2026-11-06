package dev.franjaresc.wolfandfox.route.domain;

public record VehicleCode(String value) {

    public VehicleCode {
        BusinessCode.requireValid(value, "Vehicle code");
    }
}
