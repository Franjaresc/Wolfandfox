package dev.franjaresc.wolfandfox.route.application.port;

/** Which page to read. Bounds mirror the {@code page} and {@code size} parameters in the API contract. */
public record PageQuery(int page, int size) {

    private static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("Page must not be negative: " + page);
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("Size must be between 1 and %d: %d".formatted(MAX_SIZE, size));
        }
    }
}
