package dev.franjaresc.wolfandfox.route.domain;

public record PackageCounts(int delivered, int failed, int returned) {

    public PackageCounts {
        requireNotNegative(delivered, "Delivered");
        requireNotNegative(failed, "Failed");
        requireNotNegative(returned, "Returned");
    }

    /** Derived and never stored. It is a long because three int counts can exceed the int range. */
    public long total() {
        return (long) delivered + failed + returned;
    }

    private static void requireNotNegative(int count, String label) {
        if (count < 0) {
            throw new InvalidRouteClosureException("%s packages must not be negative: %d".formatted(label, count));
        }
    }
}
