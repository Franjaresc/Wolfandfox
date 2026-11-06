package dev.franjaresc.wolfandfox.route.domain;

/** The user who performed an action. */
public record UserId(String value) {

    private static final int MAX_LENGTH = 100;

    public UserId {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            throw new InvalidRouteClosureException(
                    "User id must not be blank or longer than %d characters".formatted(MAX_LENGTH));
        }
    }
}
