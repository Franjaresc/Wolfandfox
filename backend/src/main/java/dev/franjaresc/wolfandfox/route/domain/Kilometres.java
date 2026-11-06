package dev.franjaresc.wolfandfox.route.domain;

public record Kilometres(int value) {

    public Kilometres {
        if (value < 0) {
            throw new InvalidRouteClosureException("Kilometres must not be negative: " + value);
        }
    }
}
