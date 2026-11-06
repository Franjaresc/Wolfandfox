package dev.franjaresc.wolfandfox.route.domain;

import java.util.regex.Pattern;

/** Format shared by route, vehicle and driver codes. It mirrors {@code Code} in the API contract. */
final class BusinessCode {

    private static final Pattern FORMAT = Pattern.compile("[A-Z0-9][A-Z0-9-]{1,19}");

    private BusinessCode() {
    }

    static void requireValid(String value, String label) {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new InvalidRouteClosureException("%s has an invalid format: %s".formatted(label, value));
        }
    }
}
