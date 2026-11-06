package dev.franjaresc.wolfandfox.route.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ValueObjectsTest {

    @ParameterizedTest
    @ValueSource(strings = {"RT-0001", "A1", "12345678901234567890"})
    void acceptsWellFormedCodes(String code) {
        assertThat(new RouteCode(code).value()).isEqualTo(code);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"A", "-RT1", "rt-0001", "RT 0001", "123456789012345678901"})
    void rejectsMalformedCodes(String code) {
        assertThatThrownBy(() -> new RouteCode(code)).isInstanceOf(InvalidRouteClosureException.class);
        assertThatThrownBy(() -> new VehicleCode(code)).isInstanceOf(InvalidRouteClosureException.class);
        assertThatThrownBy(() -> new DriverCode(code)).isInstanceOf(InvalidRouteClosureException.class);
    }

    @Test
    void rejectsMalformedCodeEndingInLineBreak() {
        assertThatThrownBy(() -> new RouteCode("RT-0001\n")).isInstanceOf(InvalidRouteClosureException.class);
    }

    @Test
    void acceptsZeroKilometres() {
        assertThat(new Kilometres(0).value()).isZero();
    }

    @Test
    void rejectsNegativeKilometres() {
        assertThatThrownBy(() -> new Kilometres(-1)).isInstanceOf(InvalidRouteClosureException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsBlankUserId(String value) {
        assertThatThrownBy(() -> new UserId(value)).isInstanceOf(InvalidRouteClosureException.class);
    }

    @Test
    void rejectsUserIdLongerThanTheColumn() {
        assertThat(new UserId("u".repeat(100)).value()).hasSize(100);
        assertThatThrownBy(() -> new UserId("u".repeat(101))).isInstanceOf(InvalidRouteClosureException.class);
    }

    @Test
    void rejectsMissingRouteClosureId() {
        assertThatThrownBy(() -> new RouteClosureId(null)).isInstanceOf(InvalidRouteClosureException.class);
    }
}
